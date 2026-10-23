package ic2.core.gametest;

import ic2.api.recipe.Recipes;
import ic2.core.init.IC2ItemVariants;
import ic2.core.init.MainConfig;
import ic2.core.recipe.BasicMachineRecipeManager;
import ic2.core.recipe.OutputUnifier;
import ic2.core.recipe.SmeltingRecipeManager;
import java.util.Collection;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;

/**
 * The {@code recipes/preferredOutputMods} setting. No other mod with IC2's metals runs here, so
 * Minecraft stands in for one: IC2's industrial diamond and the diamond share {@code c:gems/diamond}.
 * Run with {@code ./gradlew runGametest}.
 */
public class OutputUnifierGameTest implements FabricGameTest {
    /** Swaps only for a mod ranked above IC2 that has the material, in the machines' recipe managers too. */
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void preferredModsOutputs(TestContext ctx) {
        ItemStack industrial = new ItemStack(IC2ItemVariants.get("crafting", "industrial_diamond"), 3);
        BasicMachineRecipeManager machine = new BasicMachineRecipeManager();
        machine.addRecipe(Recipes.inputFactory.forStack(new ItemStack(Items.DIRT)), null, false, industrial.copy());
        SmeltingRecipeManager furnace = new SmeltingRecipeManager();
        furnace.addRecipe(new ItemStack(Items.DIRT), industrial.copy(), null, false);

        try {
            ctx.assertTrue(!OutputUnifier.isActive(), "the default changes outputs");
            ctx.assertTrue(OutputUnifier.unify(industrial) == industrial, "swapped by default");

            OutputUnifier.setPreferred(List.of("othermod", "ic2"));
            ctx.assertTrue(OutputUnifier.unify(industrial).isOf(industrial.getItem()), "swapped for a mod without the material");

            OutputUnifier.setPreferred(List.of("minecraft", "ic2"));
            ItemStack unified = OutputUnifier.unify(industrial);
            ctx.assertTrue(unified.isOf(Items.DIAMOND) && unified.getCount() == 3, "unified to " + unified);
            ctx.assertTrue(OutputUnifier.unify(new ItemStack(IC2ItemVariants.get("crushed", "tin"))).isOf(IC2ItemVariants.get("crushed", "tin")),
                    "an item without a counterpart changed");

            Collection<ItemStack> fromMachine = machine.apply(new ItemStack(Items.DIRT), false).getOutput();
            ctx.assertTrue(fromMachine.iterator().next().isOf(Items.DIAMOND), "machine gives " + fromMachine);
            ItemStack fromFurnace = furnace.apply(new ItemStack(Items.DIRT), false).getOutput();
            ctx.assertTrue(fromFurnace.isOf(Items.DIAMOND) && fromFurnace.getCount() == 3, "furnace gives " + fromFurnace);
            ctx.assertTrue(machine.getRecipes().iterator().next().getOutput().iterator().next().isOf(industrial.getItem()),
                    "the stored recipe was changed");

            // a mod not in the list never wins, so "minecraft" alone also prefers it to IC2
            OutputUnifier.setPreferred(List.of("minecraft"));
            ctx.assertTrue(OutputUnifier.unify(industrial).isOf(Items.DIAMOND), "unlisted ic2 kept its item");
        } finally {
            OutputUnifier.setPreferred(List.of("ic2"));
        }
        ctx.complete();
    }

    /** The default config goes to config/IC2.ini, where the player can change it. */
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void userConfigIsWritten(TestContext ctx) {
        MainConfig.get();
        java.nio.file.Path file = FabricLoader.getInstance().getConfigDir().resolve("IC2.ini");
        ctx.assertTrue(java.nio.file.Files.exists(file), "no " + file);
        ctx.assertTrue("ic2".equals(MainConfig.get().getString("recipes/preferredOutputMods", null)), "setting missing");
        ctx.complete();
    }
}
