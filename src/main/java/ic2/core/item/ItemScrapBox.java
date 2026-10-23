package ic2.core.item;

import ic2.api.recipe.Recipes;
import net.minecraft.block.DispenserBlock;
import net.minecraft.block.dispenser.ItemDispenserBehavior;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPointer;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Position;
import net.minecraft.world.World;

/**
 * The scrap box, {@code ItemHandlers.scrapBoxUnpack} and {@code BehaviorScrapboxDispense} of
 * 1.12.2.
 *
 * <p>Right click opens one: the drop {@link Recipes#scrapboxDrops} rolls is thrown in front of the
 * player and the box is used up, except in creative mode. A dispenser opens them as well and
 * shoots the drop out.
 */
public class ItemScrapBox extends ItemCrafting {

    public ItemScrapBox(String name) {
        super(name);
        DispenserBlock.registerBehavior(this, new ItemDispenserBehavior() {
            @Override
            protected ItemStack dispenseSilently(BlockPointer pointer, ItemStack stack) {
                if (Recipes.scrapboxDrops == null) {
                    return stack;
                }
                ItemStack drop = Recipes.scrapboxDrops.getDrop(stack, true);
                if (drop != null && !drop.isEmpty()) {
                    Direction facing = pointer.state().get(DispenserBlock.FACING);
                    Position position = DispenserBlock.getOutputLocation(pointer);
                    spawnItem(pointer.world(), drop, 6, facing, position);
                }
                return stack;
            }
        });
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (!world.isClient && Recipes.scrapboxDrops != null) {
            ItemStack drop = Recipes.scrapboxDrops.getDrop(stack, false);
            if (drop != null && player.dropItem(drop, false) != null && !player.getAbilities().creativeMode) {
                stack.decrement(1);
                return TypedActionResult.success(stack);
            }
        }
        return TypedActionResult.pass(stack);
    }
}
