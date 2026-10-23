package ic2.core.ref;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import ic2.core.gui.GuiParser;
import java.util.TreeSet;
import org.junit.Test;

/** The GUI of a block is found by name, so the aliased blocks have to map back to their file. */
public class TeBlockGuiNameTest {

    @Test
    public void aliasesMapToTheirGuidef() {
        TreeSet<String> broken = new TreeSet<>();

        for (String alias : new String[] {"geothermalgenerator", "rtgenerator", "semfluid",
                "solar", "wind", "water", "kinetic", "stirling"}) {
            TeBlock block = TeBlock.valueOf(alias);
            if (GuiParser.parse(block.getGuiName()) == null) {
                broken.add(alias + " -> " + block.getGuiName());
            }
        }

        assertTrue("aliased blocks without a guidef: " + broken, broken.isEmpty());
    }

    @Test
    public void plainBlocksKeepTheirName() {
        assertNotNull(GuiParser.parse(TeBlock.macerator.getGuiName()));
        assertTrue(TeBlock.macerator.getGuiName().equals("macerator"));
    }
}
