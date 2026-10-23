package ic2.core.item;

import ic2.core.IC2Potion;
import ic2.core.init.IC2Sounds;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

/**
 * Fabric port of {@code ic2.core.item.ItemIodineTablet}: swallowing tablets cuts the radiation
 * effect short, one second of it per tablet, and the whole stack goes at once.
 *
 * <p>Not a food item - in 1.12.2 it was a plain right click, with no eating animation and no
 * hunger involved - so the earlier placeholder that handed out resistance is gone.
 */
public class ItemIodineTablet extends ItemIC2 {

    public ItemIodineTablet() {
        super("iodine_tablet");
    }

    public ItemIodineTablet(String name) {
        super(name);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (world.isClient) {
            return TypedActionResult.pass(stack);
        }
        return onEaten(world, player, stack);
    }

    /** The 1.12.2 {@code onEaten}: each tablet takes one second off the effect. */
    public TypedActionResult<ItemStack> onEaten(World world, PlayerEntity player, ItemStack stack) {
        if (IC2Potion.radiationEntry == null) {
            return TypedActionResult.pass(stack);
        }

        StatusEffectInstance radiation = player.getStatusEffect(IC2Potion.radiationEntry);
        if (radiation == null) {
            return TypedActionResult.pass(stack);
        }

        int duration = radiation.getDuration() / 20;
        int amount = Math.min(stack.getCount(), duration);
        if (amount <= 0) {
            return TypedActionResult.pass(stack);
        }

        player.removeStatusEffect(IC2Potion.radiationEntry);
        if (amount < duration) {
            player.addStatusEffect(new StatusEffectInstance(IC2Potion.radiationEntry, (duration - amount) * 20));
        }

        stack.decrement(amount);
        world.playSound(null, player.getBlockPos(), IC2Sounds.TOOLS_EAT, SoundCategory.PLAYERS, 1.0f, 1.0f);
        return TypedActionResult.success(stack);
    }
}
