package ic2.core.gui.dynamic;

/**
 * Geometry of a {@code <slotgrid>}, ported from GuiParser.SlotGridNode.getDimension.
 *
 * <p>A guidef grid states at most one of {@code rows}/{@code cols} - the upgrade grid is written as
 * {@code cols="1"}, the tank inventories as {@code rows="2"} - so the missing dimension follows from
 * how many slots the named group actually has. Both the screen handler and the screen build the
 * layout from this class, otherwise one of them would place fewer slots than the other.
 */
public record SlotGridLayout(int rows, int cols, boolean vertical, int offset, int spacing, SlotStyle style) {

    public static SlotGridLayout of(GuiElementNode element, int slotCount) {
        int spacing = element.getInt("spacing", 0);
        int offset = element.getInt("offset", 0);
        int rows = element.getInt("rows", -1);
        int cols = element.getInt("cols", -1);
        boolean vertical = element.getBool("vertical", false);
        SlotStyle style = SlotStyle.of(element);

        int total = slotCount - offset;
        if (total <= 0) {
            return new SlotGridLayout(0, 0, vertical, offset, spacing, style);
        }

        if (!vertical) {
            if (cols > 0) {
                rows = Math.max(rows, ceilDiv(total, cols));
            } else if (rows > 0) {
                cols = ceilDiv(total, rows);
            } else {
                cols = (int) Math.floor(Math.sqrt(total));
                rows = ceilDiv(total, Math.max(1, cols));
            }
        } else {
            if (rows > 0) {
                cols = Math.max(cols, ceilDiv(total, rows));
            } else if (cols > 0) {
                rows = ceilDiv(total, cols);
            } else {
                rows = (int) Math.floor(Math.sqrt(total));
                cols = ceilDiv(total, Math.max(1, rows));
            }
        }

        return new SlotGridLayout(Math.max(1, rows), Math.max(1, cols), vertical, offset, spacing, style);
    }

    /** Number of slots the grid shows of a group of the given size. */
    public int count(int slotCount) {
        return Math.max(0, Math.min(rows * cols, slotCount - offset));
    }

    /** Index inside the slot group of the n-th drawn cell. */
    public int slotIndex(int cell) {
        return offset + cell;
    }

    public int columnOf(int cell) {
        return vertical ? cell / rows : cell % cols;
    }

    public int rowOf(int cell) {
        return vertical ? cell % rows : cell / cols;
    }

    /** Horizontal distance between the corners of two neighbouring cells. */
    public int stepX() {
        return style.width() + spacing;
    }

    public int stepY() {
        return style.height() + spacing;
    }

    private static int ceilDiv(int value, int divisor) {
        return (value + divisor - 1) / divisor;
    }
}
