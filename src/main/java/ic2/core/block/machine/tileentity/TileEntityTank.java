package ic2.core.block.machine.tileentity;

import ic2.api.upgrade.IUpgradableBlock;
import ic2.api.upgrade.UpgradableProperty;
import ic2.core.block.TileEntityInventory;
import ic2.core.block.comp.Fluids;
import ic2.core.block.invslot.InvSlotUpgrade;
import ic2.core.init.IC2Blocks;
import ic2.core.ref.TeBlock;
import java.util.EnumSet;
import java.util.Set;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

/**
 * Fabric port of the machine TileEntityTank: a 24 000 mB tank open on all sides, with four
 * slots for fluid ejector/pulling upgrades and a comparator signal of 1 to 15 by how full it is.
 */
public class TileEntityTank extends TileEntityInventory implements IUpgradableBlock {
    public final InvSlotUpgrade upgradeSlot;
    protected final Fluids.InternalFluidTank fluidTank;

    public TileEntityTank(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type == null ? IC2Blocks.TANK_BE : type, pos, state, TeBlock.tank);
        this.upgradeSlot = new InvSlotUpgrade(this, "upgrade", 4);
        this.fluidTank = fluids().addTank("fluid", 24000);
    }

    public TileEntityTank(BlockPos pos, BlockState state) {
        this(null, pos, state);
    }

    public Fluids.InternalFluidTank getTank() {
        return fluidTank;
    }

    @Override
    public void tick() {
        if (world == null || world.isClient) {
            return;
        }
        upgradeSlot.tick();
    }

    @Override
    public int getComparatorOutput() {
        return fluidTank.getFluidAmount() == 0 ? 0
                : (int) MathHelper.lerp((float) fluidTank.getFluidAmount() / fluidTank.getCapacity(), 1.0F, 15.0F);
    }

    @Override
    public double getEnergy() {
        return 0.0;
    }

    @Override
    public boolean useEnergy(double amount) {
        return false;
    }

    @Override
    public Set<UpgradableProperty> getUpgradableProperties() {
        return EnumSet.of(UpgradableProperty.FluidConsuming, UpgradableProperty.FluidProducing);
    }
}
