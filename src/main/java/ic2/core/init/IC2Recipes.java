package ic2.core.init;

import ic2.api.recipe.Recipes;
import ic2.core.LiquidHeatExchangerManager;
import ic2.core.ref.FluidName;
import ic2.core.util.ConfigUtil;
import ic2.core.recipe.RecipeInputFactory;
import ic2.core.recipe.SmeltingRecipeManager;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Recipe bootstrap: creates the recipe managers, then loads {@code assets/ic2/config/*.ini}
 * through {@link Rezepte} so the recipe values match 1.12.2 exactly.
 */
public final class IC2Recipes {
    private static final Logger LOG = LoggerFactory.getLogger("IC2-Recipes");

    /** UU-matter scan values from {@code uu_scan_values.ini}, keyed by the raw config item name. */
    private static Map<String, Double> scanValues = Map.of();

    private IC2Recipes(){}

    public static void init(){
        if (Recipes.inputFactory == null) {
            Recipes.inputFactory = new RecipeInputFactory();
        }

        // machine recipe managers - each machine's init() is idempotent
        ic2.core.block.machine.tileentity.TileEntityMacerator.init();
        ic2.core.block.machine.tileentity.TileEntityCompressor.init();
        ic2.core.block.machine.tileentity.TileEntityExtractor.init();
        ic2.core.block.machine.tileentity.TileEntityElectricFurnace.init();
        ic2.core.block.machine.tileentity.TileEntityRecycler.init();
        ic2.core.block.machine.tileentity.TileEntityCentrifuge.init();
        ic2.core.block.machine.tileentity.TileEntityBlastFurnace.init();
        ic2.core.block.machine.tileentity.TileEntityMetalFormer.init();
        ic2.core.block.machine.tileentity.TileEntityOreWashing.init();
        ic2.core.block.machine.tileentity.TileEntityBlockCutter.init();
        if (Recipes.furnace == null) {
            Recipes.furnace = new SmeltingRecipeManager();
        }

        initFluidManagers();
        ic2.core.recipe.ScrapboxRecipeManager.init();

        Rezepte.loadRecipes();
        scanValues = Rezepte.loadScanValues();
        ic2.core.uu.UuIndex.getInstance().load(scanValues);
        LOG.info("[IC2] Recipe loading finished");
    }

    /**
     * The fluid recipe managers. They have to exist before the first machine is built, because a
     * tank binds to its manager when the tile entity is constructed - in 1.12.2 this was the
     * {@code init()} of the liquid heat exchanger and of the semifluid generator.
     */
    private static void initFluidManagers() {
        ic2.core.block.generator.tileentity.TileEntitySemifluidGenerator.init();
        ic2.core.block.heatgenerator.tileentity.TileEntityFluidHeatGenerator.init();
        ic2.core.block.machine.tileentity.TileEntityElectrolyzer.init();
        ic2.core.block.machine.tileentity.TileEntityMatter.init();

        if (Recipes.liquidHeatupManager == null) {
            Recipes.liquidCooldownManager = new LiquidHeatExchangerManager(false);
            Recipes.liquidHeatupManager = new LiquidHeatExchangerManager(true);

            addCooldownRecipe("lava", FluidName.pahoehoe_lava.getName(),
                    Math.round(20.0f * (float) ConfigUtil.getDouble(MainConfig.get(),
                            "balance/energy/fluidconversion/heatExchangerLava")));
            addBiDiRecipe(FluidName.hot_coolant.getName(), FluidName.coolant.getName(),
                    Math.round(20.0f * (float) ConfigUtil.getDouble(MainConfig.get(),
                            "balance/energy/fluidconversion/heatExchangerHotCoolant")));
            addHeatupRecipe(FluidName.hot_water.getName(), "water",
                    Math.round(1.0f * (float) ConfigUtil.getDouble(MainConfig.get(),
                            "balance/energy/fluidconversion/heatExchangerWater")));
        }
    }

    /** The fluid pair works in both directions, like coolant and hot coolant. */
    public static void addBiDiRecipe(String hotFluid, String coldFluid, int huPerMB) {
        addHeatupRecipe(hotFluid, coldFluid, huPerMB);
        addCooldownRecipe(hotFluid, coldFluid, huPerMB);
    }

    public static void addHeatupRecipe(String hotFluid, String coldFluid, int huPerMB) {
        Recipes.liquidHeatupManager.addFluid(coldFluid, hotFluid, huPerMB);
    }

    public static void addCooldownRecipe(String hotFluid, String coldFluid, int huPerMB) {
        Recipes.liquidCooldownManager.addFluid(hotFluid, coldFluid, huPerMB);
    }

    /** UU-matter scan values, keyed by the config item reference, e.g. {@code minecraft:cobblestone}. */
    public static Map<String, Double> getScanValues() {
        return scanValues;
    }
}
