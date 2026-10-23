package ic2.core.gametest;

import ic2.api.recipe.MachineRecipeResult;
import ic2.api.recipe.Recipes;
import ic2.core.init.IC2ItemVariants;
import ic2.core.init.IC2Items;
import ic2.core.util.ConfigUtil;
import java.util.Collection;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.Identifier;

/** IC2's copper is Minecraft's since 1.17, and raw metal crushes like ore. Run with {@code ./gradlew runGametest}. */
public class VanillaCopperGameTest implements FabricGameTest {
    @SuppressWarnings("unchecked")
    private static ItemStack macerate(ItemStack input) {
        MachineRecipeResult<?, ?, ?> result = Recipes.macerator.apply(input, false);
        return result == null ? ItemStack.EMPTY : ((Collection<ItemStack>) result.getOutput()).iterator().next();
    }

    /** The config names and the code reach the vanilla copper; the IC2 items are gone. */
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void copperIsMinecrafts(TestContext ctx) throws Exception {
        ctx.assertTrue(ConfigUtil.asStack("ic2:ingot#copper").isOf(Items.COPPER_INGOT), "ingot#copper");
        ctx.assertTrue(ConfigUtil.asStack("ic2:resource#copper_ore").isOf(Items.COPPER_ORE), "resource#copper_ore");
        ctx.assertTrue(ConfigUtil.asStack("ic2:resource#copper_block").isOf(Items.COPPER_BLOCK), "resource#copper_block");
        ctx.assertTrue(IC2Items.COPPER_INGOT == Items.COPPER_INGOT, "IC2Items.COPPER_INGOT");
        for (String id : new String[] {"ingot_copper", "copper_ore", "copper_block"}) {
            ctx.assertTrue(!Registries.ITEM.containsId(Identifier.of("ic2", id)), "ic2:" + id + " still registered");
        }

        ItemStack ingot = Recipes.furnace.apply(new ItemStack(IC2ItemVariants.get("crushed", "copper")), false).getOutput();
        ctx.assertTrue(ingot.isOf(Items.COPPER_INGOT), "crushed copper smelts to " + ingot);
        @SuppressWarnings("unchecked")
        Collection<ItemStack> block = (Collection<ItemStack>) Recipes.compressor.apply(new ItemStack(Items.COPPER_INGOT, 9), false).getOutput();
        ctx.assertTrue(block.iterator().next().isOf(Items.COPPER_BLOCK), "compressor makes " + block);
        ctx.assertTrue(Recipes.scrapboxDrops.getDrops().keySet().stream().anyMatch(stack -> stack.isOf(Items.COPPER_ORE)),
                "the scrap box lost its copper ore");
        ctx.assertTrue(Recipes.scrapboxDrops.getDrops().keySet().stream().noneMatch(ItemStack::isEmpty), "the scrap box drops air");
        ctx.complete();
    }

    /** Raw metal crushes like the ore it came from, a raw block like nine. */
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void rawMetalCrushes(TestContext ctx) {
        Item[][] cases = {
            {Items.RAW_COPPER, IC2ItemVariants.get("crushed", "copper")},
            {Items.RAW_IRON, IC2ItemVariants.get("crushed", "iron")},
            {Items.RAW_GOLD, IC2ItemVariants.get("crushed", "gold")},
        };
        Item[] blocks = {Items.RAW_COPPER_BLOCK, Items.RAW_IRON_BLOCK, Items.RAW_GOLD_BLOCK};
        for (int i = 0; i < cases.length; i++) {
            ItemStack raw = macerate(new ItemStack(cases[i][0]));
            ctx.assertTrue(raw.isOf(cases[i][1]) && raw.getCount() == 2, cases[i][0] + " -> " + raw);
            ItemStack block = macerate(new ItemStack(blocks[i]));
            ctx.assertTrue(block.isOf(cases[i][1]) && block.getCount() == 18, blocks[i] + " -> " + block);
        }
        ItemStack ore = macerate(new ItemStack(Items.DEEPSLATE_COPPER_ORE));
        ctx.assertTrue(ore.isOf(IC2ItemVariants.get("crushed", "copper")) && ore.getCount() == 2, "deepslate ore -> " + ore);
        ctx.complete();
    }
}
