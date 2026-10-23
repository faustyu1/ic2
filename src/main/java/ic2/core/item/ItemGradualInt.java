package ic2.core.item;

import ic2.api.item.ICustomDamageItem;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

/**
 * Fabric port of ItemGradualInt - an item whose wear is counted in its own NBT rather than in the
 * vanilla damage value.
 *
 * <p>IC2 needs this because the numbers are far larger than a vanilla durability (a heat storage
 * holds 60000) and because the items still stack while undamaged. The vanilla durability bar is
 * driven from the same number, which is what the original did through getDurabilityForDisplay.
 */
public class ItemGradualInt extends ItemIC2 implements ICustomDamageItem {
    /** Key the 1.12.2 items used, kept so the number reads the same. */
    private static final String NBT_KEY = "advDmg";

    private final int maxDamage;

    public ItemGradualInt(String name, int maxDamage) {
        this(name, maxDamage, new Settings());
    }

    public ItemGradualInt(String name, int maxDamage, Settings settings) {
        super(name, settings);
        this.maxDamage = maxDamage;
    }

    @Override
    public int getCustomDamage(ItemStack stack) {
        return stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt().getInt(NBT_KEY);
    }

    @Override
    public int getMaxCustomDamage(ItemStack stack) {
        return maxDamage;
    }

    @Override
    public void setCustomDamage(ItemStack stack, int damage) {
        NbtCompound nbt = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
        nbt.putInt(NBT_KEY, damage);
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
    }

    /**
     * Wears the item down.
     *
     * @return false when the damage used it up, which is when the caller has to remove it
     */
    @Override
    public boolean applyCustomDamage(ItemStack stack, int damage, LivingEntity entity) {
        int newDamage = getCustomDamage(stack) + damage;
        if (newDamage > getMaxCustomDamage(stack)) {
            return false;
        }

        setCustomDamage(stack, newDamage);
        return true;
    }

    @Override
    public boolean isItemBarVisible(ItemStack stack) {
        return maxDamage > 0 && getCustomDamage(stack) > 0;
    }

    @Override
    public int getItemBarStep(ItemStack stack) {
        if (maxDamage <= 0) {
            return 13;
        }
        double left = 1.0 - (double) getCustomDamage(stack) / maxDamage;
        return Math.max(0, Math.min(13, (int) Math.round(left * 13.0)));
    }

    @Override
    public int getItemBarColor(ItemStack stack) {
        if (maxDamage <= 0) {
            return 0x00FF00;
        }
        float left = Math.max(0.0f, 1.0f - (float) getCustomDamage(stack) / maxDamage);
        return net.minecraft.util.math.MathHelper.hsvToRgb(left / 3.0f, 1.0f, 1.0f);
    }
}
