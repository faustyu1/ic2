package ic2.core.ref;

import ic2.core.block.state.IIdProvider;
import java.util.Set;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;
import net.minecraft.util.math.Direction;

public enum TeBlock implements IIdProvider {
    DUMMY,
    invalid,
    generator,
    geothermalgenerator,
    solar,
    wind,
    water,
    kinetic,
    rtgenerator,
    semfluid,
    stirling,
    batbox,
    cesu,
    mfe,
    mfsu,
    cable,
    detector_cable,
    splitter_cable,
    lv_transformer,
    mv_transformer,
    hv_transformer,
    ev_transformer,
    chargepad_batbox,
    chargepad_cesu,
    chargepad_mfe,
    chargepad_mfsu,
    luminator_flat,
    electric_furnace,
    macerator,
    extractor,
    compressor,
    recycler,
    centrifuge,
    blast_furnace,
    metal_former,
    ore_washing_plant,
    block_cutter,
    canner,
    solid_canner,
    fluid_bottler,
    induction_furnace,
    mass_fabricator,
    matter_generator,
    electric_heat_generator,
    fluid_heat_generator,
    rt_heat_generator,
    solid_heat_generator,
    electric_kinetic_generator,
    manual_kinetic_generator,
    steam_kinetic_generator,
    stirling_kinetic_generator,
    water_kinetic_generator,
    wind_kinetic_generator,
    nuclear_reactor,
    reactor_chamber,
    reactor_access_hatch,
    reactor_fluid_port,
    reactor_redstone_port,
    reactor_vessel,
    energy_o_mat,
    personal_chest,
    trade_o_mat,
    crop,
    bronze_tank,
    iron_tank,
    steel_tank,
    iridium_tank,
    pump,
    miner,
    advanced_miner,
    crop_harvester,
    cropmatron,
    liquid_heat_exchanger,
    condenser,
    teleporter,
    magnetizer,
    tesla_coil,
    sorting_machine,
    electrolyzer,
    fermenter,
    coke_kiln,
    burning_box,
    steam_generator,
    tank,
    terraformer,
    chunk_loader,
    iron_furnace,
    fluid_distributor,
    fluid_regulator,
    weighted_fluid_distributor,
    weighted_item_distributor,
    industrial_workbench,
    batch_crafter,
    wooden_storage_box,
    iron_storage_box,
    bronze_storage_box,
    steel_storage_box,
    iridium_storage_box,
    fluid_pipe,
    coke_kiln_hatch,
    coke_kiln_grate,
    solar_distiller,
    creative_generator,
    rci_rsh,
    rci_lzh,
    steam_repressurizer,
    barrel,
    wall,
    itnt,
    nuke,
    item_buffer,
    item_buffer_2,
    pattern_storage,
    replicator,
    scanner,
    trading_terminal,
    uu_assembly_bench,
    // original naming aliases for 1:1 parity
    geo_generator,
    kinetic_generator,
    rt_generator,
    semifluid_generator,
    solar_generator,
    stirling_generator,
    water_generator,
    wind_generator;

    public enum HarvestTool { None, Pickaxe, Shovel, Axe, Wrench }
    public enum DefaultDrop { Self, None, Generator, Machine, AdvMachine }
    public interface ITePlaceHandler {}

    @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target(java.lang.annotation.ElementType.TYPE)
    public @interface Delegated {
        Class<?> current();
        Class<?> old() default Object.class;
    }

    public String getName() { return name(); }

    /**
     * Name of the block's {@code assets/ic2/guidef/<name>.xml}.
     *
     * <p>Some blocks are registered under the short pre-1.7 name this enum kept as an alias
     * ({@code rtgenerator}), while the GUI files are named after the 1.12.2 block
     * ({@code rt_generator}), so the alias is mapped back here.
     */
    public String getGuiName() { return GUI_NAMES.getOrDefault(name(), name()); }

    private static final java.util.Map<String, String> GUI_NAMES = java.util.Map.of(
            "geothermalgenerator", "geo_generator",
            "rtgenerator", "rt_generator",
            "semfluid", "semifluid_generator",
            "solar", "solar_generator",
            "wind", "wind_generator",
            "water", "water_generator",
            "kinetic", "kinetic_generator",
            "stirling", "stirling_generator");

    public int getId() { return ordinal(); }
    public Identifier getIdentifier() { return Identifier.of("ic2", getName()); }
    /** Whether the block has an item of its own; the crop, the barrel and the foam wall do not. */
    public boolean hasItem() { return info().hasItem(); }
    public boolean hasActive() { return false; }
    /** The ways the wrench may turn the block; empty when it has no facing. */
    public Set<Direction> getSupportedFacings() { return info().facings().directions; }
    /** -1 for the blocks only their owner can take, as the personal safe. */
    public float getHardness() { return info().hardness(); }
    public float getExplosionResistance() { return info().resistance(); }
    public HarvestTool getHarvestTool() { return info().tool(); }
    /** What the block drops when it is mined rather than wrenched. */
    public DefaultDrop getDefaultDrop() { return info().drop(); }
    public Rarity getRarity() { return info().rarity(); }
    public boolean allowWrenchRotating() { return info().rotate(); }
    /** Whether only the harvest tool gets anything out of the block, from its 1.12.2 material. */
    public boolean requiresTool() { return info().requiresTool(); }

    private Info info() {
        return INFO.get(this);
    }

    /** The facings a TeBlock allows, the sets 1.12.2 kept in Util. */
    public enum Facings {
        NONE(Set.of()),
        HORIZONTAL(Set.of(Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST)),
        ALL(Set.of(Direction.values())),
        DOWN_SIDE(Set.of(Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST));

        public final Set<Direction> directions;

        Facings(Set<Direction> directions) {
            this.directions = directions;
        }
    }

    private record Info(HarvestTool tool, DefaultDrop drop, float hardness, float resistance, Rarity rarity,
                        boolean requiresTool, Facings facings, boolean rotate, boolean hasItem) {
    }

    private static final java.util.Map<TeBlock, Info> INFO = new java.util.EnumMap<>(TeBlock.class);

    private static void info(TeBlock block, HarvestTool tool, DefaultDrop drop, float hardness, float resistance,
                             Rarity rarity, boolean requiresTool, Facings facings, boolean rotate, boolean hasItem) {
        INFO.put(block, new Info(tool, drop, hardness, resistance, rarity, requiresTool, facings, rotate, hasItem));
    }

    // the 1.12.2 constructor arguments of every block, aliases included
    static {
        // BEGIN GENERATED by tools/gen_block_drops.py
        info(DUMMY, HarvestTool.None, DefaultDrop.None, 5.0f, 10.0f, Rarity.COMMON, false, Facings.NONE, false, true);
        info(invalid, HarvestTool.None, DefaultDrop.None, 5.0f, 10.0f, Rarity.COMMON, false, Facings.NONE, false, true);
        info(generator, HarvestTool.Wrench, DefaultDrop.Self, 2.0f, 10.0f, Rarity.COMMON, true, Facings.HORIZONTAL, true, true);
        info(geothermalgenerator, HarvestTool.Wrench, DefaultDrop.Generator, 2.0f, 10.0f, Rarity.COMMON, true, Facings.HORIZONTAL, true, true);
        info(solar, HarvestTool.Wrench, DefaultDrop.Generator, 2.0f, 10.0f, Rarity.COMMON, true, Facings.HORIZONTAL, true, true);
        info(wind, HarvestTool.Wrench, DefaultDrop.Generator, 2.0f, 10.0f, Rarity.COMMON, true, Facings.HORIZONTAL, true, true);
        info(water, HarvestTool.Wrench, DefaultDrop.Self, 2.0f, 10.0f, Rarity.COMMON, true, Facings.HORIZONTAL, true, true);
        info(kinetic, HarvestTool.Wrench, DefaultDrop.Generator, 2.0f, 10.0f, Rarity.COMMON, true, Facings.ALL, true, true);
        info(rtgenerator, HarvestTool.Wrench, DefaultDrop.Generator, 2.0f, 10.0f, Rarity.COMMON, true, Facings.HORIZONTAL, true, true);
        info(semfluid, HarvestTool.Wrench, DefaultDrop.Generator, 2.0f, 10.0f, Rarity.COMMON, true, Facings.HORIZONTAL, true, true);
        info(stirling, HarvestTool.Wrench, DefaultDrop.Generator, 2.0f, 10.0f, Rarity.COMMON, true, Facings.ALL, true, true);
        info(batbox, HarvestTool.Wrench, DefaultDrop.Self, 2.0f, 10.0f, Rarity.COMMON, true, Facings.ALL, true, true);
        info(cesu, HarvestTool.Wrench, DefaultDrop.Self, 2.0f, 10.0f, Rarity.COMMON, true, Facings.ALL, true, true);
        info(mfe, HarvestTool.Wrench, DefaultDrop.Machine, 2.0f, 10.0f, Rarity.COMMON, true, Facings.ALL, true, true);
        info(mfsu, HarvestTool.Wrench, DefaultDrop.AdvMachine, 2.0f, 10.0f, Rarity.UNCOMMON, true, Facings.ALL, true, true);
        info(cable, HarvestTool.Pickaxe, DefaultDrop.Self, 0.5f, 5.0f, Rarity.COMMON, false, Facings.NONE, false, true);
        info(detector_cable, HarvestTool.Pickaxe, DefaultDrop.Self, 0.5f, 5.0f, Rarity.COMMON, false, Facings.NONE, false, false);
        info(splitter_cable, HarvestTool.Pickaxe, DefaultDrop.Self, 0.5f, 5.0f, Rarity.COMMON, false, Facings.NONE, false, false);
        info(lv_transformer, HarvestTool.Wrench, DefaultDrop.Self, 2.0f, 10.0f, Rarity.COMMON, true, Facings.ALL, true, true);
        info(mv_transformer, HarvestTool.Wrench, DefaultDrop.Machine, 2.0f, 10.0f, Rarity.COMMON, true, Facings.ALL, true, true);
        info(hv_transformer, HarvestTool.Wrench, DefaultDrop.Machine, 2.0f, 10.0f, Rarity.UNCOMMON, true, Facings.ALL, true, true);
        info(ev_transformer, HarvestTool.Wrench, DefaultDrop.Machine, 2.0f, 10.0f, Rarity.UNCOMMON, true, Facings.ALL, true, true);
        info(chargepad_batbox, HarvestTool.Wrench, DefaultDrop.Self, 2.0f, 10.0f, Rarity.COMMON, true, Facings.DOWN_SIDE, true, true);
        info(chargepad_cesu, HarvestTool.Wrench, DefaultDrop.Self, 2.0f, 10.0f, Rarity.COMMON, true, Facings.DOWN_SIDE, true, true);
        info(chargepad_mfe, HarvestTool.Wrench, DefaultDrop.Self, 2.0f, 10.0f, Rarity.COMMON, true, Facings.DOWN_SIDE, true, true);
        info(chargepad_mfsu, HarvestTool.Wrench, DefaultDrop.Self, 2.0f, 10.0f, Rarity.UNCOMMON, true, Facings.DOWN_SIDE, true, true);
        info(luminator_flat, HarvestTool.Pickaxe, DefaultDrop.Self, 5.0f, 10.0f, Rarity.COMMON, true, Facings.ALL, false, true);
        info(electric_furnace, HarvestTool.Wrench, DefaultDrop.Self, 2.0f, 10.0f, Rarity.COMMON, true, Facings.HORIZONTAL, true, true);
        info(macerator, HarvestTool.Wrench, DefaultDrop.Machine, 2.0f, 10.0f, Rarity.COMMON, true, Facings.HORIZONTAL, true, true);
        info(extractor, HarvestTool.Wrench, DefaultDrop.Machine, 2.0f, 10.0f, Rarity.COMMON, true, Facings.HORIZONTAL, true, true);
        info(compressor, HarvestTool.Wrench, DefaultDrop.Machine, 2.0f, 10.0f, Rarity.COMMON, true, Facings.HORIZONTAL, true, true);
        info(recycler, HarvestTool.Wrench, DefaultDrop.Machine, 2.0f, 10.0f, Rarity.COMMON, true, Facings.HORIZONTAL, true, true);
        info(centrifuge, HarvestTool.Wrench, DefaultDrop.Machine, 2.0f, 10.0f, Rarity.COMMON, true, Facings.HORIZONTAL, true, true);
        info(blast_furnace, HarvestTool.Wrench, DefaultDrop.Machine, 2.0f, 10.0f, Rarity.COMMON, true, Facings.ALL, true, true);
        info(metal_former, HarvestTool.Wrench, DefaultDrop.Machine, 2.0f, 10.0f, Rarity.COMMON, true, Facings.HORIZONTAL, true, true);
        info(ore_washing_plant, HarvestTool.Wrench, DefaultDrop.Machine, 2.0f, 10.0f, Rarity.COMMON, true, Facings.HORIZONTAL, true, true);
        info(block_cutter, HarvestTool.Wrench, DefaultDrop.AdvMachine, 2.0f, 10.0f, Rarity.COMMON, true, Facings.HORIZONTAL, true, true);
        info(canner, HarvestTool.Wrench, DefaultDrop.Machine, 2.0f, 10.0f, Rarity.COMMON, true, Facings.HORIZONTAL, true, true);
        info(solid_canner, HarvestTool.Wrench, DefaultDrop.Machine, 2.0f, 10.0f, Rarity.COMMON, true, Facings.HORIZONTAL, true, true);
        info(fluid_bottler, HarvestTool.Wrench, DefaultDrop.Machine, 2.0f, 10.0f, Rarity.COMMON, true, Facings.HORIZONTAL, true, true);
        info(induction_furnace, HarvestTool.Wrench, DefaultDrop.AdvMachine, 2.0f, 10.0f, Rarity.UNCOMMON, true, Facings.HORIZONTAL, true, true);
        info(mass_fabricator, HarvestTool.Wrench, DefaultDrop.AdvMachine, 2.0f, 10.0f, Rarity.RARE, true, Facings.HORIZONTAL, true, true);
        info(matter_generator, HarvestTool.Wrench, DefaultDrop.AdvMachine, 2.0f, 10.0f, Rarity.RARE, true, Facings.HORIZONTAL, true, true);
        info(electric_heat_generator, HarvestTool.Wrench, DefaultDrop.Self, 2.0f, 10.0f, Rarity.COMMON, true, Facings.ALL, true, true);
        info(fluid_heat_generator, HarvestTool.Wrench, DefaultDrop.Self, 2.0f, 10.0f, Rarity.COMMON, true, Facings.ALL, true, true);
        info(rt_heat_generator, HarvestTool.Wrench, DefaultDrop.Self, 2.0f, 10.0f, Rarity.COMMON, true, Facings.ALL, true, true);
        info(solid_heat_generator, HarvestTool.Wrench, DefaultDrop.Self, 2.0f, 10.0f, Rarity.COMMON, true, Facings.ALL, true, true);
        info(electric_kinetic_generator, HarvestTool.Wrench, DefaultDrop.Self, 2.0f, 10.0f, Rarity.COMMON, true, Facings.ALL, true, true);
        info(manual_kinetic_generator, HarvestTool.Pickaxe, DefaultDrop.Self, 5.0f, 10.0f, Rarity.COMMON, true, Facings.ALL, true, true);
        info(steam_kinetic_generator, HarvestTool.Wrench, DefaultDrop.Self, 2.0f, 10.0f, Rarity.COMMON, true, Facings.ALL, true, true);
        info(stirling_kinetic_generator, HarvestTool.Wrench, DefaultDrop.Self, 2.0f, 10.0f, Rarity.COMMON, true, Facings.ALL, true, true);
        info(water_kinetic_generator, HarvestTool.Wrench, DefaultDrop.Self, 2.0f, 10.0f, Rarity.COMMON, true, Facings.HORIZONTAL, true, true);
        info(wind_kinetic_generator, HarvestTool.Wrench, DefaultDrop.Self, 2.0f, 10.0f, Rarity.COMMON, true, Facings.HORIZONTAL, true, true);
        info(nuclear_reactor, HarvestTool.Wrench, DefaultDrop.Generator, 2.0f, 10.0f, Rarity.UNCOMMON, true, Facings.HORIZONTAL, true, true);
        info(reactor_chamber, HarvestTool.Wrench, DefaultDrop.Machine, 2.0f, 10.0f, Rarity.UNCOMMON, true, Facings.NONE, true, true);
        info(reactor_access_hatch, HarvestTool.Pickaxe, DefaultDrop.Self, 40.0f, 90.0f, Rarity.UNCOMMON, true, Facings.NONE, false, true);
        info(reactor_fluid_port, HarvestTool.Pickaxe, DefaultDrop.Self, 40.0f, 90.0f, Rarity.UNCOMMON, true, Facings.NONE, false, true);
        info(reactor_redstone_port, HarvestTool.Pickaxe, DefaultDrop.Self, 40.0f, 90.0f, Rarity.UNCOMMON, true, Facings.NONE, false, true);
        info(reactor_vessel, HarvestTool.Pickaxe, DefaultDrop.Self, 40.0f, 90.0f, Rarity.COMMON, true, Facings.NONE, false, true);
        info(energy_o_mat, HarvestTool.Pickaxe, DefaultDrop.Self, -1.0f, 3600000.0f, Rarity.COMMON, true, Facings.ALL, false, true);
        info(personal_chest, HarvestTool.Pickaxe, DefaultDrop.Self, -1.0f, 3600000.0f, Rarity.UNCOMMON, true, Facings.HORIZONTAL, false, true);
        info(trade_o_mat, HarvestTool.Pickaxe, DefaultDrop.Self, -1.0f, 3600000.0f, Rarity.COMMON, true, Facings.HORIZONTAL, false, true);
        info(crop, HarvestTool.Axe, DefaultDrop.Self, 0.8f, 0.2f, Rarity.COMMON, false, Facings.NONE, false, false);
        info(bronze_tank, HarvestTool.Wrench, DefaultDrop.Self, 3.0f, 15.0f, Rarity.COMMON, true, Facings.HORIZONTAL, false, true);
        info(iron_tank, HarvestTool.Wrench, DefaultDrop.Self, 3.0f, 15.0f, Rarity.COMMON, true, Facings.HORIZONTAL, false, true);
        info(steel_tank, HarvestTool.Wrench, DefaultDrop.Self, 4.0f, 20.0f, Rarity.COMMON, true, Facings.HORIZONTAL, false, true);
        info(iridium_tank, HarvestTool.Wrench, DefaultDrop.Self, 5.0f, 100.0f, Rarity.COMMON, true, Facings.HORIZONTAL, false, true);
        info(pump, HarvestTool.Wrench, DefaultDrop.Machine, 2.0f, 10.0f, Rarity.COMMON, true, Facings.ALL, true, true);
        info(miner, HarvestTool.Wrench, DefaultDrop.Self, 2.0f, 10.0f, Rarity.COMMON, true, Facings.HORIZONTAL, true, true);
        info(advanced_miner, HarvestTool.Wrench, DefaultDrop.Self, 2.0f, 10.0f, Rarity.COMMON, true, Facings.HORIZONTAL, true, true);
        info(crop_harvester, HarvestTool.Wrench, DefaultDrop.Machine, 2.0f, 10.0f, Rarity.COMMON, true, Facings.HORIZONTAL, true, true);
        info(cropmatron, HarvestTool.Wrench, DefaultDrop.Machine, 2.0f, 10.0f, Rarity.COMMON, true, Facings.HORIZONTAL, true, true);
        info(liquid_heat_exchanger, HarvestTool.Wrench, DefaultDrop.Machine, 2.0f, 10.0f, Rarity.COMMON, true, Facings.ALL, true, true);
        info(condenser, HarvestTool.Wrench, DefaultDrop.Machine, 2.0f, 10.0f, Rarity.COMMON, true, Facings.HORIZONTAL, true, true);
        info(teleporter, HarvestTool.Wrench, DefaultDrop.AdvMachine, 2.0f, 10.0f, Rarity.RARE, true, Facings.HORIZONTAL, true, true);
        info(magnetizer, HarvestTool.Wrench, DefaultDrop.Self, 2.0f, 10.0f, Rarity.COMMON, true, Facings.HORIZONTAL, true, true);
        info(tesla_coil, HarvestTool.Wrench, DefaultDrop.Machine, 2.0f, 10.0f, Rarity.COMMON, true, Facings.HORIZONTAL, true, true);
        info(sorting_machine, HarvestTool.Wrench, DefaultDrop.Machine, 2.0f, 10.0f, Rarity.COMMON, true, Facings.NONE, true, true);
        info(electrolyzer, HarvestTool.Wrench, DefaultDrop.Machine, 2.0f, 10.0f, Rarity.COMMON, true, Facings.HORIZONTAL, true, true);
        info(fermenter, HarvestTool.Wrench, DefaultDrop.Machine, 2.0f, 10.0f, Rarity.COMMON, true, Facings.ALL, true, true);
        info(coke_kiln, HarvestTool.Pickaxe, DefaultDrop.Self, 2.0f, 10.0f, Rarity.COMMON, true, Facings.HORIZONTAL, true, true);
        info(burning_box, HarvestTool.Pickaxe, DefaultDrop.Self, 1.5f, 10.0f, Rarity.COMMON, true, Facings.HORIZONTAL, true, true);
        info(steam_generator, HarvestTool.Wrench, DefaultDrop.Self, 2.0f, 10.0f, Rarity.COMMON, true, Facings.HORIZONTAL, true, true);
        info(tank, HarvestTool.Wrench, DefaultDrop.Self, 2.0f, 10.0f, Rarity.COMMON, true, Facings.HORIZONTAL, true, true);
        info(terraformer, HarvestTool.Wrench, DefaultDrop.AdvMachine, 2.0f, 10.0f, Rarity.UNCOMMON, true, Facings.HORIZONTAL, true, true);
        info(chunk_loader, HarvestTool.Wrench, DefaultDrop.Self, 2.0f, 10.0f, Rarity.UNCOMMON, true, Facings.DOWN_SIDE, true, true);
        info(iron_furnace, HarvestTool.Pickaxe, DefaultDrop.Self, 5.0f, 10.0f, Rarity.COMMON, true, Facings.HORIZONTAL, false, true);
        info(fluid_distributor, HarvestTool.Wrench, DefaultDrop.Machine, 2.0f, 10.0f, Rarity.COMMON, true, Facings.ALL, true, true);
        info(fluid_regulator, HarvestTool.Wrench, DefaultDrop.Machine, 2.0f, 10.0f, Rarity.COMMON, true, Facings.ALL, true, true);
        info(weighted_fluid_distributor, HarvestTool.Wrench, DefaultDrop.Machine, 2.0f, 10.0f, Rarity.COMMON, true, Facings.ALL, true, true);
        info(weighted_item_distributor, HarvestTool.Wrench, DefaultDrop.Machine, 2.0f, 10.0f, Rarity.COMMON, true, Facings.ALL, true, true);
        info(industrial_workbench, HarvestTool.Wrench, DefaultDrop.Self, 2.0f, 10.0f, Rarity.COMMON, true, Facings.HORIZONTAL, true, true);
        info(batch_crafter, HarvestTool.Wrench, DefaultDrop.AdvMachine, 2.0f, 10.0f, Rarity.UNCOMMON, true, Facings.HORIZONTAL, true, true);
        info(wooden_storage_box, HarvestTool.Axe, DefaultDrop.Self, 1.0f, 10.0f, Rarity.COMMON, false, Facings.NONE, false, true);
        info(iron_storage_box, HarvestTool.Wrench, DefaultDrop.Self, 1.0f, 15.0f, Rarity.COMMON, true, Facings.NONE, false, true);
        info(bronze_storage_box, HarvestTool.Wrench, DefaultDrop.Self, 1.0f, 15.0f, Rarity.COMMON, true, Facings.NONE, false, true);
        info(steel_storage_box, HarvestTool.Wrench, DefaultDrop.Self, 2.0f, 20.0f, Rarity.UNCOMMON, true, Facings.NONE, false, true);
        info(iridium_storage_box, HarvestTool.Wrench, DefaultDrop.Self, 3.0f, 100.0f, Rarity.EPIC, true, Facings.NONE, false, true);
        info(fluid_pipe, HarvestTool.Wrench, DefaultDrop.Self, 1.0f, 5.0f, Rarity.COMMON, true, Facings.ALL, false, false);
        info(coke_kiln_hatch, HarvestTool.Pickaxe, DefaultDrop.Self, 2.0f, 10.0f, Rarity.COMMON, true, Facings.ALL, true, true);
        info(coke_kiln_grate, HarvestTool.Pickaxe, DefaultDrop.Self, 2.0f, 10.0f, Rarity.COMMON, true, Facings.ALL, true, true);
        info(solar_distiller, HarvestTool.Wrench, DefaultDrop.Machine, 2.0f, 10.0f, Rarity.COMMON, true, Facings.HORIZONTAL, true, true);
        info(creative_generator, HarvestTool.None, DefaultDrop.None, -1.0f, 3600000.0f, Rarity.COMMON, false, Facings.NONE, false, true);
        info(rci_rsh, HarvestTool.Wrench, DefaultDrop.AdvMachine, 2.0f, 10.0f, Rarity.COMMON, true, Facings.ALL, true, true);
        info(rci_lzh, HarvestTool.Wrench, DefaultDrop.AdvMachine, 2.0f, 10.0f, Rarity.COMMON, true, Facings.ALL, true, true);
        info(steam_repressurizer, HarvestTool.Wrench, DefaultDrop.Machine, 2.0f, 10.0f, Rarity.COMMON, true, Facings.HORIZONTAL, true, true);
        info(barrel, HarvestTool.Axe, DefaultDrop.None, 2.0f, 6.0f, Rarity.COMMON, true, Facings.HORIZONTAL, false, false);
        info(wall, HarvestTool.Pickaxe, DefaultDrop.Self, 3.0f, 30.0f, Rarity.COMMON, true, Facings.NONE, false, false);
        info(itnt, HarvestTool.None, DefaultDrop.Self, 0.0f, 0.0f, Rarity.COMMON, false, Facings.HORIZONTAL, false, true);
        info(nuke, HarvestTool.None, DefaultDrop.Self, 0.0f, 0.0f, Rarity.UNCOMMON, false, Facings.HORIZONTAL, false, true);
        info(item_buffer, HarvestTool.Wrench, DefaultDrop.Machine, 2.0f, 10.0f, Rarity.COMMON, true, Facings.HORIZONTAL, true, true);
        info(item_buffer_2, HarvestTool.Wrench, DefaultDrop.Self, 2.0f, 10.0f, Rarity.COMMON, true, Facings.NONE, true, true);
        info(pattern_storage, HarvestTool.Wrench, DefaultDrop.AdvMachine, 2.0f, 10.0f, Rarity.COMMON, true, Facings.HORIZONTAL, true, true);
        info(replicator, HarvestTool.Wrench, DefaultDrop.AdvMachine, 2.0f, 10.0f, Rarity.UNCOMMON, true, Facings.HORIZONTAL, true, true);
        info(scanner, HarvestTool.Wrench, DefaultDrop.AdvMachine, 2.0f, 10.0f, Rarity.UNCOMMON, true, Facings.HORIZONTAL, true, true);
        info(trading_terminal, HarvestTool.Wrench, DefaultDrop.Machine, 2.0f, 10.0f, Rarity.UNCOMMON, true, Facings.HORIZONTAL, false, true);
        info(uu_assembly_bench, HarvestTool.Wrench, DefaultDrop.AdvMachine, 2.0f, 10.0f, Rarity.UNCOMMON, true, Facings.HORIZONTAL, true, true);
        info(geo_generator, HarvestTool.Wrench, DefaultDrop.Generator, 2.0f, 10.0f, Rarity.COMMON, true, Facings.HORIZONTAL, true, true);
        info(kinetic_generator, HarvestTool.Wrench, DefaultDrop.Generator, 2.0f, 10.0f, Rarity.COMMON, true, Facings.ALL, true, true);
        info(rt_generator, HarvestTool.Wrench, DefaultDrop.Generator, 2.0f, 10.0f, Rarity.COMMON, true, Facings.HORIZONTAL, true, true);
        info(semifluid_generator, HarvestTool.Wrench, DefaultDrop.Generator, 2.0f, 10.0f, Rarity.COMMON, true, Facings.HORIZONTAL, true, true);
        info(solar_generator, HarvestTool.Wrench, DefaultDrop.Generator, 2.0f, 10.0f, Rarity.COMMON, true, Facings.HORIZONTAL, true, true);
        info(stirling_generator, HarvestTool.Wrench, DefaultDrop.Generator, 2.0f, 10.0f, Rarity.COMMON, true, Facings.ALL, true, true);
        info(water_generator, HarvestTool.Wrench, DefaultDrop.Self, 2.0f, 10.0f, Rarity.COMMON, true, Facings.HORIZONTAL, true, true);
        info(wind_generator, HarvestTool.Wrench, DefaultDrop.Generator, 2.0f, 10.0f, Rarity.COMMON, true, Facings.HORIZONTAL, true, true);
        // END GENERATED
    }
    @SuppressWarnings("unchecked")
    public Class<? extends ic2.core.block.TileEntityBlock> getTeClass() {
        return switch (this) {
            case batbox -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.wiring.TileEntityElectricBatBox.class;
            case cesu -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.wiring.TileEntityElectricCESU.class;
            case mfe -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.wiring.TileEntityElectricMFE.class;
            case mfsu -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.wiring.TileEntityElectricMFSU.class;
            case cable -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.wiring.TileEntityCable.class;
            case detector_cable -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.wiring.TileEntityCableDetector.class;
            case splitter_cable -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.wiring.TileEntityCableSplitter.class;
            case lv_transformer -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.wiring.TileEntityTransformerLV.class;
            case mv_transformer -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.wiring.TileEntityTransformerMV.class;
            case hv_transformer -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.wiring.TileEntityTransformerHV.class;
            case ev_transformer -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.wiring.TileEntityTransformerEV.class;
            case macerator -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.machine.tileentity.TileEntityMacerator.class;
            case compressor -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.machine.tileentity.TileEntityCompressor.class;
            case extractor -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.machine.tileentity.TileEntityExtractor.class;
            case electric_furnace -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.machine.tileentity.TileEntityElectricFurnace.class;
            case recycler -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.machine.tileentity.TileEntityRecycler.class;
            case centrifuge -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.machine.tileentity.TileEntityCentrifuge.class;
            case blast_furnace -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.machine.tileentity.TileEntityBlastFurnace.class;
            case metal_former -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.machine.tileentity.TileEntityMetalFormer.class;
            case ore_washing_plant -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.machine.tileentity.TileEntityOreWashing.class;
            case block_cutter -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.machine.tileentity.TileEntityBlockCutter.class;
            case canner -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.machine.tileentity.TileEntityCanner.class;
            case solid_canner -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.machine.tileentity.TileEntitySolidCanner.class;
            case fluid_bottler -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.machine.tileentity.TileEntityFluidBottler.class;
            case induction_furnace -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.machine.tileentity.TileEntityInduction.class;
            case mass_fabricator -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.machine.tileentity.TileEntityMassFabricator.class;
            case matter_generator -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.machine.tileentity.TileEntityMatter.class;
            case generator -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.generator.tileentity.TileEntityGenerator.class;
            case solar -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.generator.tileentity.TileEntitySolarGenerator.class;
            case water -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.generator.tileentity.TileEntityWaterGenerator.class;
            case wind -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.generator.tileentity.TileEntityWindGenerator.class;
            case geothermalgenerator -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.generator.tileentity.TileEntityGeoGenerator.class;
            case semfluid -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.generator.tileentity.TileEntitySemifluidGenerator.class;
            case rtgenerator -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.generator.tileentity.TileEntityRTGenerator.class;
            case stirling -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.generator.tileentity.TileEntityStirlingGenerator.class;
            case kinetic -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.generator.tileentity.TileEntityKineticGenerator.class;
            case electric_heat_generator -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.heatgenerator.tileentity.TileEntityElectricHeatGenerator.class;
            case fluid_heat_generator -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.heatgenerator.tileentity.TileEntityFluidHeatGenerator.class;
            case rt_heat_generator -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.heatgenerator.tileentity.TileEntityRTHeatGenerator.class;
            case solid_heat_generator -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.heatgenerator.tileentity.TileEntitySolidHeatGenerator.class;
            case electric_kinetic_generator -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.kineticgenerator.tileentity.TileEntityElectricKineticGenerator.class;
            case manual_kinetic_generator -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.kineticgenerator.tileentity.TileEntityManualKineticGenerator.class;
            case steam_kinetic_generator -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.kineticgenerator.tileentity.TileEntitySteamKineticGenerator.class;
            case stirling_kinetic_generator -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.kineticgenerator.tileentity.TileEntityStirlingKineticGenerator.class;
            case water_kinetic_generator -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.kineticgenerator.tileentity.TileEntityWaterKineticGenerator.class;
            case wind_kinetic_generator -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.kineticgenerator.tileentity.TileEntityWindKineticGenerator.class;
            case chargepad_batbox -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.wiring.TileEntityChargepadBatBox.class;
            case chargepad_cesu -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.wiring.TileEntityChargepadCESU.class;
            case chargepad_mfe -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.wiring.TileEntityChargepadMFE.class;
            case chargepad_mfsu -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.wiring.TileEntityChargepadMFSU.class;
            case luminator_flat -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.wiring.TileEntityLuminator.class;
            case nuclear_reactor -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.reactor.tileentity.TileEntityNuclearReactorElectric.class;
            case reactor_chamber -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.reactor.tileentity.TileEntityReactorChamberElectric.class;
            case reactor_access_hatch -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.reactor.tileentity.TileEntityReactorAccessHatch.class;
            case reactor_fluid_port -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.reactor.tileentity.TileEntityReactorFluidPort.class;
            case reactor_redstone_port -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.reactor.tileentity.TileEntityReactorRedstonePort.class;
            case reactor_vessel -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.reactor.tileentity.TileEntityReactorVessel.class;
            case energy_o_mat -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.personal.TileEntityEnergyOMat.class;
            case personal_chest -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.personal.TileEntityPersonalChest.class;
            case trade_o_mat -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.personal.TileEntityTradeOMat.class;
            case crop -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.crop.TileEntityCrop.class;
            case bronze_tank -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.storage.tank.TileEntityBronzeTank.class;
            case iron_tank -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.storage.tank.TileEntityIronTank.class;
            case steel_tank -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.storage.tank.TileEntitySteelTank.class;
            case iridium_tank -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.storage.tank.TileEntityIridiumTank.class;
            case pump -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.machine.tileentity.TileEntityPump.class;
            case miner -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.machine.tileentity.TileEntityMiner.class;
            case advanced_miner -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.machine.tileentity.TileEntityAdvMiner.class;
            case crop_harvester -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.machine.tileentity.TileEntityCropHarvester.class;
            case cropmatron -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.machine.tileentity.TileEntityCropmatron.class;
            case liquid_heat_exchanger -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.machine.tileentity.TileEntityLiquidHeatExchanger.class;
            case condenser -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.machine.tileentity.TileEntityCondenser.class;
            case teleporter -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.machine.tileentity.TileEntityTeleporter.class;
            case magnetizer -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.machine.tileentity.TileEntityMagnetizer.class;
            case tesla_coil -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.machine.tileentity.TileEntityTesla.class;
            case sorting_machine -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.machine.tileentity.TileEntitySortingMachine.class;
            case electrolyzer -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.machine.tileentity.TileEntityElectrolyzer.class;
            case fermenter -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.machine.tileentity.TileEntityFermenter.class;
            case coke_kiln -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.steam.TileEntityCokeKiln.class;
            case burning_box -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.steam.TileEntityBurningBox.class;
            case steam_generator -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.steam.TileEntitySteamGenerator.class;
            case tank -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.machine.tileentity.TileEntityTank.class;
            case terraformer -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.machine.tileentity.TileEntityTerraformer.class;
            case chunk_loader -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.machine.tileentity.TileEntityChunkloader.class;
            case iron_furnace -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.machine.tileentity.TileEntityIronFurnace.class;
            case fluid_distributor -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.machine.tileentity.TileEntityFluidDistributor.class;
            case fluid_regulator -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.machine.tileentity.TileEntityFluidRegulator.class;
            case weighted_fluid_distributor -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.machine.tileentity.TileEntityWeightedFluidDistributor.class;
            case weighted_item_distributor -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.machine.tileentity.TileEntityWeightedItemDistributor.class;
            case industrial_workbench -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.machine.tileentity.TileEntityIndustrialWorkbench.class;
            case batch_crafter -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.machine.tileentity.TileEntityBatchCrafter.class;
            case wooden_storage_box -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.storage.box.TileEntityWoodenStorageBox.class;
            case iron_storage_box -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.storage.box.TileEntityIronStorageBox.class;
            case bronze_storage_box -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.storage.box.TileEntityBronzeStorageBox.class;
            case steel_storage_box -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.storage.box.TileEntitySteelStorageBox.class;
            case iridium_storage_box -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.storage.box.TileEntityIridiumStorageBox.class;
            case fluid_pipe -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.transport.TileEntityFluidPipe.class;
            case coke_kiln_hatch -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.steam.TileEntityCokeKilnHatch.class;
            case coke_kiln_grate -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.steam.TileEntityCokeKilnGrate.class;
            case solar_distiller -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.machine.tileentity.TileEntitySolarDistiller.class;
            case creative_generator -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.machine.tileentity.TileEntityCreativeGenerator.class;
            case rci_rsh -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.reactor.tileentity.TileEntityRCI_RSH.class;
            case rci_lzh -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.reactor.tileentity.TileEntityRCI_LZH.class;
            case steam_repressurizer -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.machine.tileentity.TileEntitySteamRepressurizer.class;
            case barrel -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.TileEntityBarrel.class;
            case wall -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.TileEntityWall.class;
            case itnt -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.machine.tileentity.TileEntityITnt.class;
            case nuke -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.machine.tileentity.TileEntityNuke.class;
            case item_buffer -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.machine.tileentity.TileEntityItemBuffer.class;
            case item_buffer_2 -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.machine.tileentity.TileEntityBetterItemBuffer.class;
            case pattern_storage -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.machine.tileentity.TileEntityPatternStorage.class;
            case replicator -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.machine.tileentity.TileEntityReplicator.class;
            case scanner -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.machine.tileentity.TileEntityScanner.class;
            case trading_terminal -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.personal.TileEntityTradingTerminal.class;
            case uu_assembly_bench -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.machine.tileentity.TileEntityAssemblyBench.class;
            case geo_generator -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.generator.tileentity.TileEntityGeoGenerator.class;
            case kinetic_generator -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.generator.tileentity.TileEntityKineticGenerator.class;
            case rt_generator -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.generator.tileentity.TileEntityRTGenerator.class;
            case semifluid_generator -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.generator.tileentity.TileEntitySemifluidGenerator.class;
            case solar_generator -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.generator.tileentity.TileEntitySolarGenerator.class;
            case stirling_generator -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.generator.tileentity.TileEntityStirlingGenerator.class;
            case water_generator -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.generator.tileentity.TileEntityWaterGenerator.class;
            case wind_generator -> (Class<? extends ic2.core.block.TileEntityBlock>) (Class<?>) ic2.core.block.generator.tileentity.TileEntityWindGenerator.class;
            default -> null;
        };
    }
    public ic2.core.block.TileEntityBlock getDummyTe() { return null; }
}
