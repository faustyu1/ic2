package ic2.core.gui;

import ic2.core.gui.dynamic.GuiDefinition;
import ic2.core.gui.dynamic.GuiElementNode;
import ic2.core.ref.TeBlock;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Reads the machine GUI descriptions from {@code assets/ic2/guidef/*.xml}.
 *
 * <p>The files are the 1.12.2 originals; only the consumer changed. Two wrapper elements are
 * resolved while parsing:
 * <ul>
 *   <li>{@code <environment name="game">} - everything the running game shows, as opposed to the
 *       standalone GUI editor the original shipped; its children are inlined.</li>
 *   <li>{@code <only if="...">} - kept as a node so the consumer can decide, since the conditions
 *       ({@code meta}, {@code energy}, {@code nbt}, {@code dev}) depend on runtime state.</li>
 * </ul>
 */
public final class GuiParser {
    private static final Logger LOG = LoggerFactory.getLogger("IC2-GUI");
    /** The environment the game screens render; a recipe viewer leaves its elements out. */
    public static final String ENVIRONMENT = "game";

    private static final Map<String, GuiDefinition> CACHE = new HashMap<>();

    private GuiParser() {}

    /** Parses the guidef of a block, e.g. {@code TeBlock.macerator} -> {@code guidef/macerator.xml}. */
    public static GuiDefinition parse(TeBlock teBlock) {
        return parse(teBlock.getName());
    }

    /**
     * Parses {@code assets/ic2/guidef/<name>.xml}, caching the result.
     *
     * @return the definition, or null when the file is missing or unreadable
     */
    public static GuiDefinition parse(String name) {
        return parse(name, ENVIRONMENT);
    }

    /**
     * Parses a guidef as the given environment sees it: the {@code <environment>} blocks of that
     * name are inlined, the others left out, as GuiParser.EnvironmentNode did for the game and JEI.
     */
    public static synchronized GuiDefinition parse(String name, String environment) {
        String key = environment.equals(ENVIRONMENT) ? name : name + "#" + environment;
        if (CACHE.containsKey(key)) {
            return CACHE.get(key);
        }

        GuiDefinition definition = read(name, environment);
        CACHE.put(key, definition);
        return definition;
    }

    private static GuiDefinition read(String name, String environment) {
        String path = "/assets/ic2/guidef/" + name + ".xml";

        try (InputStream is = GuiParser.class.getResourceAsStream(path)) {
            if (is == null) {
                LOG.debug("[IC2] No guidef for {}", name);
                return null;
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // the files ship with the mod, but there is no reason to allow external entities
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            DocumentBuilder builder = factory.newDocumentBuilder();

            Document document = builder.parse(is);
            Element root = document.getDocumentElement();

            List<GuiElementNode> elements = new ArrayList<>();
            collect(root, elements, environment);

            return new GuiDefinition(name,
                    attribute(root, "width", GuiDefinition.DEFAULT_WIDTH),
                    attribute(root, "height", GuiDefinition.DEFAULT_HEIGHT),
                    elements);
        } catch (IOException | ParserConfigurationException | SAXException e) {
            LOG.error("[IC2] Failed to parse guidef {}", path, e);
            return null;
        }
    }

    /** Collects the children of an element, inlining the environment the game renders. */
    private static void collect(Element parent, List<GuiElementNode> out, String environment) {
        NodeList children = parent.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            Element element = (Element) node;
            if (element.getTagName().equals("environment")) {
                if (environment.equals(element.getAttribute("name"))) {
                    collect(element, out, environment);
                }
                continue;
            }

            out.add(toNode(element, environment));
        }
    }

    private static GuiElementNode toNode(Element element, String environment) {
        Map<String, String> attributes = new LinkedHashMap<>();
        NamedNodeMap map = element.getAttributes();
        for (int i = 0; i < map.getLength(); i++) {
            Node attribute = map.item(i);
            attributes.put(attribute.getNodeName(), attribute.getNodeValue());
        }

        List<GuiElementNode> children = new ArrayList<>();
        collect(element, children, environment);

        GuiElementNode node = new GuiElementNode(element.getTagName(), attributes, children, textOf(element));
        for (GuiElementNode child : children) {
            child.setParent(node);
        }
        return node;
    }

    /** Direct text content of an element, ignoring the text of nested elements. */
    private static String textOf(Element element) {
        StringBuilder ret = new StringBuilder();
        NodeList children = element.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() == Node.TEXT_NODE || node.getNodeType() == Node.CDATA_SECTION_NODE) {
                ret.append(node.getNodeValue());
            }
        }

        return ret.toString().trim();
    }

    private static int attribute(Element element, String name, int fallback) {
        String value = element.getAttribute(name);
        if (value == null || value.isEmpty()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /** Drops the cache, so a resource reload picks up changed files. */
    public static synchronized void clearCache() {
        CACHE.clear();
    }
}
