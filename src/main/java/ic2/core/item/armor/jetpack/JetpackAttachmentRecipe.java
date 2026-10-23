package ic2.core.item.armor.jetpack;

import ic2.api.item.ElectricItem;
import ic2.core.init.IC2ItemVariants;
import ic2.core.init.MainConfig;
import ic2.core.util.ConfigUtil;
import java.text.ParseException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.recipe.SpecialRecipeSerializer;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

/**
 * Port of {@code ic2.core.item.armor.jetpack.JetpackAttachmentRecipe}: an electric jetpack, a
 * chestplate and a jetpack attachment plate, anywhere in the crafting grid, give the chestplate
 * with the jetpack attached (see {@link JetpackHandler}), carrying over the jetpack's charge.
 *
 * <p>Anything worn in the chest slot qualifies except the jetpacks themselves, the quantum
 * chestplate (which flies already) and what {@code recipes/jetpackAttachmentBlacklist} lists; a
 * chestplate that already has a jetpack is refused.
 */
public class JetpackAttachmentRecipe extends SpecialCraftingRecipe {
    public static final Set<Item> blacklistedItems = Collections.newSetFromMap(new IdentityHashMap<>());
    public static RecipeSerializer<JetpackAttachmentRecipe> SERIALIZER;

    public JetpackAttachmentRecipe(CraftingRecipeCategory category) {
        super(category);
    }

    /** Registers the recipe type and fills the blacklist; the items have to exist. */
    public static void init() {
        if (SERIALIZER == null) {
            SERIALIZER = Registry.register(Registries.RECIPE_SERIALIZER, Identifier.of("ic2", "jetpack_attachment"),
                    new SpecialRecipeSerializer<>(JetpackAttachmentRecipe::new));
        }

        blacklistedItems.clear();
        for (String name : new String[] {"jetpack", "jetpack_electric", "quantum_chestplate"}) {
            blacklistedItems.add(Registries.ITEM.get(Identifier.of("ic2", name)));
        }
        for (String part : MainConfig.get().getList("recipes/jetpackAttachmentBlacklist")) {
            try {
                ItemStack stack = ConfigUtil.asStack(part);
                if (stack != null && !stack.isEmpty()) {
                    blacklistedItems.add(stack.getItem());
                }
            } catch (ParseException e) {
                // an unparsable entry blacklists nothing
            }
        }
    }

    @Override
    public boolean matches(CraftingRecipeInput input, World world) {
        return !craft(input, world.getRegistryManager()).isEmpty();
    }

    @Override
    public ItemStack craft(CraftingRecipeInput input, RegistryWrapper.WrapperLookup lookup) {
        Item jetpackItem = Registries.ITEM.get(Identifier.of("ic2", "jetpack_electric"));
        Item plate = IC2ItemVariants.get("crafting", "jetpack_attachment_plate");
        ItemStack jetpack = null;
        ItemStack armor = null;
        boolean attachmentPlate = false;

        for (int i = 0; i < input.getSize(); i++) {
            ItemStack stack = input.getStackInSlot(i);
            if (stack.isEmpty()) {
                continue;
            }

            Item item = stack.getItem();
            if (item == jetpackItem) {
                if (jetpack != null) {
                    return ItemStack.EMPTY;
                }
                jetpack = stack;
            } else if (JetpackHandler.isChestArmor(stack) && !blacklistedItems.contains(item)) {
                if (armor != null) {
                    return ItemStack.EMPTY;
                }
                armor = stack;
            } else {
                if (item != plate || attachmentPlate) {
                    return ItemStack.EMPTY;
                }
                attachmentPlate = true;
            }
        }

        if (jetpack == null || armor == null || !attachmentPlate || JetpackHandler.hasJetpackAttached(armor)) {
            return ItemStack.EMPTY;
        }

        ItemStack ret = armor.copyWithCount(1);
        JetpackHandler.setJetpackAttached(ret, true);
        ElectricItem.manager.charge(ret, ElectricItem.manager.getCharge(jetpack), Integer.MAX_VALUE, true, false);
        return ret;
    }

    @Override
    public boolean fits(int width, int height) {
        return width * height >= 3;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }
}
