package ic2.core.item;

import ic2.api.item.ElectricItem;
import java.util.Locale;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

/**
 * Fabric port of ItemBatteryChargeHotbar - a battery that keeps the hotbar charged.
 *
 * <p>Right clicking cycles the mode the original had (on, off, everything but the held item); the
 * mode lives in the item's custom data, where the port also keeps the charge.
 */
public class ItemBatteryChargeHotbar extends ItemElectric {

    public ItemBatteryChargeHotbar(String name, int tier, double maxCharge, double transferLimit) {
        super(name, tier, maxCharge, transferLimit);
    }

    /** What the battery charges. */
    public enum Mode {
        ENABLED(true),
        DISABLED(false),
        NOT_IN_HAND(true);

        public final boolean enabled;

        Mode(boolean enabled) {
            this.enabled = enabled;
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        Mode mode = getMode(stack);
        if (world.isClient || !(entity instanceof PlayerEntity player) || !mode.enabled
                || ElectricItem.manager == null) {
            return;
        }
        // the original spread the work over ten ticks, more often for the higher tiers
        if (world.getTime() % 10 >= getTier(stack)) {
            return;
        }

        double limit = getTransferLimit(stack);
        int tier = getTier(stack);

        for (int i = 0; i < 9 && limit > 0.0; i++) {
            ItemStack target = player.getInventory().getStack(i);
            if (target.isEmpty()
                    || (mode == Mode.NOT_IN_HAND && i == player.getInventory().selectedSlot)
                    || target.getItem() instanceof ItemBatteryChargeHotbar) {
                continue;
            }

            double charge = ElectricItem.manager.charge(target, limit, tier, false, true);
            charge = ElectricItem.manager.discharge(stack, charge, tier, true, false, false);
            ElectricItem.manager.charge(target, charge, tier, true, false);
            limit -= charge;
        }
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (world.isClient) {
            return TypedActionResult.pass(stack);
        }

        Mode mode = Mode.values()[(getMode(stack).ordinal() + 1) % Mode.values().length];
        setMode(stack, mode);
        player.sendMessage(Text.translatable("tooltip.mode",
                Text.translatable("tooltip.mode." + mode.name().toLowerCase(Locale.ROOT))), true);
        return TypedActionResult.success(stack);
    }

    public Mode getMode(ItemStack stack) {
        NbtCompound nbt = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
        if (!nbt.contains("mode")) {
            return Mode.ENABLED;
        }
        int mode = nbt.getByte("mode");
        return mode < 0 || mode >= Mode.values().length ? Mode.ENABLED : Mode.values()[mode];
    }

    public void setMode(ItemStack stack, Mode mode) {
        NbtCompound nbt = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
        nbt.putByte("mode", (byte) mode.ordinal());
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
    }
}
