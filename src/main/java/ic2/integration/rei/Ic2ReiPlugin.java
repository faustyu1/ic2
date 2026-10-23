package ic2.integration.rei;

import ic2.api.crops.Crops;
import ic2.api.item.IBlockCuttingBlade;
import ic2.api.recipe.IBasicMachineRecipeManager;
import ic2.api.recipe.IElectrolyzerRecipeManager;
import ic2.api.recipe.IRecipeInput;
import ic2.api.recipe.MachineRecipe;
import ic2.api.recipe.Recipes;
import ic2.core.block.machine.tileentity.TileEntityCanner;
import ic2.core.gui.GuiIC2;
import ic2.core.init.IC2ItemVariants;
import ic2.core.init.IC2Items;
import ic2.core.item.ItemCropSeed;
import ic2.core.item.armor.jetpack.JetpackAttachmentRecipe;
import ic2.core.item.armor.jetpack.JetpackHandler;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.client.registry.entry.EntryRegistry;
import me.shedaniel.rei.api.client.registry.screen.ClickArea;
import me.shedaniel.rei.api.client.registry.screen.ScreenRegistry;
import me.shedaniel.rei.api.client.registry.transfer.TransferHandlerRegistry;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.entry.comparison.ItemComparatorRegistry;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import me.shedaniel.rei.api.common.util.EntryStacks;
import me.shedaniel.rei.plugin.common.BuiltinPlugin;
import me.shedaniel.rei.plugin.common.displays.crafting.DefaultCustomShapelessDisplay;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Roughly Enough Items support, port of ic2.jeiIntegration.SubModule.
 *
 * <p>Loaded through the {@code rei_client} entrypoint, so only when REI is installed. Each machine
 * gets a category laid out like its window, filled from IC2's recipe managers; the furnaces,
 * generators and crafting tables are workstations of the vanilla categories; the recipe buttons
 * of the machine windows open their category; the crop seeds are listed; the jetpack
 * attachment, a special recipe REI cannot show by itself, gets a display per chestplate; and
 * crafting recipes can be moved into the industrial workbench and the batch crafter.
 */
public class Ic2ReiPlugin implements REIClientPlugin {
    public static final CategoryIdentifier<MachineDisplay> MACERATOR = id("macerator");
    public static final CategoryIdentifier<MachineDisplay> EXTRACTOR = id("extractor");
    public static final CategoryIdentifier<MachineDisplay> COMPRESSOR = id("compressor");
    public static final CategoryIdentifier<MachineDisplay> CENTRIFUGE = id("centrifuge");
    public static final CategoryIdentifier<MachineDisplay> BLAST_FURNACE = id("blast_furnace");
    public static final CategoryIdentifier<MachineDisplay> ORE_WASHING = id("ore_washing_plant");
    public static final CategoryIdentifier<MachineDisplay> BLOCK_CUTTER = id("block_cutter");
    public static final CategoryIdentifier<MachineDisplay> RECYCLER = id("recycler");
    public static final CategoryIdentifier<MachineDisplay> SOLID_CANNER = id("solid_canner");
    public static final CategoryIdentifier<MachineDisplay> ELECTROLYZER = id("electrolyzer");
    public static final CategoryIdentifier<MachineDisplay> CANNER_BOTTLING = id("canner_bottle_solid");
    public static final CategoryIdentifier<MachineDisplay> CANNER_ENRICHING = id("canner_enrich_liquid");
    public static final CategoryIdentifier<MachineDisplay> SCRAPBOX = id("scrapbox");
    /** Extruding, rolling and cutting, in the order of the machine's modes. */
    public static final List<CategoryIdentifier<MachineDisplay>> METAL_FORMER = List.of(
            id("metal_former0"), id("metal_former1"), id("metal_former2"));
    private static final String[] METAL_FORMER_MODES = {"Extruding", "Rolling", "Cutting"};
    private static final String[] METAL_FORMER_ICONS = {"copper_cable_0", "forge_hammer", "cutter"};

    private static CategoryIdentifier<MachineDisplay> id(String path) {
        return CategoryIdentifier.of("ic2", path);
    }

    private static ItemStack block(String name) {
        return new ItemStack(Registries.ITEM.get(Identifier.of("ic2", name)));
    }

    @Override
    public void registerCategories(CategoryRegistry registry) {
        machine(registry, MACERATOR, "macerator", 300, Map.of());
        machine(registry, EXTRACTOR, "extractor", 300, Map.of());
        machine(registry, COMPRESSOR, "compressor", 300, Map.of());
        machine(registry, CENTRIFUGE, "centrifuge", 500, Map.of("output", 3));
        // the blast furnace is no standard machine, so JEI animated it at its default speed
        machine(registry, BLAST_FURNACE, "blast_furnace", 0, Map.of("output", 2));
        machine(registry, ORE_WASHING, "ore_washing_plant", 500, Map.of("output", 3));
        machine(registry, BLOCK_CUTTER, "block_cutter", 450, Map.of());
        machine(registry, RECYCLER, "recycler", 45, Map.of());
        machine(registry, SOLID_CANNER, "solid_canner", 200, Map.of());

        ItemStack metalFormer = block("metal_former");
        for (int mode = 0; mode < METAL_FORMER.size(); mode++) {
            Text title = metalFormer.getName().copy().append(": ")
                    .append(Text.translatable("MetalFormer.gui.switch." + METAL_FORMER_MODES[mode]));
            registry.add(new GuidefCategory(METAL_FORMER.get(mode), metalFormer, title, "metal_former", 200, Map.of())
                    .withOverlay(block(METAL_FORMER_ICONS[mode]), 70, 35));
            registry.addWorkstations(METAL_FORMER.get(mode), EntryStacks.of(metalFormer));
        }

        ItemStack electrolyzer = block("electrolyzer");
        registry.add(new ElectrolyzerCategory(ELECTROLYZER, electrolyzer));
        registry.addWorkstations(ELECTROLYZER, EntryStacks.of(electrolyzer));

        ItemStack canner = block("canner");
        registry.add(new CannerCategory(CANNER_BOTTLING, canner, CannerCategory.Activity.CANNING));
        registry.add(new CannerCategory(CANNER_ENRICHING, canner, CannerCategory.Activity.ENRICHING));
        registry.addWorkstations(CANNER_BOTTLING, EntryStacks.of(canner));
        registry.addWorkstations(CANNER_ENRICHING, EntryStacks.of(canner));

        ItemStack scrapBox = new ItemStack(IC2ItemVariants.get("crafting", "scrap_box"));
        registry.add(new ScrapboxCategory(SCRAPBOX, scrapBox));
        registry.addWorkstations(SCRAPBOX, EntryStacks.of(scrapBox));

        for (String furnace : new String[] {"iron_furnace", "electric_furnace", "induction_furnace"}) {
            registry.addWorkstations(BuiltinPlugin.SMELTING, EntryStacks.of(block(furnace)));
        }
        for (String generator : new String[] {"iron_furnace", "generator", "solid_heat_generator"}) {
            registry.addWorkstations(BuiltinPlugin.FUEL, EntryStacks.of(block(generator)));
        }
        for (String table : new String[] {"industrial_workbench", "batch_crafter"}) {
            registry.addWorkstations(BuiltinPlugin.CRAFTING, EntryStacks.of(block(table)));
        }
    }

    private static void machine(CategoryRegistry registry, CategoryIdentifier<MachineDisplay> id, String name,
                                int operationLength, Map<String, Integer> gridSizes) {
        ItemStack icon = block(name);
        registry.add(new GuidefCategory(id, icon, icon.getName(), name, operationLength, gridSizes));
        registry.addWorkstations(id, EntryStacks.of(icon));
    }

    @Override
    public void registerDisplays(DisplayRegistry registry) {
        basicMachine(registry, MACERATOR, Recipes.macerator);
        basicMachine(registry, EXTRACTOR, Recipes.extractor);
        basicMachine(registry, COMPRESSOR, Recipes.compressor);
        basicMachine(registry, CENTRIFUGE, Recipes.centrifuge);
        basicMachine(registry, BLAST_FURNACE, Recipes.blastfurnace);
        oreWashing(registry);
        basicMachine(registry, METAL_FORMER.get(0), Recipes.metalformerExtruding);
        basicMachine(registry, METAL_FORMER.get(1), Recipes.metalformerRolling);
        basicMachine(registry, METAL_FORMER.get(2), Recipes.metalformerCutting);
        blockCutter(registry);
        recycler(registry);
        electrolyzer(registry);
        canner(registry);
        scrapbox(registry);
        jetpackAttachment(registry);
    }

    private static EntryIngredient input(IRecipeInput input) {
        List<ItemStack> stacks = new ArrayList<>();
        for (ItemStack stack : input.getInputs()) {
            stacks.add(stack.copyWithCount(Math.max(1, input.getAmount())));
        }
        return EntryIngredients.ofItemStacks(stacks);
    }

    private static EntryIngredient ingredient(Ingredient ingredient, int count) {
        return EntryIngredients.ofItemStacks(Arrays.stream(ingredient.getMatchingStacks())
                .map(stack -> stack.copyWithCount(count)).toList());
    }

    /** Each output stack of a recipe in a slot of its own, as the machine gives it (see OutputUnifier). */
    private static List<EntryIngredient> outputs(Collection<ItemStack> stacks) {
        return ic2.core.recipe.OutputUnifier.unify(stacks).stream().map(EntryIngredients::of).toList();
    }

    private static void basicMachine(DisplayRegistry registry, CategoryIdentifier<MachineDisplay> id, IBasicMachineRecipeManager manager) {
        if (manager == null) {
            return;
        }
        for (MachineRecipe<IRecipeInput, Collection<ItemStack>> recipe : manager.getRecipes()) {
            registry.add(new MachineDisplay(id, List.of(input(recipe.getInput())), outputs(recipe.getOutput())));
        }
    }

    /** The ore washing plant also takes water, the amount the recipe's {@code @fluid} names. */
    private static void oreWashing(DisplayRegistry registry) {
        if (Recipes.oreWashing == null) {
            return;
        }
        for (MachineRecipe<IRecipeInput, Collection<ItemStack>> recipe : Recipes.oreWashing.getRecipes()) {
            int water = recipe.getMetaData() == null ? 0 : recipe.getMetaData().getInt("amount");
            List<EntryIngredient> inputs = water > 0
                    ? List.of(input(recipe.getInput()), ReiFluids.of("water", water))
                    : List.of(input(recipe.getInput()));
            registry.add(new MachineDisplay(ORE_WASHING, inputs, outputs(recipe.getOutput())));
        }
    }

    /** Port of IRecipeWrapperGenerator.blockCutter: every blade at least as hard as the block. */
    private static void blockCutter(DisplayRegistry registry) {
        if (Recipes.blockcutter == null) {
            return;
        }
        List<ItemStack> blades = new ArrayList<>();
        for (String type : new String[] {"iron", "steel", "diamond"}) {
            Item blade = IC2ItemVariants.get("block_cutting_blade", type);
            if (blade != null) {
                blades.add(new ItemStack(blade));
            }
        }

        for (MachineRecipe<IRecipeInput, Collection<ItemStack>> recipe : Recipes.blockcutter.getRecipes()) {
            int hardness = recipe.getMetaData() == null ? Integer.MAX_VALUE : recipe.getMetaData().getInt("hardness");
            List<ItemStack> fitting = blades.stream()
                    .filter(stack -> ((IBlockCuttingBlade) stack.getItem()).getHardness(stack) >= hardness)
                    .toList();
            registry.add(new MachineDisplay(BLOCK_CUTTER,
                    List.of(input(recipe.getInput()), EntryIngredients.ofItemStacks(fitting)),
                    outputs(recipe.getOutput())));
        }
    }

    /** Port of RecyclerCategory: anything off the blacklist, or only the whitelist when it has entries. */
    private static void recycler(DisplayRegistry registry) {
        Item scrap = IC2ItemVariants.get("crafting", "scrap");
        if (scrap == null) {
            return;
        }

        List<ItemStack> items = new ArrayList<>();
        if (Recipes.recyclerWhitelist == null || Recipes.recyclerWhitelist.isEmpty()) {
            for (Item item : Registries.ITEM) {
                ItemStack stack = item.getDefaultStack();
                if (!stack.isEmpty() && (Recipes.recyclerBlacklist == null || !Recipes.recyclerBlacklist.contains(stack))) {
                    items.add(stack);
                }
            }
        } else {
            for (IRecipeInput input : Recipes.recyclerWhitelist) {
                items.addAll(input.getInputs());
            }
        }
        registry.add(new MachineDisplay(RECYCLER, List.of(EntryIngredients.ofItemStacks(items)),
                List.of(EntryIngredients.of(scrap))));
    }

    private static void electrolyzer(DisplayRegistry registry) {
        if (Recipes.electrolyzer == null) {
            return;
        }
        for (Map.Entry<String, IElectrolyzerRecipeManager.ElectrolyzerRecipe> entry : Recipes.electrolyzer.getRecipeMap().entrySet()) {
            EntryIngredient input = ReiFluids.of(entry.getKey(), entry.getValue().inputAmount);
            if (input.isEmpty()) {
                continue;
            }
            List<EntryIngredient> outputs = Arrays.stream(entry.getValue().outputs)
                    .map(output -> ReiFluids.of(output.fluidName, output.fluidAmount))
                    .toList();
            registry.add(new MachineDisplay(ELECTROLYZER, List.of(input), outputs));
        }
    }

    /** Canning shows in the canner and in the solid canner, which share the recipes. */
    private static void canner(DisplayRegistry registry) {
        for (TileEntityCanner.BottleRecipe recipe : TileEntityCanner.BOTTLE_RECIPES) {
            List<EntryIngredient> inputs = List.of(ingredient(recipe.fill(), recipe.fillAmount()),
                    ingredient(recipe.container(), recipe.containerAmount()));
            List<EntryIngredient> outputs = List.of(EntryIngredients.of(recipe.output()));
            registry.add(new MachineDisplay(CANNER_BOTTLING, inputs, outputs));
            registry.add(new MachineDisplay(SOLID_CANNER, inputs, outputs));
        }

        for (TileEntityCanner.EnrichRecipe recipe : TileEntityCanner.ENRICH_RECIPES) {
            EntryIngredient input = ReiFluids.of(recipe.input(), recipe.inputAmount());
            EntryIngredient output = ReiFluids.of(recipe.output(), recipe.outputAmount());
            if (input.isEmpty() || output.isEmpty()) {
                continue;
            }
            registry.add(new MachineDisplay(CANNER_ENRICHING,
                    List.of(input, ingredient(recipe.additive(), recipe.additiveAmount())), List.of(output)));
        }
    }

    private static void scrapbox(DisplayRegistry registry) {
        Item box = IC2ItemVariants.get("crafting", "scrap_box");
        if (Recipes.scrapboxDrops == null || box == null) {
            return;
        }
        Recipes.scrapboxDrops.getDrops().entrySet().stream()
                .sorted(Map.Entry.<ItemStack, Float>comparingByValue(Comparator.reverseOrder()))
                .forEach(drop -> registry.add(new MachineDisplay(SCRAPBOX, List.of(EntryIngredients.of(box)),
                        List.of(EntryIngredients.of(drop.getKey())), ScrapboxCategory.chance(drop.getValue()))));
    }

    /** Port of JetpackRecipeWrapper: the attachment for each chestplate it accepts. */
    private static void jetpackAttachment(DisplayRegistry registry) {
        Item jetpack = Registries.ITEM.get(Identifier.of("ic2", "jetpack_electric"));
        Item plate = IC2ItemVariants.get("crafting", "jetpack_attachment_plate");
        if (plate == null) {
            return;
        }

        for (Item item : Registries.ITEM) {
            ItemStack armor = item.getDefaultStack();
            if (armor.isEmpty() || item == jetpack || JetpackAttachmentRecipe.blacklistedItems.contains(item)
                    || !JetpackHandler.isChestArmor(armor)) {
                continue;
            }
            ItemStack out = armor.copy();
            JetpackHandler.setJetpackAttached(out, true);
            registry.add(new DefaultCustomShapelessDisplay(null,
                    List.of(EntryIngredients.of(jetpack), EntryIngredients.of(plate), EntryIngredients.of(armor)),
                    List.of(EntryIngredients.of(out))));
        }
    }

    @Override
    public void registerScreens(ScreenRegistry registry) {
        // port of RecipeButton.jeiRecipeListOpener: the recipe buttons the machine windows define
        registry.registerClickArea(GuiIC2.class, context -> {
            String event = context.getScreen().recipeEventAt(context.getMousePosition().x, context.getMousePosition().y);
            CategoryIdentifier<?> category = categoryOf(event);
            return category == null ? ClickArea.Result.fail() : ClickArea.Result.success().category(category);
        });
    }

    /** The category a recipe button names, in the JEI uids the guidefs still use. */
    static CategoryIdentifier<?> categoryOf(String event) {
        if (event == null || event.isEmpty()) {
            return null;
        }
        return switch (event) {
            case "minecraft.smelting" -> BuiltinPlugin.SMELTING;
            case "minecraft.fuel" -> BuiltinPlugin.FUEL;
            case "minecraft.crafting" -> BuiltinPlugin.CRAFTING;
            default -> id(event);
        };
    }

    @Override
    public void registerTransferHandlers(TransferHandlerRegistry registry) {
        // the "+" of crafting recipes in the industrial workbench and the batch crafter
        registry.register(new Ic2TransferHandler());
    }

    @Override
    public void registerEntries(EntryRegistry registry) {
        // port of SubModule.onRuntimeAvailable: a seed bag of every crop
        if (Crops.instance == null) {
            return;
        }
        registry.addEntries(Crops.instance.getCrops().stream()
                .filter(Objects::nonNull)
                .map(crop -> EntryStacks.of(ItemCropSeed.generateItemStackFromValues(crop, 1, 1, 1, 4)))
                .toList());
    }

    @Override
    public void registerItemComparators(ItemComparatorRegistry registry) {
        // the seed bags of different crops are one item told apart by their data
        if (IC2Items.CROP_SEED != null) {
            registry.registerComponents(IC2Items.CROP_SEED);
        }
    }
}
