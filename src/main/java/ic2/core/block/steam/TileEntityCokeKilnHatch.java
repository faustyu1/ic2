package ic2.core.block.steam;

import ic2.core.block.TileEntityInventory;
import ic2.core.block.invslot.InvSlot;
import ic2.core.init.IC2Blocks;
import ic2.core.ref.TeBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * Fabric port of TileEntityCokeKilnHatch - the roof of the coke kiln, through which the wood or
 * coal goes in. Automation can only fill it from the side it faces and never take from it.
 */
public class TileEntityCokeKilnHatch extends TileEntityInventory {
    protected final InvSlot inventory;

    public TileEntityCokeKilnHatch(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type == null ? IC2Blocks.COKE_HATCH_BE : type, pos, state, TeBlock.coke_kiln_hatch);
        this.inventory = new InvSlot(this, "inventory", InvSlot.Access.I, 1, InvSlot.InvSide.ANY);
    }

    public TileEntityCokeKilnHatch(BlockPos pos, BlockState state) {
        this(null, pos, state);
    }

    public InvSlot getInventory() {
        return inventory;
    }

    @Override
    public boolean canInsert(int index, ItemStack stack, Direction side) {
        return side == getFacing() && super.canInsert(index, stack, side);
    }

    @Override
    public boolean canExtract(int index, ItemStack stack, Direction side) {
        return false;
    }
}
