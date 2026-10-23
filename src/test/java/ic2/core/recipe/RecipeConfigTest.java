package ic2.core.recipe;

import static org.junit.Assert.assertEquals;

import ic2.core.init.Rezepte;
import java.util.List;
import org.junit.Test;

/**
 * Parity checks for the parsing rules of {@code assets/ic2/config/*.ini}, covering the parts
 * that do not need a bootstrapped registry.
 */
public class RecipeConfigTest {
    @Test
    public void splitsOutputsOnWhitespace() {
        List<String> parts = Rezepte.splitWhitespace(
                "ic2:purified#silver ic2:dust#small_silver*2 ic2:dust#stone @fluid:1000");

        assertEquals(4, parts.size());
        assertEquals("ic2:purified#silver", parts.get(0));
        assertEquals("ic2:dust#small_silver*2", parts.get(1));
        assertEquals("ic2:dust#stone", parts.get(2));
        assertEquals("@fluid:1000", parts.get(3));
    }

    @Test
    public void keepsQuotedCraftingPatternsTogether() {
        List<String> parts = Rezepte.splitWhitespace("\"AA |B \" A:ingotTin B:ingotCopper");

        assertEquals(3, parts.size());
        assertEquals("\"AA |B \"", parts.get(0));
        assertEquals("A:ingotTin", parts.get(1));
        assertEquals("B:ingotCopper", parts.get(2));
    }

    @Test
    public void mapsRegularOreDictNamesToConventionalTags() {
        assertEquals("c:ingots/copper", OreDictionaryCompat.getTagId("ingotCopper").toString());
        assertEquals("c:ores/uranium", OreDictionaryCompat.getTagId("oreUranium").toString());
        assertEquals("c:storage_blocks/bronze", OreDictionaryCompat.getTagId("blockBronze").toString());
        assertEquals("c:dusts/silicon_dioxide", OreDictionaryCompat.getTagId("dustSiliconDioxide").toString());
        assertEquals("c:plates/dense_lead", OreDictionaryCompat.getTagId("plateDenseLead").toString());
        assertEquals("c:nuggets/uranium_235", OreDictionaryCompat.getTagId("nuggetUranium235").toString());
    }

    @Test
    public void prefersTheLongestOreDictPrefix() {
        assertEquals("ic2:crushed/gold", OreDictionaryCompat.getTagId("crushedGold").toString());
        assertEquals("ic2:purified/gold", OreDictionaryCompat.getTagId("crushedPurifiedGold").toString());
    }

    @Test
    public void mapsIrregularOreDictNamesExplicitly() {
        assertEquals("minecraft:leaves", OreDictionaryCompat.getTagId("treeLeaves").toString());
        assertEquals("c:rods/wooden", OreDictionaryCompat.getTagId("stickWood").toString());
        assertEquals("ic2:rubber", OreDictionaryCompat.getTagId("itemRubber").toString());
    }
}
