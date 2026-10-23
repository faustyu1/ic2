package ic2.core.gui.dynamic;

/**
 * Background variants a {@code <slot>} or {@code <slotgrid>} can use, taken from
 * SlotGrid.SlotStyle of the 1.12.2 sources.
 *
 * <p>The style decides two things at once: which part of the common atlas is drawn behind the slot
 * and how far the 16x16 item sits inside it, which is why the screen handler needs it as well - the
 * slot position in the guidef names the background's corner, not the item's.
 */
public record SlotStyle(int u, int v, int width, int height, boolean hasBackground) {
    /** Size of the item drawn in a slot; the styles are centred around it. */
    public static final int ITEM_SIZE = 16;

    public static final SlotStyle NORMAL = new SlotStyle(103, 7, 18, 18, true);
    public static final SlotStyle LARGE = new SlotStyle(99, 35, 26, 26, true);
    /** Slots drawn by the machine's own background image, e.g. the block cutter's blade slot. */
    public static final SlotStyle PLAIN = new SlotStyle(0, 0, 16, 16, false);

    public static SlotStyle of(String name) {
        return switch (name.toLowerCase()) {
            case "large" -> LARGE;
            case "plain" -> PLAIN;
            default -> NORMAL;
        };
    }

    public static SlotStyle of(GuiElementNode element) {
        return of(element.get("style", "normal"));
    }

    public int insetX() {
        return (width - ITEM_SIZE) / 2;
    }

    public int insetY() {
        return (height - ITEM_SIZE) / 2;
    }
}
