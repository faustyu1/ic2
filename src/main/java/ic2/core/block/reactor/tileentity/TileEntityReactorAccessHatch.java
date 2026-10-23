package ic2.core.block.reactor.tileentity;

import ic2.core.ref.TeBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * Fabric port of TileEntityReactorAccessHatch - a hull block that lets machines at the reactor's
 * grid from outside.
 *
 * <p>It holds nothing itself: every inventory call goes to the reactor it belongs to, so a hopper
 * against the hatch feeds the reactor. Right clicking it opens the reactor's screen, which the
 * block handles for every reactor chamber alike.
 */
public class TileEntityReactorAccessHatch extends TileEntityReactorVessel implements SidedInventory {
    private static final int[] NO_SLOTS = new int[0];

    public TileEntityReactorAccessHatch(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, TeBlock.reactor_access_hatch);
    }

    public TileEntityReactorAccessHatch(BlockPos pos, BlockState state) { this(null, pos, state); }
    public TileEntityReactorAccessHatch() { this(null, BlockPos.ORIGIN, null); }

    @Override
    public int size() {
        TileEntityNuclearReactorElectric reactor = getReactorInstance();
        return reactor == null ? 0 : reactor.size();
    }

    @Override
    public boolean isEmpty() {
        TileEntityNuclearReactorElectric reactor = getReactorInstance();
        return reactor == null || reactor.isEmpty();
    }

    @Override
    public ItemStack getStack(int slot) {
        TileEntityNuclearReactorElectric reactor = getReactorInstance();
        return reactor == null ? ItemStack.EMPTY : reactor.getStack(slot);
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        TileEntityNuclearReactorElectric reactor = getReactorInstance();
        return reactor == null ? ItemStack.EMPTY : reactor.removeStack(slot, amount);
    }

    @Override
    public ItemStack removeStack(int slot) {
        TileEntityNuclearReactorElectric reactor = getReactorInstance();
        return reactor == null ? ItemStack.EMPTY : reactor.removeStack(slot);
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        TileEntityNuclearReactorElectric reactor = getReactorInstance();
        if (reactor != null) {
            reactor.setStack(slot, stack);
        }
    }

    @Override
    public int getMaxCountPerStack() {
        return 1;
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        TileEntityNuclearReactorElectric reactor = getReactorInstance();
        return reactor != null && reactor.canPlayerUse(player);
    }

    @Override
    public boolean isValid(int slot, ItemStack stack) {
        TileEntityNuclearReactorElectric reactor = getReactorInstance();
        return reactor != null && reactor.isValid(slot, stack);
    }

    @Override
    public void clear() {
        TileEntityNuclearReactorElectric reactor = getReactorInstance();
        if (reactor != null) {
            reactor.clear();
        }
    }

    @Override
    public int[] getAvailableSlots(Direction side) {
        TileEntityNuclearReactorElectric reactor = getReactorInstance();
        return reactor == null ? NO_SLOTS : reactor.getAvailableSlots(side);
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, Direction side) {
        TileEntityNuclearReactorElectric reactor = getReactorInstance();
        return reactor != null && reactor.canInsert(slot, stack, side);
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction side) {
        TileEntityNuclearReactorElectric reactor = getReactorInstance();
        return reactor != null && reactor.canExtract(slot, stack, side);
    }
}
