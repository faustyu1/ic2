package ic2.core.item;

import ic2.api.item.ElectricItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

/**
 * Fabric port of ItemBatterySU - the single use battery.
 *
 * <p>It is not an electric item: right clicking pours its whole charge into the hotbar and the
 * battery itself is used up, which is the only thing it ever did.
 */
public class ItemBatterySU extends ItemIC2 {
    private final int capacity;
    private final int tier;

    public ItemBatterySU(String name, int capacity, int tier) {
        // it stacks, as in 1.12.2: the recipe hands out six at a time
        super(name, new Settings());
        this.capacity = capacity;
        this.tier = tier;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (ElectricItem.manager == null) {
            return TypedActionResult.pass(stack);
        }

        double energy = capacity;
        for (int i = 0; i < 9 && energy > 0.0; i++) {
            ItemStack target = player.getInventory().getStack(i);
            if (!target.isEmpty() && target != stack) {
                energy -= ElectricItem.manager.charge(target, energy, tier, true, false);
            }
        }

        if (energy == capacity) {
            return TypedActionResult.pass(stack);
        }

        stack.decrement(1);
        return TypedActionResult.success(stack, world.isClient);
    }

    public int getCapacity() {
        return capacity;
    }

    public int getTier() {
        return tier;
    }
}
