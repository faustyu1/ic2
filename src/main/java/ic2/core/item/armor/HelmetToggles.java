package ic2.core.item.armor;

import ic2.api.item.ElectricItem;
import ic2.api.item.HudMode;
import ic2.core.IC2;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.world.World;

/**
 * The part of {@code onArmorTick} the nano helmet and the quantum helmet share in 1.12.2: ALT +
 * mode switch toggles night vision, ALT + HUD mode cycles the HUD, and night vision costs 1 EU a
 * tick while it is on.
 *
 * <p>Both the toggles and the effect run on the server only - it is the server that owns the
 * stack's data and that the key state is reported to.
 */
final class HelmetToggles {
    /** Ticks a toggle locks out the keys for, {@code toggleTimer} in 1.12.2. */
    private static final int TOGGLE_LOCKOUT = 10;

    private HelmetToggles() {}

    static void tick(World world, PlayerEntity player, ItemStack stack) {
        if (world.isClient) return;

        NbtCompound data = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
        byte toggleTimer = data.getByte("toggleTimer");
        boolean nightvision = data.getBoolean("Nightvision");
        short hudMode = data.getShort("HudMode");
        boolean changed = false;

        boolean alt = IC2.keyboard.isAltKeyDown(player);

        if (alt && IC2.keyboard.isModeSwitchKeyDown(player) && toggleTimer == 0) {
            toggleTimer = TOGGLE_LOCKOUT;
            nightvision = !nightvision;
            data.putBoolean("Nightvision", nightvision);
            player.sendMessage(Text.literal(nightvision ? "Nightvision enabled." : "Nightvision disabled."), false);
            changed = true;
        }

        if (alt && IC2.keyboard.isHudModeKeyDown(player) && toggleTimer == 0) {
            toggleTimer = TOGGLE_LOCKOUT;
            hudMode = hudMode == HudMode.getMaxMode() ? 0 : (short) (hudMode + 1);
            data.putShort("HudMode", hudMode);
            player.sendMessage(Text.translatable(HudMode.getFromID(hudMode).getTranslationKey()), false);
            changed = true;
        }

        if (toggleTimer > 0) {
            data.putByte("toggleTimer", (byte) (toggleTimer - 1));
            changed = true;
        }

        if (changed) {
            stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(data));
        }

        if (nightvision && ElectricItem.manager.use(stack, 1.0, player)) {
            ItemArmorNightvisionGoggles.applyNightvision(world, player);
        }
    }
}
