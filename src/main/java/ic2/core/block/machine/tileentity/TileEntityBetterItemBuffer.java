package ic2.core.block.machine.tileentity;

import ic2.api.upgrade.IUpgradableBlock;
import ic2.api.upgrade.UpgradableProperty;
import ic2.core.block.TileEntityInventory;
import ic2.core.block.invslot.InvSlot;
import ic2.core.block.invslot.InvSlotUpgrade;
import ic2.core.init.IC2Blocks;
import java.util.EnumSet;
import java.util.Set;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;

/**
 * Fabric port of TileEntityBetterItemBuffer - the compact item buffer ({@code item_buffer_2}):
 * nine slots reachable from every side and four upgrade slots, laid out by
 * {@code guidef/item_buffer_2.xml}. Ejector and pulling upgrades all run every tick, for free.
 */
public class TileEntityBetterItemBuffer extends TileEntityInventory implements IUpgradableBlock {
    public final InvSlot bufferSlot;
    public final InvSlotUpgrade upgradeSlot;

    public TileEntityBetterItemBuffer(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type == null ? IC2Blocks.ITEM_BUFFER_2_BE : type, pos, state);
        this.bufferSlot = new InvSlot(this, "buffer", InvSlot.Access.IO, 9, InvSlot.InvSide.ANY);
        this.upgradeSlot = new InvSlotUpgrade(this, "upgrade", 4);
    }

    public TileEntityBetterItemBuffer(BlockPos pos, BlockState state) {
        this(null, pos, state);
    }

    @Override
    public void tick() {
        if (world == null || world.isClient) {
            return;
        }
        upgradeSlot.tick();
    }

    @Override
    public Set<UpgradableProperty> getUpgradableProperties() {
        return EnumSet.of(UpgradableProperty.ItemProducing, UpgradableProperty.ItemConsuming);
    }

    @Override
    public double getEnergy() {
        return 0.0;
    }

    @Override
    public boolean useEnergy(double amount) {
        return true;
    }
}
