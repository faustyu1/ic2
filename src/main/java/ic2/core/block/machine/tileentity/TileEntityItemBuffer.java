package ic2.core.block.machine.tileentity;

import ic2.api.upgrade.IUpgradableBlock;
import ic2.api.upgrade.IUpgradeItem;
import ic2.api.upgrade.UpgradableProperty;
import ic2.core.block.TileEntityInventory;
import ic2.core.block.invslot.InvSlot;
import ic2.core.block.invslot.InvSlotUpgrade;
import ic2.core.init.IC2Blocks;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

/**
 * Fabric port of TileEntityItemBuffer - a 48 slot buffer between two transport systems.
 *
 * <p>The right half (24 slots) is what pipes and hoppers on the sides reach first, the left half
 * what the top and bottom reach. Each half has one upgrade slot under it, for an ejector: with
 * both filled the two upgrades take turns, one tick each, so each side pushes at half the rate;
 * with one filled it works every tick. The upgrades run for free - the buffer reports 40 EU and
 * never spends any. A comparator reads how full the two halves are.
 */
public class TileEntityItemBuffer extends TileEntityInventory implements IUpgradableBlock {
    public final InvSlot rightContentSlot;
    public final InvSlot leftContentSlot;
    public final InvSlotUpgrade upgradeSlot;
    /** Whose turn it is when both upgrade slots are filled: true for the left one. */
    private boolean tick = true;

    public TileEntityItemBuffer(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type == null ? IC2Blocks.ITEM_BUFFER_BE : type, pos, state);
        this.rightContentSlot = new InvSlot(this, "rightcontent", InvSlot.Access.IO, 24, InvSlot.InvSide.SIDE);
        this.leftContentSlot = new InvSlot(this, "leftcontent", InvSlot.Access.IO, 24, InvSlot.InvSide.NOTSIDE);
        this.upgradeSlot = new InvSlotUpgrade(this, "upgrade", 2);
    }

    public TileEntityItemBuffer(BlockPos pos, BlockState state) {
        this(null, pos, state);
    }

    @Override
    public void tick() {
        if (world == null || world.isClient) {
            return;
        }

        ItemStack upgradeLeft = upgradeSlot.get(0);
        ItemStack upgradeRight = upgradeSlot.get(1);
        if (!upgradeLeft.isEmpty() && !upgradeRight.isEmpty()) {
            runUpgrade(tick ? upgradeLeft : upgradeRight);
            tick = !tick;
        } else {
            if (!upgradeLeft.isEmpty()) {
                tick = true;
                runUpgrade(upgradeLeft);
            }
            if (!upgradeRight.isEmpty()) {
                tick = false;
                runUpgrade(upgradeRight);
            }
        }
    }

    private void runUpgrade(ItemStack stack) {
        if (stack.getItem() instanceof IUpgradeItem upgrade && upgrade.onTick(stack, this)) {
            markDirty();
        }
    }

    @Override
    public int getComparatorOutput() {
        return calcRedstoneFromInvSlots(List.of(rightContentSlot, leftContentSlot));
    }

    @Override
    public Set<UpgradableProperty> getUpgradableProperties() {
        return EnumSet.of(UpgradableProperty.ItemProducing);
    }

    @Override
    public double getEnergy() {
        return 40.0;
    }

    @Override
    public boolean useEnergy(double amount) {
        return true;
    }
}
