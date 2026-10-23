package ic2.core.block.machine.tileentity;

import ic2.api.item.ElectricItem;
import ic2.api.item.IElectricItem;
import ic2.api.recipe.IBasicMachineRecipeManager;
import ic2.api.recipe.MachineRecipeResult;
import ic2.api.upgrade.IUpgradableBlock;
import ic2.api.upgrade.UpgradableProperty;
import ic2.core.block.invslot.InvSlotConsumable;
import ic2.core.block.invslot.InvSlotDischarge;
import ic2.core.block.invslot.InvSlotOutput;
import ic2.core.block.invslot.InvSlotUpgrade;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;

/**
 * Fabric port of TileEntityStandardMachine - the base of the single-input processing machines
 * (macerator, compressor, extractor, ...).
 *
 * <p>The slot groups carry the names the guidef files use - {@code input}, {@code output},
 * {@code upgrade} and {@code discharge} - so the GUI layout comes straight from the XML. What the
 * input slot accepts is decided by the machine's recipe manager, which is looked up lazily because
 * the managers are created while the recipes load, after the block entity classes.
 */
public abstract class TileEntityStandardMachine<RI, RO, I> extends TileEntityElectricMachine implements IUpgradableBlock, InvSlotUpgrade.Listener {

    protected short progress = 0;
    public final int defaultEnergyConsume;
    public final int defaultOperationLength;
    protected int operationsPerTick = 1;
    protected int energyConsume;
    protected int operationLength;
    protected float guiProgress = 0;

    public final InvSlotConsumable inputSlot;
    public final InvSlotOutput outputSlot;
    public final InvSlotUpgrade upgradeSlot;
    public final InvSlotDischarge dischargeSlot;

    public TileEntityStandardMachine(BlockEntityType<?> type, BlockPos pos, BlockState state, int energyPerTick, int length, int outputSlots) {
        this(type, pos, state, energyPerTick, length, outputSlots, 1);
    }

    public TileEntityStandardMachine(BlockEntityType<?> type, BlockPos pos, BlockState state, int energyPerTick, int length, int outputSlots, int tier) {
        super(type, pos, state, energyPerTick * length, tier);
        this.defaultEnergyConsume = this.energyConsume = energyPerTick;
        this.defaultOperationLength = this.operationLength = length;

        this.inputSlot = new RecipeInputSlot();
        this.outputSlot = new InvSlotOutput(this, "output", outputSlots);
        this.upgradeSlot = new InvSlotUpgrade(this, "upgrade", 4);
        this.dischargeSlot = new InvSlotDischarge(this, "discharge", tier);
        onUpgradesChanged();
    }

    /** Convenience constructor for the no-arg instantiation path used by the block entity registry. */
    public TileEntityStandardMachine(int energyPerTick, int length, int outputSlots, int tier) {
        this(null, BlockPos.ORIGIN, null, energyPerTick, length, outputSlots, tier);
    }

    /** Input slot that accepts exactly what this machine has a recipe for. */
    private final class RecipeInputSlot extends InvSlotConsumable {
        private RecipeInputSlot() {
            super(TileEntityStandardMachine.this, "input", 1);
        }

        @Override
        public boolean accepts(ItemStack stack) {
            return !stack.isEmpty() && acceptsInput(stack);
        }
    }

    /** Whether the input slot takes the stack: by default, when the recipe manager has a recipe for it. */
    protected boolean acceptsInput(ItemStack stack) {
        IBasicMachineRecipeManager manager = getRecipeManager();
        return manager != null && manager.apply(stack, false) != null;
    }

    public ItemStack getInput() {
        return inputSlot.get();
    }

    public void setInput(ItemStack stack) {
        inputSlot.put(stack);
    }

    public ItemStack getOutput(int index) {
        return outputSlot.get(index);
    }

    protected abstract IBasicMachineRecipeManager getRecipeManager();

    @SuppressWarnings("unchecked")
    protected MachineRecipeResult<RI, RO, I> getOutput() {
        ItemStack input = getInput();
        if (input.isEmpty()) {
            return null;
        }

        MachineRecipeResult<RI, RO, I> result = (MachineRecipeResult<RI, RO, I>) getRecipeManager().apply(input, false);
        if (result == null) {
            return null;
        }

        // an operation only starts when its result fits, so nothing is lost mid-craft
        return outputSlot.canAdd((Collection<ItemStack>) result.getRecipe().getOutput()) ? result : null;
    }

    @SuppressWarnings("unchecked")
    protected void operate(MachineRecipeResult<RI, RO, I> result) {
        if (result == null) {
            return;
        }

        // the recipe states how much of the input it takes, which is not always one
        ItemStack adjusted = (ItemStack) result.getAdjustedInput();
        ItemStack input = getInput();
        int consumed = Math.max(1, input.getCount() - (adjusted == null ? input.getCount() - 1 : adjusted.getCount()));
        inputSlot.consume(consumed);

        Collection<ItemStack> outputs = (Collection<ItemStack>) result.getRecipe().getOutput();
        for (ItemStack out : outputs) {
            outputSlot.add(out.copy());
        }

        markDirty();
    }

    /** Server tick: charge from the discharge slot, run the upgrades, then one operation step. */
    public void tick() {
        if (world == null || world.isClient) {
            return;
        }

        drainDischargeSlot();
        upgradeSlot.tickNoMark();

        MachineRecipeResult<RI, RO, I> output = getOutput();
        if (output != null && useEnergy(energyConsume)) {
            setActive(true);
            progress++;
            if (progress >= operationLength) {
                // an overclocked machine may finish several operations in one tick
                for (int i = 0; i < operationsPerTick && output != null; i++) {
                    operate(output);
                    output = getOutput();
                }
                progress = 0;
            }
        } else {
            if (progress != 0 && output == null) {
                progress = 0;
            }
            setActive(false);
        }

        guiProgress = operationLength == 0 ? 0 : (float) progress / (float) operationLength;
    }

    /**
     * Re-reads speed, power, voltage and buffer from the upgrades, keeping how far the current
     * operation got - setOverclockRates in 1.12.2.
     */
    @Override
    public void onUpgradesChanged() {
        double previousProgress = operationLength == 0 ? 0 : (double) progress / operationLength;
        operationsPerTick = upgradeSlot.getOperationsPerTick(defaultOperationLength);
        operationLength = upgradeSlot.getOperationLength(defaultOperationLength);
        energyConsume = upgradeSlot.getEnergyDemand(defaultEnergyConsume);
        setTier(upgradeSlot.getTier(defaultTier));
        setCapacity(upgradeSlot.getEnergyStorage(defaultEnergyStorage, defaultOperationLength, defaultEnergyConsume));
        progress = (short) Math.floor(previousProgress * operationLength + 0.1);
    }

    /** Moves energy out of a battery in the discharge slot into the machine's buffer. */
    private void drainDischargeSlot() {
        double space = maxEnergy - energyStored;
        if (space <= 0 || dischargeSlot.isEmpty()) {
            return;
        }

        ItemStack stack = dischargeSlot.get();
        if (ElectricItem.manager == null || !(stack.getItem() instanceof IElectricItem)) {
            return;
        }

        double drained = ElectricItem.manager.discharge(stack, space, tier, false, true, false);
        if (drained > 0) {
            energyStored += drained;
            markDirty();
        }
    }

    @Override
    public double getEnergy() {
        return energyStored;
    }

    @Override
    public boolean useEnergy(double amount) {
        return super.useEnergy(amount);
    }

    @Override
    public Set<UpgradableProperty> getUpgradableProperties() {
        return EnumSet.of(UpgradableProperty.Processing, UpgradableProperty.Transformer,
                UpgradableProperty.EnergyStorage, UpgradableProperty.ItemConsuming,
                UpgradableProperty.ItemProducing);
    }

    @Override
    public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.writeNbt(nbt, registries);
        nbt.putShort("progress", progress);
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.readNbt(nbt, registries);
        if (nbt.contains("progress")) {
            progress = nbt.getShort("progress");
        }
    }

    /** Ticks an operation takes now, upgrades included. */
    public int getOperationLength() {
        return operationLength;
    }

    /** EU an operation takes a tick now, upgrades included. */
    public int getEnergyConsume() {
        return energyConsume;
    }

    /** Progress of the current operation as 0..1, which is what the guidef gauge reads. */
    public float getProgress() {
        return guiProgress;
    }

    @Override
    public double getGuiValue(String name) {
        return switch (name) {
            case "progress" -> guiProgress;
            case "energy" -> maxEnergy == 0 ? 0.0 : energyStored / maxEnergy;
            default -> super.getGuiValue(name);
        };
    }
}
