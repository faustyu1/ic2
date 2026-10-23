package ic2.core.block.invslot;

import ic2.api.upgrade.IAugmentationUpgrade;
import ic2.api.upgrade.IEnergyStorageUpgrade;
import ic2.api.upgrade.IFullUpgrade;
import ic2.api.upgrade.IProcessingUpgrade;
import ic2.api.upgrade.IRedstoneSensitiveUpgrade;
import ic2.api.upgrade.IRemoteAccessUpgrade;
import ic2.api.upgrade.ITransformerUpgrade;
import ic2.api.upgrade.IUpgradableBlock;
import ic2.api.upgrade.IUpgradeItem;
import ic2.core.block.IInventorySlotHolder;
import net.minecraft.item.ItemStack;

/**
 * Fabric port of ic2.core.block.invslot.InvSlotUpgrade - the upgrade slots of a machine.
 *
 * <p>Only items that declare themselves suitable for the machine's
 * {@link ic2.api.upgrade.UpgradableProperty} set are accepted, which is what stops an overclocker
 * from going into a machine that cannot process anything. Whenever the slots change the combined
 * effect of the upgrades is worked out again, and the machine is told to re-read its rates - the
 * same arithmetic as 1.12.2, stacks counting as many upgrades as they hold.
 */
public class InvSlotUpgrade extends InvSlot {
    private final IUpgradableBlock parent;

    public int augmentation;
    public int extraProcessTime;
    public double processTimeMultiplier;
    public int extraEnergyDemand;
    public double energyDemandMultiplier;
    public int extraEnergyStorage;
    public double energyStorageMultiplier;
    public int extraTier;

    public InvSlotUpgrade(IInventorySlotHolder<?> base, String name, int count) {
        super(base, name, Access.NONE, count);
        this.parent = base.getParent() instanceof IUpgradableBlock upgradable ? upgradable : null;
        resetRates();
    }

    @Override
    public boolean accepts(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof IUpgradeItem upgrade)) {
            return false;
        }
        return parent == null || upgrade.isSuitableFor(stack, parent.getUpgradableProperties());
    }

    @Override
    public void onChanged() {
        resetRates();

        for (ItemStack stack : this) {
            if (stack.isEmpty() || !accepts(stack)) {
                continue;
            }

            IUpgradeItem upgrade = (IUpgradeItem) stack.getItem();
            boolean all = upgrade instanceof IFullUpgrade;
            int size = stack.getCount();

            if (all || upgrade instanceof IAugmentationUpgrade) {
                augmentation += ((IAugmentationUpgrade) upgrade).getAugmentation(stack, parent) * size;
            }
            if (all || upgrade instanceof IProcessingUpgrade) {
                IProcessingUpgrade processing = (IProcessingUpgrade) upgrade;
                extraProcessTime += processing.getExtraProcessTime(stack, parent) * size;
                processTimeMultiplier *= Math.pow(processing.getProcessTimeMultiplier(stack, parent), size);
                extraEnergyDemand += processing.getExtraEnergyDemand(stack, parent) * size;
                energyDemandMultiplier *= Math.pow(processing.getEnergyDemandMultiplier(stack, parent), size);
            }
            if (all || upgrade instanceof IEnergyStorageUpgrade) {
                IEnergyStorageUpgrade storage = (IEnergyStorageUpgrade) upgrade;
                extraEnergyStorage += storage.getExtraEnergyStorage(stack, parent) * size;
                energyStorageMultiplier *= Math.pow(storage.getEnergyStorageMultiplier(stack, parent), size);
            }
            if (all || upgrade instanceof ITransformerUpgrade) {
                extraTier += ((ITransformerUpgrade) upgrade).getExtraTier(stack, parent) * size;
            }
        }

        super.onChanged();
        if (base != null && base.getParent() instanceof Listener listener) {
            listener.onUpgradesChanged();
        }
    }

    private void resetRates() {
        augmentation = 0;
        extraProcessTime = 0;
        processTimeMultiplier = 1.0;
        extraEnergyDemand = 0;
        energyDemandMultiplier = 1.0;
        extraEnergyStorage = 0;
        energyStorageMultiplier = 1.0;
        extraTier = 0;
    }

    /** Operations a machine runs per tick: overclocked below one tick, it runs several. */
    public int getOperationsPerTick(int defaultOperationLength) {
        return defaultOperationLength == 0 ? 64 : getOpsPerTick(getStackOpLen(defaultOperationLength));
    }

    public int getOperationLength(int defaultOperationLength) {
        if (defaultOperationLength == 0) {
            return 1;
        }
        double stackOpLen = getStackOpLen(defaultOperationLength);
        int opsPerTick = getOpsPerTick(stackOpLen);
        return Math.max(1, (int) Math.round(stackOpLen * opsPerTick / 64.0));
    }

    private double getStackOpLen(int defaultOperationLength) {
        return ((double) defaultOperationLength + extraProcessTime) * 64.0 * processTimeMultiplier;
    }

    private static int getOpsPerTick(double stackOpLen) {
        return (int) Math.min(Math.ceil(64.0 / stackOpLen), Integer.MAX_VALUE);
    }

    public int getEnergyDemand(int defaultEnergyDemand) {
        return applyModifier(defaultEnergyDemand, extraEnergyDemand, energyDemandMultiplier);
    }

    /** The buffer: the default, plus a whole operation's energy, plus the storage upgrades. */
    public int getEnergyStorage(int defaultEnergyStorage, int defaultOperationLength, int defaultEnergyDemand) {
        int opLen = getOperationLength(defaultOperationLength);
        int energyDemand = getEnergyDemand(defaultEnergyDemand);
        return applyModifier(defaultEnergyStorage, extraEnergyStorage + opLen * energyDemand, energyStorageMultiplier);
    }

    public int getTier(int defaultTier) {
        return applyModifier(defaultTier, extraTier, 1.0);
    }

    public int getRemoteRange(int existingRange) {
        for (ItemStack stack : this) {
            if (!stack.isEmpty() && accepts(stack) && stack.getItem() instanceof IRemoteAccessUpgrade remote) {
                existingRange = remote.getRangeAmplification(stack, parent, existingRange);
            }
        }
        return existingRange;
    }

    /** The redstone signal the machine sees through its inverters. */
    public int getRedstoneInput(int redstoneInput) {
        for (ItemStack stack : this) {
            if (!stack.isEmpty() && accepts(stack) && stack.getItem() instanceof IRedstoneSensitiveUpgrade redstone
                    && redstone.modifiesRedstoneInput(stack, parent)) {
                redstoneInput = redstone.getRedstoneInput(stack, parent, redstoneInput);
            }
        }
        return redstoneInput;
    }

    private static int applyModifier(int base, int extra, double multiplier) {
        double ret = Math.round((base + (double) extra) * multiplier);
        return ret > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) ret;
    }

    /** Runs the per-tick upgrade hooks; returns true when the inventory changed. */
    public boolean tickNoMark() {
        if (parent == null) {
            return false;
        }

        boolean changed = false;
        for (ItemStack stack : this) {
            if (!stack.isEmpty() && stack.getItem() instanceof IUpgradeItem upgrade) {
                changed |= upgrade.onTick(stack, parent);
            }
        }
        return changed;
    }

    public void tick() {
        if (tickNoMark()) {
            markDirty();
        }
    }

    /** A block whose rates depend on its upgrades. */
    public interface Listener {
        void onUpgradesChanged();
    }
}
