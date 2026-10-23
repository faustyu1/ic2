package ic2.core.item;

import ic2.api.item.ElectricItem;
import ic2.api.item.IElectricItem;
import ic2.api.item.IElectricItemManager;
import ic2.api.item.ISpecialElectricItem;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;

/**
 * Fabric port of {@code ic2.core.item.GatewayElectricItemManager}: {@link ElectricItem#manager}.
 * Routes every call to the manager responsible for the stack - the item's own for an
 * {@link ISpecialElectricItem}, {@link ElectricItemManager} for an {@link IElectricItem}, and a
 * registered backup manager (such as the attached jetpack's) for anything else.
 */
public class GatewayElectricItemManager implements IElectricItemManager {

    @Override
    public double charge(ItemStack stack, double amount, int tier, boolean ignoreTransferLimit, boolean simulate) {
        IElectricItemManager manager = getManager(stack);
        return manager == null ? 0.0 : manager.charge(stack, amount, tier, ignoreTransferLimit, simulate);
    }

    @Override
    public double discharge(ItemStack stack, double amount, int tier, boolean ignoreTransferLimit, boolean externally, boolean simulate) {
        IElectricItemManager manager = getManager(stack);
        return manager == null ? 0.0 : manager.discharge(stack, amount, tier, ignoreTransferLimit, externally, simulate);
    }

    @Override
    public double getCharge(ItemStack stack) {
        IElectricItemManager manager = getManager(stack);
        return manager == null ? 0.0 : manager.getCharge(stack);
    }

    @Override
    public double getMaxCharge(ItemStack stack) {
        IElectricItemManager manager = getManager(stack);
        return manager == null ? 0.0 : manager.getMaxCharge(stack);
    }

    @Override
    public boolean canUse(ItemStack stack, double amount) {
        IElectricItemManager manager = getManager(stack);
        return manager != null && manager.canUse(stack, amount);
    }

    @Override
    public boolean use(ItemStack stack, double amount, LivingEntity entity) {
        IElectricItemManager manager = getManager(stack);
        return manager != null && manager.use(stack, amount, entity);
    }

    @Override
    public void chargeFromArmor(ItemStack stack, LivingEntity entity) {
        IElectricItemManager manager = getManager(stack);
        if (manager != null && entity != null) {
            manager.chargeFromArmor(stack, entity);
        }
    }

    @Override
    public String getToolTip(ItemStack stack) {
        IElectricItemManager manager = getManager(stack);
        return manager == null ? null : manager.getToolTip(stack);
    }

    @Override
    public int getTier(ItemStack stack) {
        IElectricItemManager manager = getManager(stack);
        return manager == null ? 0 : manager.getTier(stack);
    }

    private static IElectricItemManager getManager(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        if (stack.getItem() instanceof ISpecialElectricItem special) {
            return special.getManager(stack);
        }
        if (stack.getItem() instanceof IElectricItem) {
            return ElectricItemManager.INSTANCE;
        }
        return ElectricItem.getBackupManager(stack);
    }
}
