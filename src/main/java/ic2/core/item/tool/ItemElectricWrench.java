package ic2.core.item.tool;

import java.util.LinkedList;
import java.util.List;

import ic2.api.item.ElectricItem;
import ic2.api.item.IElectricItem;
import ic2.api.item.IItemHudInfo;
import ic2.core.item.ItemWrench;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

/**
 * Fabric port of {@code ic2.core.item.tool.ItemToolWrenchElectric}: the same wrench, paying 100 EU
 * for what the hand wrench pays a point of wear.
 */
public class ItemElectricWrench extends ItemWrench implements IElectricItem, IItemHudInfo {
    /** EU one point of the hand wrench's wear is worth. */
    private static final double EU_PER_DAMAGE = 100.0;

    public ItemElectricWrench() {
        super("electric_wrench", new Settings().maxCount(1).maxDamage(27));
    }

    @Override
    public boolean canTakeDamage(ItemStack stack, int amount) {
        return ElectricItem.manager.getCharge(stack) >= amount * EU_PER_DAMAGE;
    }

    @Override
    public void damage(ItemStack stack, int amount, PlayerEntity player) {
        ElectricItem.manager.use(stack, amount * EU_PER_DAMAGE, player);
    }

    @Override
    public boolean canProvideEnergy(ItemStack stack) {
        return false;
    }

    @Override
    public double getMaxCharge(ItemStack stack) {
        return 12000.0;
    }

    @Override
    public int getTier(ItemStack stack) {
        return 1;
    }

    @Override
    public double getTransferLimit(ItemStack stack) {
        return 250.0;
    }

    @Override
    public List<String> getHudInfo(ItemStack stack, boolean advanced) {
        List<String> info = new LinkedList<>();
        info.add(ElectricItem.manager.getToolTip(stack));
        return info;
    }
}
