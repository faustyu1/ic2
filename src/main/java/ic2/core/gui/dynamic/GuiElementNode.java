package ic2.core.gui.dynamic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * One element of a parsed {@code assets/ic2/guidef/*.xml} file, e.g. a {@code <slot>} or a
 * {@code <gauge>}.
 *
 * <p>The original parser built a class per element type; keeping the generic form here means the
 * guidef files stay the single source of truth and new element types only need handling where they
 * are consumed - in the screen handler for slots and in the screen for everything visual.
 */
public final class GuiElementNode {
    public final String name;
    private final Map<String, String> attributes;
    private final List<GuiElementNode> children;
    private final String text;
    /** Set by the parser once the tree is built; the wrapper elements are read through it. */
    private GuiElementNode parent;

    public GuiElementNode(String name, Map<String, String> attributes, List<GuiElementNode> children, String text) {
        this.name = name;
        this.attributes = Map.copyOf(attributes);
        this.children = List.copyOf(children);
        this.text = text;
    }

    public List<GuiElementNode> getChildren() {
        return children;
    }

    /** The element this one is nested in, e.g. the {@code <only>} or {@code <tooltip>} wrapper. */
    public GuiElementNode getParent() {
        return parent;
    }

    public void setParent(GuiElementNode parent) {
        this.parent = parent;
    }

    /**
     * The conditions that have to hold for this element to show, outermost first. A
     * {@code <only if="scrap">} wrapper contributes one; nesting them means all of them apply.
     */
    public List<GuiElementNode> getConditions() {
        List<GuiElementNode> ret = new ArrayList<>();
        for (GuiElementNode node = parent; node != null; node = node.parent) {
            if (node.name.equals("only")) {
                ret.add(0, node);
            }
        }
        return ret;
    }

    /** Translation key of the nearest enclosing {@code <tooltip>}, or null when there is none. */
    public String getTooltipKey() {
        for (GuiElementNode node = parent; node != null; node = node.parent) {
            if (node.name.equals("tooltip")) {
                return node.get("text", null);
            }
        }
        return null;
    }

    public Map<String, String> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

    /** Element text content, e.g. the label of a {@code <text>} or {@code <button>}. */
    public String getText() {
        return text;
    }

    public boolean has(String attribute) {
        return attributes.containsKey(attribute);
    }

    public String get(String attribute, String fallback) {
        String value = attributes.get(attribute);
        return value == null ? fallback : value;
    }

    public int getInt(String attribute, int fallback) {
        String value = attributes.get(attribute);
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public boolean getBool(String attribute, boolean fallback) {
        String value = attributes.get(attribute);
        return value == null ? fallback : (value.isEmpty() || Boolean.parseBoolean(value.trim()));
    }

    public int getX() {
        return getInt("x", 0);
    }

    public int getY() {
        return getInt("y", 0);
    }

    public int getWidth(int fallback) {
        return getInt("width", fallback);
    }

    public int getHeight(int fallback) {
        return getInt("height", fallback);
    }

    @Override
    public String toString() {
        return "<" + name + " " + attributes + ">";
    }
}
