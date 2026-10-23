package ic2.core.item.tool;

import ic2.core.init.IC2ItemVariants;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.TagKey;

/**
 * The bronze tool material, with the numbers EnumHelper.addToolMaterial("IC2_BRONZE", 2, 350, 6.0F,
 * 2.0F, 13) gave it in 1.12.2: stone's mining level, 350 uses, and a little better than iron at
 * digging.
 */
public final class BronzeToolMaterial implements ToolMaterial {
    public static final BronzeToolMaterial INSTANCE = new BronzeToolMaterial();

    private BronzeToolMaterial() {}

    @Override
    public int getDurability() {
        return 350;
    }

    @Override
    public float getMiningSpeedMultiplier() {
        return 6.0f;
    }

    @Override
    public float getAttackDamage() {
        return 2.0f;
    }

    @Override
    public TagKey<Block> getInverseTag() {
        // harvest level 2 in 1.12.2 terms, i.e. everything but the diamond tier
        return BlockTags.INCORRECT_FOR_IRON_TOOL;
    }

    @Override
    public int getEnchantability() {
        return 13;
    }

    @Override
    public Ingredient getRepairIngredient() {
        Item ingot = IC2ItemVariants.get("ingot", "bronze");
        return ingot == null ? Ingredient.EMPTY : Ingredient.ofStacks(new ItemStack(ingot));
    }
}
