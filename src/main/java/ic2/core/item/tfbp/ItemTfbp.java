package ic2.core.item.tfbp;

import ic2.api.item.ITerraformingBP;
import ic2.core.item.ItemIC2;
import ic2.core.item.type.TfbpType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Fabric port of Tfbp - a terraforming blueprint, one item per kind. Put into a terraformer it
 * decides what the terraformer does, how far it reaches and what each change costs (see
 * {@link TfbpType}). The blank blueprint does nothing; it is what the others are crafted from.
 */
public class ItemTfbp extends ItemIC2 implements ITerraformingBP {
    private final TfbpType type;
    private final TerraformerBase logic;

    public ItemTfbp(String name, TfbpType type) {
        super(name, new Settings().maxCount(1));
        this.type = type;
        this.logic = switch (type) {
            case blank -> null;
            case chilling -> new Chilling();
            case cultivation -> new Cultivation();
            case desertification -> new Desertification();
            case flatification -> new Flatification();
            case irrigation -> new Irrigation();
            case mushroom -> new Mushroom();
        };
    }

    public TfbpType getType() {
        return type;
    }

    @Override
    public double getConsume(ItemStack stack) {
        return type.consume;
    }

    @Override
    public int getRange(ItemStack stack) {
        return type.range;
    }

    @Override
    public boolean canInsert(ItemStack stack, PlayerEntity player, World world, BlockPos pos) {
        return true;
    }

    @Override
    public boolean terraform(ItemStack stack, World world, BlockPos pos) {
        return logic != null && logic.terraform(world, pos);
    }
}
