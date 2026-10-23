package ic2.core.gui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import ic2.core.gui.dynamic.GuiDefinition;
import ic2.core.gui.dynamic.GuiElementNode;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import org.junit.Test;

/** Covers the guidef reader against the files shipped in {@code assets/ic2/guidef}. */
public class GuiParserTest {
    private static final File GUIDEF = new File("src/main/resources/assets/ic2/guidef");

    @Test
    public void everyShippedGuidefParses() {
        List<String> failed = new ArrayList<>();

        for (String name : names()) {
            GuiDefinition definition = GuiParser.parse(name);
            if (definition == null) {
                failed.add(name);
                continue;
            }
            if (definition.width <= 0 || definition.height <= 0 || definition.getElements().isEmpty()) {
                failed.add(name + " (" + definition + ")");
            }
        }

        assertTrue("guidef files that did not parse: " + failed, failed.isEmpty());
    }

    @Test
    public void readsElementsAndAttributes() {
        GuiDefinition definition = GuiParser.parse("macerator");

        assertNotNull(definition);
        assertEquals(176, definition.width);
        assertEquals(166, definition.height);

        List<GuiElementNode> slots = definition.getElements("slot");
        assertEquals(3, slots.size());
        assertEquals("input", slots.get(0).get("name", ""));
        assertEquals(55, slots.get(0).getX());
        assertEquals(16, slots.get(0).getY());
        assertEquals("large", slots.get(1).get("style", ""));

        assertEquals(1, definition.getElements("playerInventory").size());
        assertEquals(1, definition.getElements("energygauge").size());
    }

    @Test
    public void inlinesTheGameEnvironment() {
        GuiDefinition definition = GuiParser.parse("macerator");

        // <slotgrid> and <button> live inside <environment name="game"> in the file
        assertTrue(definition.getElements().stream().noneMatch(e -> e.name.equals("environment")));
        assertEquals(1, definition.getElements("slotgrid").size());
        assertEquals("upgrade", definition.getElements("slotgrid").get(0).get("name", ""));
        assertEquals(1, definition.getElements("button").size());
    }

    @Test
    public void readsElementText() {
        GuiDefinition definition = GuiParser.parse("macerator");

        assertEquals("%name%", definition.getElements("text").get(0).getText());
    }

    @Test
    public void missingGuidefIsNull() {
        assertNull(GuiParser.parse("no_such_machine"));
    }

    @Test
    public void slotElementsNameTheirGroup() {
        TreeSet<String> unnamed = new TreeSet<>();

        for (String name : names()) {
            GuiDefinition definition = GuiParser.parse(name);
            if (definition == null) {
                continue;
            }
            for (GuiElementNode element : definition.getElements()) {
                if ((element.name.equals("slot") || element.name.equals("slotgrid"))
                        && element.get("name", "").isEmpty()) {
                    unnamed.add(name + ": " + element);
                }
            }
        }

        assertTrue("slot elements without a slot group name: " + unnamed, unnamed.isEmpty());
    }

    @Test
    public void nestedElementsAreListed() {
        GuiDefinition definition = GuiParser.parse("block_cutter");

        // the blade slot sits inside a <tooltip>, the "blade too weak" image inside <only><tooltip>
        assertEquals(4, definition.getElements("slot").size());
        assertTrue(definition.getElements("slot").stream()
                .anyMatch(element -> element.get("name", "").equals("cutterInputSlot")));
        assertEquals(1, definition.getElements("image").size());
    }

    @Test
    public void nestedElementsKnowTheirWrappers() {
        GuiDefinition definition = GuiParser.parse("block_cutter");

        GuiElementNode image = definition.getElements("image").get(0);
        assertEquals("ic2.BlockCutter.gui.bladeTooWeak", image.getTooltipKey());
        assertEquals(1, image.getConditions().size());
        assertEquals("isBladeTooWeak", image.getConditions().get(0).get("if", ""));

        GuiElementNode blade = definition.getElements("slot").stream()
                .filter(element -> element.get("name", "").equals("cutterInputSlot"))
                .findFirst().orElseThrow();
        assertEquals("ic2.BlockCutter.gui.bladeslot", blade.getTooltipKey());
        assertTrue(blade.getConditions().isEmpty());
    }

    @Test
    public void everyReferencedTextureExists() {
        TreeSet<String> missing = new TreeSet<>();

        for (String name : names()) {
            GuiDefinition definition = GuiParser.parse(name);
            if (definition == null) {
                continue;
            }
            for (GuiElementNode element : definition.getElements()) {
                String src = element.get("src", "");
                if (src.isEmpty()) {
                    continue;
                }

                // identifiers are lower case since 1.13, and so are the extracted assets, while
                // the 1.12.2 guidef files still spell names like "GUITermalCentrifuge"
                String path = src.contains(":") ? src.substring(src.indexOf(':') + 1) : src;
                path = path.toLowerCase();
                if (!path.endsWith(".png")) {
                    path = path + ".png";
                }

                if (!new File("src/main/resources/assets/ic2/" + path).isFile()) {
                    missing.add(src + " (" + name + ")");
                }
            }
        }

        assertTrue("guidef textures that do not exist: " + missing, missing.isEmpty());
    }

    private static List<String> names() {
        File[] files = GUIDEF.listFiles((dir, name) -> name.endsWith(".xml"));
        assertNotNull("guidef directory missing", files);

        List<String> ret = new ArrayList<>(files.length);
        for (File file : files) {
            ret.add(file.getName().substring(0, file.getName().length() - ".xml".length()));
        }
        return ret;
    }
}
