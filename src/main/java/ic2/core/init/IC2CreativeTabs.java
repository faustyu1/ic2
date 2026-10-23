package ic2.core.init;

import ic2.api.item.ElectricItem;
import ic2.api.item.IElectricItem;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.block.Block;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * The creative tabs. 1.12.2 had a single IC2 tab; the port splits it in three: tools and armour,
 * building blocks, and everything else - machines, cables, components and resources.
 */
public final class IC2CreativeTabs {
    /** Machines, generators, cables, components and resources. */
    public static ItemGroup IC2;
    public static ItemGroup TOOLS;
    public static ItemGroup BUILDING;

    private static Set<Block> buildingBlocks;

    private IC2CreativeTabs(){}

    public static void register(){
        // Fabric orders the mods' tabs by id, so the ids carry the order: main, tools, building
        IC2 = register("tab_1_main", "itemGroup.ic2", () -> new ItemStack(IC2Blocks.MACERATOR_BLOCK),
                item -> !isTool(item) && !isBuildingBlock(item));
        TOOLS = register("tab_2_tools", "itemGroup.ic2.tools", () -> new ItemStack(IC2Items.WRENCH),
                IC2CreativeTabs::isTool);
        BUILDING = register("tab_3_building", "itemGroup.ic2.building", () -> new ItemStack(IC2ClassicBlocks.REINFORCED_STONE),
                IC2CreativeTabs::isBuildingBlock);
    }

    private static ItemGroup register(String id, String name, java.util.function.Supplier<ItemStack> icon, Predicate<Item> filter) {
        ItemGroup group = FabricItemGroup.builder()
            .icon(icon)
            .displayName(Text.translatable(name))
            .entries((ctx, entries) -> {
                // the ic2 items in registration order, the ones this tab takes
                for (var item : Registries.ITEM) {
                    Identifier itemId = Registries.ITEM.getId(item);
                    if (itemId == null || !"ic2".equals(itemId.getNamespace()) || !filter.test(item)) {
                        continue;
                    }
                    // a bare seed bag holds nothing; the tab lists one bag per crop instead
                    if (item instanceof ic2.core.item.ItemCropSeed) {
                        ic2.core.item.ItemCropSeed.creativeStacks().forEach(entries::add);
                        continue;
                    }
                    entries.add(item);
                    // 1.12.2 listed every electric item twice, empty and full; without the
                    // full one there is no way to get a charged tool in creative
                    ItemStack charged = chargedCopy(item);
                    if (charged != null) {
                        entries.add(charged);
                    }
                    // the same for the items with a tank of their own, empty and full
                    if (item instanceof ic2.core.item.IItemFluidTank tank && tank.getCreativeFluid() != null) {
                        entries.add(tank.filledCopy(new ItemStack(item), tank.getCreativeFluid()));
                    }
                }
            })
            .build();
        return Registry.register(Registries.ITEM_GROUP, Identifier.of("ic2", id), group);
    }

    /** Tools, weapons and armour: what the player holds or wears to work, not what they build. */
    private static boolean isTool(Item item) {
        if (item instanceof ToolItem || item instanceof ArmorItem
                || item instanceof ic2.core.item.ItemWrench
                || item instanceof ic2.core.item.ItemTreetap
                || item instanceof ic2.core.item.handheld.ItemHandHeldContainer) {
            return true;
        }
        String pkg = item.getClass().getPackageName();
        return pkg.equals("ic2.core.item.tool") || pkg.equals("ic2.core.item.armor")
                || pkg.startsWith("ic2.core.item.armor.");
    }

    /** Ores, rubber trees, metal blocks and the construction blocks: walls, glass, doors. */
    private static boolean isBuildingBlock(Item item) {
        if (!(item instanceof BlockItem blockItem)) {
            return false;
        }
        if (buildingBlocks == null) {
            Set<Block> set = new HashSet<>();
            for (Block block : new Block[] {
                    IC2ClassicBlocks.TIN_ORE, IC2ClassicBlocks.URANIUM_ORE, IC2ClassicBlocks.LEAD_ORE,
                    IC2ClassicBlocks.RUBBER_WOOD, IC2ClassicBlocks.RUBBER_LEAVES, IC2ClassicBlocks.RUBBER_SAPLING,
                    IC2ClassicBlocks.TIN_BLOCK, IC2ClassicBlocks.URANIUM_BLOCK, IC2ClassicBlocks.LEAD_BLOCK,
                    IC2ClassicBlocks.BRONZE_BLOCK, IC2ClassicBlocks.STEEL_BLOCK, IC2ClassicBlocks.SILVER_BLOCK,
                    IC2ClassicBlocks.REINFORCED_STONE, IC2ClassicBlocks.REINFORCED_GLASS, IC2ClassicBlocks.REINFORCED_DOOR,
                    IC2ClassicBlocks.IRON_FENCE, IC2ClassicBlocks.FENCE_GATE,
                    IC2ClassicBlocks.REFRACTORY_BRICKS, IC2ClassicBlocks.FOAM}) {
                if (block != null) {
                    set.add(block);
                }
            }
            set.addAll(IC2ClassicBlocks.WALLS);
            buildingBlocks = set;
        }
        return buildingBlocks.contains(blockItem.getBlock());
    }

    /** A full stack of {@code item}, or null when the item does not hold EU. */
    private static ItemStack chargedCopy(Item item) {
        if (!(item instanceof IElectricItem electric) || ElectricItem.manager == null) {
            return null;
        }

        ItemStack stack = new ItemStack(item);
        double maxCharge = electric.getMaxCharge(stack);
        if (maxCharge <= 0.0) {
            return null;
        }

        ElectricItem.manager.charge(stack, maxCharge, Integer.MAX_VALUE, true, false);
        return stack;
    }
}
