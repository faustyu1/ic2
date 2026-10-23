package ic2.core.block.steam;

import ic2.core.block.TileEntityInventory;
import ic2.core.block.comp.Fluids;
import ic2.core.block.invslot.InvSlot;
import ic2.core.init.IC2Blocks;
import ic2.core.ref.TeBlock;
import java.util.Collections;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * Fabric port of TileEntityCokeKilnGrate - the floor of the coke kiln, which collects the
 * creosote in a 64 000 mB tank that can only be drained from the side it faces.
 */
public class TileEntityCokeKilnGrate extends TileEntityInventory {
    protected final Fluids.InternalFluidTank fluidTank;

    public TileEntityCokeKilnGrate(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type == null ? IC2Blocks.COKE_GRATE_BE : type, pos, state, TeBlock.coke_kiln_grate);
        this.fluidTank = fluids().addTank("fluidTank", 64000, InvSlot.Access.O, InvSlot.InvSide.ANY, fluid -> true);
        updateConnectivity();
    }

    public TileEntityCokeKilnGrate(BlockPos pos, BlockState state) {
        this(null, pos, state);
    }

    public Fluids.InternalFluidTank getFluidTank() {
        return fluidTank;
    }

    @Override
    public void setFacing(Direction facing) {
        super.setFacing(facing);
        updateConnectivity();
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.readNbt(nbt, lookup);
        updateConnectivity();
    }

    private void updateConnectivity() {
        fluidTank.setSides(Collections.emptyList(), Collections.singleton(getFacing()));
    }
}
