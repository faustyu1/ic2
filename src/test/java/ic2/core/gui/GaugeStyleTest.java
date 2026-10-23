package ic2.core.gui;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import ic2.core.gui.dynamic.GuiDefinition;
import ic2.core.gui.dynamic.GuiElementNode;
import java.io.File;
import java.util.TreeSet;
import org.junit.Test;

/** Every gauge a shipped guidef asks for has to have geometry, otherwise it is drawn as nothing. */
public class GaugeStyleTest {
    private static final File GUIDEF = new File("src/main/resources/assets/ic2/guidef");

    @Test
    public void everyGaugeStyleUsedByAGuidefResolves() {
        TreeSet<String> missing = new TreeSet<>();

        for (File file : files()) {
            String name = file.getName().substring(0, file.getName().length() - ".xml".length());
            GuiDefinition definition = GuiParser.parse(name);
            if (definition == null) {
                continue;
            }

            for (GuiElementNode element : definition.getElements()) {
                String style = element.get("style", "");
                switch (element.name) {
                    case "gauge" -> {
                        if (GaugeStyle.of(style) == null) {
                            missing.add(style + " (" + name + ")");
                        }
                    }
                    case "energygauge", "energyGauge" -> {
                        if (GaugeStyle.ofEnergy(style) == null) {
                            missing.add(style + " (" + name + ")");
                        }
                    }
                    default -> { }
                }
            }
        }

        assertTrue("gauge styles without geometry: " + missing, missing.isEmpty());
    }

    @Test
    public void gaugeTexturesExist() {
        TreeSet<String> missing = new TreeSet<>();

        for (String style : new String[] {"progressarrow", "progressorewasher", "progressblockcutter",
                "progresslongarrow", "progresscondenser", "progressfermenter", "heatfermenter",
                "heatnuclearreactor", "heatsteamgenerator", "calcificationsteamgenerator"}) {
            GaugeStyle gauge = GaugeStyle.of(style);
            assertNotNull(style, gauge);

            String path = gauge.texture().getPath();
            if (!new File("src/main/resources/assets/ic2/" + path).isFile()) {
                missing.add(style + " -> " + path);
            }
        }

        assertTrue("gauge textures that do not exist: " + missing, missing.isEmpty());
    }

    private static File[] files() {
        File[] files = GUIDEF.listFiles((dir, name) -> name.endsWith(".xml"));
        assertNotNull("guidef directory missing", files);
        return files;
    }
}
