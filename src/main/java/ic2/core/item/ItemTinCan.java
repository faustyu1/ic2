package ic2.core.item;

import ic2.core.init.IC2ItemVariants;
import ic2.core.init.IC2Sounds;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

/**
 * Fabric port of {@code ic2.core.item.ItemTinCan}, the filled tin can.
 *
 * <p>Not a vanilla food: a right click eats as many cans as the player has hunger points to
 * fill, each giving one point of food, and hands back the empty cans - dropped when the
 * inventory is full, where 1.12.2 refused to eat. The quantum helmet feeds its wearer this way.
 */
public class ItemTinCan extends ItemIC2 {
    public ItemTinCan() {
        super("filled_tin_can");
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (world.isClient || !player.getHungerManager().isNotFull()) {
            return TypedActionResult.pass(stack);
        }
        return onEaten(player, stack) ? TypedActionResult.success(stack) : TypedActionResult.pass(stack);
    }

    /**
     * Eats from the stack, giving the empty cans back.
     *
     * @return false when nothing was eaten because the player is full
     */
    public static boolean onEaten(PlayerEntity player, ItemStack stack) {
        int amount = Math.min(stack.getCount(), 20 - player.getHungerManager().getFoodLevel());
        if (amount <= 0) {
            return false;
        }

        // 1.12.2 addStats(amount, amount): the saturation modifier counted twice per food point
        player.getHungerManager().add(amount, amount * amount * 2.0f);
        stack.decrement(amount);

        Item emptyCan = IC2ItemVariants.get("crafting", "tin_can");
        if (emptyCan != null) {
            ItemStack empty = new ItemStack(emptyCan, amount);
            if (!player.getInventory().insertStack(empty)) {
                player.dropItem(empty, false);
            }
        }
        player.getWorld().playSound(null, player.getBlockPos(), IC2Sounds.TOOLS_EAT, SoundCategory.PLAYERS, 1.0f, 1.0f);
        return true;
    }
}
