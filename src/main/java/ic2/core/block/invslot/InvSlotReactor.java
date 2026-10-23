package ic2.core.block.invslot;

import ic2.core.block.reactor.tileentity.TileEntityNuclearReactorElectric;
import net.minecraft.item.ItemStack;

/**
 * Fabric port of InvSlotReactor - the reactor grid.
 *
 * <p>The grid is always stored as nine columns by six rows, but only the columns the built
 * chambers unlock are usable. The index mapping keeps the usable part contiguous, so a reactor
 * that grows or shrinks does not shuffle its contents.
 */
public class InvSlotReactor extends InvSlot {
    private static final int ROWS = 6;
    private static final int MAX_COLS = 9;

    public InvSlotReactor(TileEntityNuclearReactorElectric base, String name, int count) {
        super(base, name, Access.IO, count);
        setStackSizeLimit(1);
    }

    @Override
    public boolean accepts(ItemStack stack) {
        return reactor().isUsefulItem(stack, true);
    }

    @Override
    public int size() {
        return reactor().getReactorSize() * ROWS;
    }

    /** Number of slots the grid physically has, usable or not. */
    public int rawSize() {
        return super.size();
    }

    @Override
    public ItemStack get(int index) {
        return super.get(mapIndex(index));
    }

    /** The slot at a grid position, which is how the components address each other. */
    public ItemStack get(int x, int y) {
        return x < 0 || x >= MAX_COLS || y < 0 || y >= ROWS ? ItemStack.EMPTY : super.get(y * MAX_COLS + x);
    }

    @Override
    public void put(int index, ItemStack content) {
        super.put(mapIndex(index), content);
    }

    public void put(int x, int y, ItemStack content) {
        if (x >= 0 && x < MAX_COLS && y >= 0 && y < ROWS) {
            super.put(y * MAX_COLS + x, content);
        }
    }

    /** Maps a flat index onto the grid: the usable columns first, the locked ones behind them. */
    private int mapIndex(int index) {
        int size = size();
        int cols = size / ROWS;

        if (index < size) {
            return index / cols * MAX_COLS + index % cols;
        }

        index -= size;
        int remaining = MAX_COLS - cols;
        return index / remaining * MAX_COLS + cols + index % remaining;
    }

    private TileEntityNuclearReactorElectric reactor() {
        return (TileEntityNuclearReactorElectric) base;
    }
}
