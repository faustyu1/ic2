package ic2.core.item.armor.jetpack;

import dev.emi.trinkets.api.TrinketsApi;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

/**
 * The Trinkets back slot, where the packs - the jetpacks, the battery packs and the CF pack - can
 * be worn instead of in the chest slot, so they leave room for a chestplate.
 */
public final class BackSlot {
    /** The items the back slot takes, {@code data/trinkets/tags/item/chest/back.json}. */
    public static final TagKey<net.minecraft.item.Item> ITEMS =
            TagKey.of(RegistryKeys.ITEM, Identifier.of("trinkets", "chest/back"));

    private BackSlot() {}

    /** The pack worn on the back, or an empty stack. */
    public static ItemStack get(LivingEntity entity) {
        return TrinketsApi.getTrinketComponent(entity)
                .flatMap(component -> component.getEquipped(stack -> stack.isIn(ITEMS)).stream().findFirst())
                .map(net.minecraft.util.Pair::getRight)
                .orElse(ItemStack.EMPTY);
    }

    /** The jetpack worn on the back, or an empty stack. */
    public static ItemStack getJetpack(LivingEntity entity) {
        ItemStack back = get(entity);
        return back.getItem() instanceof IJetpack ? back : ItemStack.EMPTY;
    }

    /**
     * The jetpack that flies: the one in the chest slot (or attached to the chestplate) if there
     * is one, else the one on the back - never both, which would fly twice as fast.
     */
    public static ItemStack getFlyingBackJetpack(LivingEntity entity) {
        if (JetpackHandler.hasJetpack(entity.getEquippedStack(EquipmentSlot.CHEST))) {
            return ItemStack.EMPTY;
        }
        return getJetpack(entity);
    }

    /** What the chest slot holds, or the pack on the back when the chest slot is empty. */
    public static ItemStack getChestOrBack(LivingEntity entity) {
        ItemStack chest = entity.getEquippedStack(EquipmentSlot.CHEST);
        return chest.isEmpty() ? get(entity) : chest;
    }
}
