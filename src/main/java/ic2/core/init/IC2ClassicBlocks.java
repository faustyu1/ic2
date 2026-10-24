package ic2.core.init;

import ic2.core.block.BlockFoam;
import ic2.core.block.BlockWall;
import ic2.core.util.Ic2Color;
import ic2.core.block.BlockIC2Door;
import ic2.core.block.BlockIC2Fluid;
import ic2.core.block.BlockMoltenMetal;
import ic2.core.block.BlockOre;
import ic2.core.block.BlockRubWood;
import ic2.core.block.ResourceBlock;
import ic2.core.block.Ic2Leaves;
import ic2.core.block.Ic2Sapling;
import ic2.core.ref.FluidName;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockSetType;
import net.minecraft.block.Blocks;
import net.minecraft.block.MapColor;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

/**
 * Registry for classic non-Te IC2 blocks (ores, rubber wood, foam).
 * Mirrors original ic2.core.block.BlockMultiID registrations but as plain Fabric Blocks.
 */
public final class IC2ClassicBlocks {

    public static Block TIN_ORE;
    public static Block URANIUM_ORE;
    public static Block LEAD_ORE;
    public static Block RUBBER_WOOD;
    public static Block FOAM;
    /** The construction foam walls, in the order of {@link Ic2Color}. */
    public static final java.util.List<Block> WALLS = new java.util.ArrayList<>();
    /** A placed stick of dynamite; it has no item of its own, the dynamite item places it. */
    public static Block DYNAMITE;

    // Extra parity blocks from ResourceBlock not strictly required but cheap to add
    public static Block TIN_BLOCK;
    public static Block URANIUM_BLOCK;
    public static Block LEAD_BLOCK;
    public static Block BRONZE_BLOCK;
    public static Block STEEL_BLOCK;
    public static Block MACHINE_BLOCK;
    public static Block ADVANCED_MACHINE_BLOCK;
    public static Block REINFORCED_STONE;
    public static Block SILVER_BLOCK;
    public static Block MOLTEN_COPPER;
    public static Block MOLTEN_TIN;
    public static Block MOLTEN_BRONZE;
    public static Block MOLTEN_GOLD;
    public static Block MOLTEN_IRON;
    public static Block MOLTEN_LEAD;
    public static Block MOLTEN_STEEL;
    public static Block REINFORCED_GLASS;
    public static Block IRON_FENCE;
    public static Block FENCE_GATE;
    public static Block REINFORCED_DOOR;
    public static Block REFRACTORY_BRICKS;
    public static ic2.core.block.machine.BlockMiningPipe MINING_PIPE;
    public static Block RUBBER_LEAVES;
    public static Block RUBBER_SAPLING;

    public static Block HOT_WATER;
    public static Block HOT_COOLANT;
    public static Block COOLANT;
    public static Block UU_FLUID;
    public static Block CONSTRUCTION_FOAM_FLUID;

    private IC2ClassicBlocks() {}

    public static void registerAll() {
        // copper ore and the copper block are Minecraft's own since 1.17 (see IC2ItemVariants.VANILLA)
        TIN_ORE = registerOre("tin_ore", BlockOre.OreType.TIN);
        URANIUM_ORE = registerOre("uranium_ore", BlockOre.OreType.URANIUM);
        LEAD_ORE = registerOre("lead_ore", BlockOre.OreType.LEAD);

        // ticksRandomly: a tapped log fills its resin spot again over time
        RUBBER_WOOD = register("rubber_wood", new BlockRubWood(BlockRubWood.createSettings()));

        // soft foam lets light through, which is what makes it set faster in a lit room
        FOAM = register("foam",
                new BlockFoam(FabricBlockSettings.create()
                        .mapColor(MapColor.WHITE)
                        .sounds(BlockSoundGroup.SLIME)
                        .strength(0.01F, 10.0F)
                        .nonOpaque()
                        .allowsSpawning((state, world, pos, type) -> false)));
        // what the foam sets into, one block per colour since the flattening
        for (Ic2Color color : Ic2Color.values) {
            WALLS.add(register("wall_" + color.getName(), new BlockWall(FabricBlockSettings.create()
                    .mapColor(color.mcColor)
                    .sounds(BlockSoundGroup.STONE)
                    .strength(3.0F, 30.0F)
                    .requiresTool(), color)));
        }

        DYNAMITE = Registry.register(Registries.BLOCK, Identifier.of("ic2", "dynamite"), new ic2.core.block.BlockDynamite());

        // Optional metal/storage blocks (parity with ResourceBlock metal variants)
        // Existing 5 use legacy registerMetal for bw-compat; new/updated use ResourceBlock (Settings.copy(Blocks.IRON_BLOCK))
        TIN_BLOCK = registerMetal("tin_block", MapColor.LIGHT_GRAY, 3.0F, 6.0F);
        URANIUM_BLOCK = registerMetal("uranium_block", MapColor.GREEN, 5.0F, 10.0F);
        LEAD_BLOCK = registerMetal("lead_block", MapColor.GRAY, 3.0F, 6.0F);
        BRONZE_BLOCK = registerMetal("bronze_block", MapColor.ORANGE, 5.0F, 10.0F);
        STEEL_BLOCK = register("steel_block", new ResourceBlock(ResourceBlock.Metal.STEEL));
        // the casing blocks every machine is built from; hardness values are the 1.12.2 ones
        MACHINE_BLOCK = registerMetal("machine", MapColor.IRON_GRAY, 5.0F, 10.0F);
        ADVANCED_MACHINE_BLOCK = registerMetal("advanced_machine", MapColor.IRON_GRAY, 8.0F, 10.0F);
        REINFORCED_STONE = register("reinforced_stone", new Block(FabricBlockSettings.create()
                .mapColor(MapColor.STONE_GRAY)
                .sounds(BlockSoundGroup.STONE)
                .strength(80.0F, 180.0F)
                .requiresTool()));
        SILVER_BLOCK = register("silver_block", new ResourceBlock(ResourceBlock.Metal.SILVER));
        MOLTEN_COPPER = register("molten_copper", new BlockMoltenMetal("copper"));
        MOLTEN_TIN = register("molten_tin", new BlockMoltenMetal("tin"));
        MOLTEN_BRONZE = register("molten_bronze", new BlockMoltenMetal("bronze"));
        MOLTEN_GOLD = register("molten_gold", new BlockMoltenMetal("gold"));
        MOLTEN_IRON = register("molten_iron", new BlockMoltenMetal("iron"));
        MOLTEN_LEAD = register("molten_lead", new BlockMoltenMetal("lead"));
        MOLTEN_STEEL = register("molten_steel", new BlockMoltenMetal("steel"));
        REINFORCED_GLASS = register("reinforced_glass", new ic2.core.block.BlockTexGlass());
        IRON_FENCE = register("iron_fence", new ic2.core.block.BlockIC2Fence());
        FENCE_GATE = register("iron_fence_gate", new ic2.core.block.BlockIC2FenceGate());

        REINFORCED_DOOR = register("reinforced_door", new BlockIC2Door(BlockSetType.IRON,
                AbstractBlock.Settings.copy(Blocks.IRON_DOOR).mapColor(MapColor.STONE_GRAY).strength(50.0F, 150.0F).requiresTool()));
        // the coke kiln and the blast furnace are built out of these
        REFRACTORY_BRICKS = register("refractory_bricks", new Block(FabricBlockSettings.create()
                .mapColor(MapColor.ORANGE)
                .sounds(BlockSoundGroup.STONE)
                .strength(2.0F, 10.0F)
                .requiresTool()));
        // laid by the miner only, so there is no block item: the plain mining_pipe item stands for it
        MINING_PIPE = Registry.register(Registries.BLOCK, Identifier.of("ic2", "mining_pipe"),
                new ic2.core.block.machine.BlockMiningPipe(FabricBlockSettings.create()
                        .mapColor(MapColor.IRON_GRAY)
                        .sounds(BlockSoundGroup.METAL)
                        .strength(6.0F, 10.0F)
                        .nonOpaque()
                        .requiresTool()));
        RUBBER_LEAVES = register("rubber_leaves", new Ic2Leaves(AbstractBlock.Settings.copy(Blocks.OAK_LEAVES)));
        RUBBER_SAPLING = register("rubber_sapling", new Ic2Sapling(AbstractBlock.Settings.copy(Blocks.OAK_SAPLING)));

        HOT_WATER = register("hot_water", new BlockIC2Fluid(FluidName.hot_water.getInstance(), AbstractBlock.Settings.copy(Blocks.WATER)));
        HOT_COOLANT = register("hot_coolant", new BlockIC2Fluid(FluidName.hot_coolant.getInstance(), AbstractBlock.Settings.copy(Blocks.WATER)));
        COOLANT = register("coolant", new BlockIC2Fluid(FluidName.coolant.getInstance(), AbstractBlock.Settings.copy(Blocks.WATER)));
        UU_FLUID = register("uu_matter", new BlockIC2Fluid(FluidName.uu_matter.getInstance(), AbstractBlock.Settings.copy(Blocks.WATER)));
        CONSTRUCTION_FOAM_FLUID = register("construction_foam", new BlockIC2Fluid(FluidName.construction_foam.getInstance(), AbstractBlock.Settings.copy(Blocks.WATER)));
    }

    private static Block registerOre(String name, BlockOre.OreType type) {
        FabricBlockSettings settings = FabricBlockSettings.create()
                .mapColor(MapColor.STONE_GRAY)
                .sounds(BlockSoundGroup.STONE)
                .strength(type.hardness, type.resistance)
                .requiresTool();
        return register(name, new BlockOre(settings, type));
    }

    private static Block registerMetal(String name, MapColor color, float hardness, float resistance) {
        FabricBlockSettings settings = FabricBlockSettings.create()
                .mapColor(color)
                .sounds(BlockSoundGroup.METAL)
                .strength(hardness, resistance)
                .requiresTool();
        return register(name, new Block(settings));
    }

    private static Block register(String name, Block block) {
        Identifier id = Identifier.of("ic2", name);
        Registry.register(Registries.BLOCK, id, block);
        Registry.register(Registries.ITEM, id, new BlockItem(block, new Item.Settings()));
        return block;
    }
}
