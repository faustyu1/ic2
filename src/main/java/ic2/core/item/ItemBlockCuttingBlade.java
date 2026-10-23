package ic2.core.item;

import ic2.api.item.IBlockCuttingBlade;
import ic2.core.item.type.BlockCuttingBladeType;
import java.util.List;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;

/**
 * Fabric port of BlockCuttingBlade: the blades of the block cutter, cutting materials up to a
 * hardness of 3 (iron), 6 (steel) or 9 (diamond).
 */
public class ItemBlockCuttingBlade extends ItemIC2 implements IBlockCuttingBlade {
    private final BlockCuttingBladeType type;

    public ItemBlockCuttingBlade(String name, BlockCuttingBladeType type) {
        super(name);
        this.type = type;
    }

    @Override
    public int getHardness(ItemStack stack) {
        return switch (type) {
            case iron -> 3;
            case steel -> 6;
            case diamond -> 9;
        };
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType tooltipType) {
        tooltip.add(Text.translatable(switch (type) {
            case iron -> "IronBlockCuttingBlade.info";
            case steel -> "AdvIronBlockCuttingBlade.info";
            case diamond -> "DiamondBlockCuttingBlade.info";
        }));
        tooltip.add(Text.translatable("CuttingBlade.hardness", getHardness(stack)));
    }
}
