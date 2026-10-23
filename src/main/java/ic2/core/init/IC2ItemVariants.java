package ic2.core.init;

import ic2.core.block.BlockScaffold;
import ic2.core.block.BlockSheet;
import ic2.core.block.state.IIdProvider;
import ic2.core.block.transport.items.PipeSize;
import ic2.core.block.transport.items.PipeType;
import ic2.core.block.wiring.CableType;
import ic2.core.item.ItemCfPowder;
import ic2.core.item.ItemCrafting;
import ic2.core.item.ItemDust;
import ic2.core.item.ItemIC2;
import ic2.core.item.ItemBoatElectric;
import ic2.core.item.ItemIC2Boat;
import ic2.core.item.ItemIngot;
import ic2.core.item.ItemMisc;
import ic2.core.item.ItemPlate;
import ic2.core.item.ItemRadioactive;
import ic2.core.item.logistics.PumpCoverType;
import ic2.core.item.type.BlockCuttingBladeType;
import ic2.core.item.type.BoatType;
import ic2.core.item.type.CasingResourceType;
import ic2.core.item.type.CraftingItemType;
import ic2.core.item.type.CropResItemType;
import ic2.core.item.type.DustResourceType;
import ic2.core.item.type.IngotResourceType;
import ic2.core.item.type.MiscResourceType;
import ic2.core.item.type.MugType;
import ic2.core.item.type.NuclearResourceType;
import ic2.core.item.type.OreResourceType;
import ic2.core.item.type.PlateResourceType;
import ic2.core.item.type.TfbpType;
import ic2.core.item.type.UpgradeType;
import ic2.core.item.type.UpdateKitType;
import ic2.core.util.Ic2Color;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registration of the IC2 multi-metadata items as individual flattened items.
 *
 * <p>On 1.12.2 IC2 registered one {@code ItemMulti} per {@link ic2.core.ref.ItemName} group and
 * addressed the members by metadata, which the config files spell {@code ic2:dust#copper}. Since
 * the 1.13 flattening there is no item metadata, so every group member becomes its own registered
 * item named {@code ic2:<group>_<variant>}.
 *
 * <p>The {@code group#variant} keys are kept in a lookup map so the recipe parser can resolve the
 * original config syntax without rewriting the shipped ini files.
 */
public final class IC2ItemVariants {
    private static final Logger LOG = LoggerFactory.getLogger("IC2-Items");

    /** "group#variant" -> registered item. */
    private static final Map<String, Item> BY_KEY = new LinkedHashMap<>();

    /** Fluid names as they appear in the config files for {@code ic2:fluid_cell#<fluid>}. */
    private static final String[] FLUID_CELL_VARIANTS = {
        "water", "lava", "ic2air", "ic2biogas", "ic2biomass", "ic2biofuel", "ic2coolant",
        "ic2construction_foam", "ic2distilled_water", "ic2hot_coolant", "ic2hot_water",
        "ic2pahoehoe_lava", "ic2steam", "ic2superheated_steam", "ic2uu_matter", "ic2weed_ex"
    };

    /**
     * Variants Minecraft has had since 1.17, which IC2 used its own of because 1.12.2 had none:
     * copper. They are not registered; the config names and the code reach the vanilla item.
     */
    private static final Map<String, Item> VANILLA = Map.of(
            "ingot#copper", Items.COPPER_INGOT,
            "resource#copper_ore", Items.COPPER_ORE,
            "resource#copper_block", Items.COPPER_BLOCK);

    private IC2ItemVariants() {}

    public static void registerAll() {
        // resources - crushed/purified share the ore variant list
        group("crushed", OreResourceType.values(), ItemDust::new);
        group("purified", OreResourceType.values(), ItemDust::new);
        group("dust", DustResourceType.values(), ItemDust::new);
        group("ingot", IngotResourceType.values(), ItemIngot::new);
        group("plate", PlateResourceType.values(), ItemPlate::new);
        group("casing", CasingResourceType.values(), ItemPlate::new);
        // the nuclear materials irradiate whoever carries them without a hazmat suit, which in
        // 1.12.2 was ItemHandlers.radioactiveUpdate on the ItemMulti
        typedGroup("nuclear", NuclearResourceType.values(), ItemRadioactive::new);
        group("misc_resource", MiscResourceType.values(), ItemMisc::new);
        // two crafting items do something on their own: cf_powder sets water, a scrap box opens
        group("crafting", CraftingItemType.values(),
                name -> switch (name) {
                    case "crafting_cf_powder" -> new ItemCfPowder(name);
                    case "crafting_scrap_box" -> new ic2.core.item.ItemScrapBox(name);
                    default -> new ItemCrafting(name);
                });
        group("crop_res", CropResItemType.values(), ItemMisc::new);

        // machine upgrades and kits
        typedGroup("upgrade", UpgradeType.values(), ic2.core.item.ItemUpgradeModule::new);
        group("upgrade_kit", UpdateKitType.values(), ItemIC2::new);
        typedGroup("block_cutting_blade", BlockCuttingBladeType.values(), ic2.core.item.ItemBlockCuttingBlade::new);

        // tools / containers with a variant axis; the blank forms are addressed without a variant
        registerBare("tfbp", new ItemIC2("tfbp"));
        typedGroup("tfbp", TfbpType.values(), ic2.core.item.tfbp.ItemTfbp::new);
        group("boat", BoatType.values(), name -> name.endsWith("_electric") ? new ItemBoatElectric(name) : new ItemIC2Boat(name));
        typedGroup("mug", MugType.values(), ic2.core.item.ItemMug::new);
        registerBare("painter", new ic2.core.item.tool.ItemToolPainter("painter", null));
        typedGroup("painter", Ic2Color.values(), ic2.core.item.tool.ItemToolPainter::new);

        // fluid_cell is addressed both bare (empty cell) and by fluid name
        registerBare("fluid_cell", new ItemIC2("fluid_cell"));
        variants("fluid_cell", FLUID_CELL_VARIANTS, ItemIC2::new);

        registerCables();
        registerPipes();
        typedGroup("cover", PumpCoverType.values(), ic2.core.item.logistics.ItemPumpCover::new);

        // placeable variants: they all place the single ported block until the block states that
        // used to carry the metadata are ported as well
        blockGroup("sheet", BlockSheet.SheetType.values(), IC2ClassicBlocks.SHEET);
        blockGroup("scaffold", BlockScaffold.ScaffoldType.values(), IC2ClassicBlocks.SCAFFOLD);

        // the mining pipe is addressed as mining_pipe#pipe by the config, but is a single item
        Item miningPipe = registerBare("mining_pipe", new ItemIC2("mining_pipe"));
        alias("mining_pipe#pipe", miningPipe);

        LOG.info("[IC2] Registered {} flattened item variants", BY_KEY.size());
    }

    /**
     * Registers the cable items, keyed the way the config addresses them
     * ({@code ic2:cable#type:copper,insulation:1}) and named the way
     * {@link CableType#getName(int, ic2.core.util.Ic2Color)} does.
     */
    private static void registerCables() {
        for (CableType type : CableType.values()) {
            for (int insulation = 0; insulation <= type.maxInsulation; insulation++) {
                // the block of each variant is registered with its item by IC2Blocks; here the
                // config keys are pointed at it
                String name = type.getName(insulation, null);
                Item item = Registries.ITEM.get(Identifier.of("ic2", name));
                BY_KEY.put("cable#type:" + type.getName() + ",insulation:" + insulation, item);
            }
        }
    }

    /**
     * Points the config keys of the fluid pipes ({@code ic2:pipe#type:bronze,size:tiny}) at their
     * block items, which IC2Blocks registers with the pipe blocks.
     */
    private static void registerPipes() {
        for (PipeType type : PipeType.values()) {
            for (PipeSize size : PipeSize.values()) {
                Item item = Registries.ITEM.get(Identifier.of("ic2", type.getName(size)));
                BY_KEY.put("pipe#type:" + type.getName() + ",size:" + size.getName(), item);
            }
        }
    }

    /** Registers a group whose members place a block, e.g. the sheet and scaffold variants. */
    private static <T extends Enum<T> & IIdProvider> void blockGroup(String group, T[] values, Block block) {
        for (T value : values) {
            String name = group + "_" + value.getName();
            Item item = Registry.register(Registries.ITEM, Identifier.of("ic2", name),
                    new BlockItem(block, new Item.Settings()));
            BY_KEY.put(group + "#" + value.getName(), item);
        }
    }

    /** Adds a second key for an already registered item. */
    private static void alias(String key, Item item) {
        BY_KEY.put(key, item);
    }

    /** Registers every member of an {@link IIdProvider} enum as {@code ic2:<group>_<variant>}. */
    private static <T extends Enum<T> & IIdProvider> void group(String group, T[] values, Function<String, Item> factory) {
        for (T value : values) {
            register(group, value.getName(), factory);
        }
    }

    /** Same, for groups whose item needs the enum member it was made from. */
    private static <T extends Enum<T> & IIdProvider> void typedGroup(String group, T[] values, BiFunction<String, T, Item> factory) {
        for (T value : values) {
            register(group, value.getName(), name -> factory.apply(name, value));
        }
    }

    /** Registers an explicit variant list for groups whose members are not backed by an enum. */
    private static void variants(String group, String[] names, Function<String, Item> factory) {
        for (String name : names) {
            register(group, name, factory);
        }
    }

    private static void register(String group, String variant, Function<String, Item> factory) {
        Item vanilla = VANILLA.get(group + "#" + variant);
        if (vanilla != null) {
            BY_KEY.put(group + "#" + variant, vanilla);
            return;
        }
        String name = group + "_" + variant;
        Item item = Registry.register(Registries.ITEM, Identifier.of("ic2", name), factory.apply(name));
        BY_KEY.put(group + "#" + variant, item);
    }

    /** Registers a group's bare form, i.e. the item the config addresses without a {@code #variant}. */
    private static Item registerBare(String group, Item item) {
        Item registered = Registry.register(Registries.ITEM, Identifier.of("ic2", group), item);
        BY_KEY.put(group, registered);
        return registered;
    }

    /**
     * Resolves a config item reference of the original {@code <group>#<variant>} form.
     *
     * @param group   group name, e.g. {@code dust}
     * @param variant variant name, or null for the group's bare item
     * @return the registered item, or null if the group/variant pair is unknown
     */
    public static Item get(String group, String variant) {
        String key = variant == null ? group : group + "#" + variant;
        Item item = BY_KEY.get(key);
        return item != null ? item : VANILLA.get(key);
    }

    /** Unmodifiable view of all registered variants keyed by {@code group#variant}. */
    public static Map<String, Item> getAll() {
        return Collections.unmodifiableMap(BY_KEY);
    }
}
