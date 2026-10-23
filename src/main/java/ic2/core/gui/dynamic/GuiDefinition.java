package ic2.core.gui.dynamic;

import java.util.ArrayList;
import java.util.List;

/**
 * A parsed guidef file: the window size plus the elements it declares.
 *
 * <p>Both sides use the same definition - the screen handler reads the slot elements to build its
 * slots, the screen reads all of them to draw the window - which is what keeps the server and
 * client slot layout in sync without extra networking.
 */
public final class GuiDefinition {
    /** Default window size when the file does not state one, matching the vanilla chest GUI. */
    public static final int DEFAULT_WIDTH = 176;
    public static final int DEFAULT_HEIGHT = 166;

    public final String name;
    public final int width;
    public final int height;
    private final List<GuiElementNode> elements;

    public GuiDefinition(String name, int width, int height, List<GuiElementNode> elements) {
        this.name = name;
        this.width = width;
        this.height = height;

        List<GuiElementNode> flat = new ArrayList<>();
        flatten(elements, flat);
        this.elements = List.copyOf(flat);
    }

    /**
     * Every element of the file in document order, with {@code <environment>} wrappers already
     * flattened away and the children of the {@code <only>} and {@code <tooltip>} wrappers listed
     * next to their siblings - the original built its widgets by recursing into those wrappers as
     * well, and a slot inside a {@code <tooltip>} still has to end up in the container.
     */
    public List<GuiElementNode> getElements() {
        return elements;
    }

    private static void flatten(List<GuiElementNode> nodes, List<GuiElementNode> out) {
        for (GuiElementNode node : nodes) {
            out.add(node);
            flatten(node.getChildren(), out);
        }
    }

    /** Elements with the given tag name, in file order. */
    public List<GuiElementNode> getElements(String name) {
        return elements.stream().filter(element -> element.name.equals(name)).toList();
    }

    @Override
    public String toString() {
        return "GuiDefinition(" + name + ", " + width + "x" + height + ", " + elements.size() + " elements)";
    }
}
