package ic2.core.item.tool;

import java.util.List;

import ic2.api.item.ElectricItem;
import ic2.api.item.IMiningDrill;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Fabric port of {@code ic2.core.item.tool.ItemDrill}: pickaxe and shovel in one, running on EU.
 *
 * <p>The three drills differ only in their numbers, which are the 1.12.2 ones: the drill mines at
 * 8 for 50 EU a block, the diamond drill at 16 for 80, and the iridium drill at 24 for 800.
 *
 * <p>The iridium drill's silk touch toggle is not here: in 1.12.2 it swapped the enchantments on
 * the stack, and 1.21.1 keeps enchantments in a registry-backed component that cannot be built
 * without a world. The 1.12.2 speed bonus for drilling underwater or in mid-air is also gone - it
 * needed to find the player holding the stack, which the mining-speed hook no longer gets.
 */
public class ItemDrill extends ItemElectricTool implements IMiningDrill {

    /** EU the mining machine is charged per block, {@code energyUse} in 1.12.2. */
    private final int machineEnergyUse;
    /** Ticks the mining machine takes per block, {@code breakTime} in 1.12.2. */
    private final int machineBreakTime;

    public ItemDrill(String name, double operationEnergyCost, double maxCharge, double transferLimit, int tier,
                     float efficiency, TagKey<Block> incorrectForDrops, int machineEnergyUse, int machineBreakTime) {
        super(name, operationEnergyCost, maxCharge, transferLimit, tier, efficiency, incorrectForDrops,
                List.of(BlockTags.PICKAXE_MINEABLE, BlockTags.SHOVEL_MINEABLE));
        this.machineEnergyUse = machineEnergyUse;
        this.machineBreakTime = machineBreakTime;
    }

    /** The plain drill: iron's reach, 8x speed, 50 EU a block. */
    public static ItemDrill drill() {
        return new ItemDrill("drill", 50, 30000, 100, 1, 8.0f, BlockTags.INCORRECT_FOR_IRON_TOOL, 6, 200);
    }

    /** The diamond drill: diamond's reach, 16x speed, 80 EU a block. */
    public static ItemDrill diamondDrill() {
        return new ItemDrill("diamond_drill", 80, 30000, 100, 1, 16.0f, BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 20, 50);
    }

    /** The iridium drill: mines everything, 24x speed, 800 EU a block. */
    public static ItemDrill iridiumDrill() {
        return new ItemDrill("iridium_drill", 800, 300000, 1000, 3, 24.0f, BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 200, 20);
    }

    // ---- IMiningDrill: what the mining machine asks when it holds one ----

    @Override
    public int energyUse(ItemStack stack, World world, BlockPos pos, BlockState state) {
        return machineEnergyUse;
    }

    @Override
    public int breakTime(ItemStack stack, World world, BlockPos pos, BlockState state) {
        return machineBreakTime;
    }

    @Override
    public boolean breakBlock(ItemStack stack, World world, BlockPos pos, BlockState state) {
        return ElectricItem.manager.use(stack, operationEnergyCost, null);
    }

}
