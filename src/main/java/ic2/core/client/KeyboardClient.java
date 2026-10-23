package ic2.core.client;

import java.util.EnumSet;
import java.util.Set;

import ic2.core.network.KeyUpdatePayload;
import ic2.core.util.Keyboard;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/**
 * Fabric port of {@code ic2.core.util.KeyboardClient}: reads the IC2 bindings plus the vanilla
 * forward and jump keys, and sends the packed state to the server whenever it changes.
 *
 * <p>The defaults are the 1.12.2 ones, translated from LWJGL 2 scan codes: left alt, left
 * control, {@code M}, {@code C} and {@code X}. Left control doubles as vanilla sprint, as it did
 * then.
 */
public class KeyboardClient extends Keyboard {
    private static final String CATEGORY = "key.categories.ic2";

    private final KeyBinding altKey = binding("key.ic2.alt", GLFW.GLFW_KEY_LEFT_ALT);
    private final KeyBinding boostKey = binding("key.ic2.boost", GLFW.GLFW_KEY_LEFT_CONTROL);
    private final KeyBinding modeSwitchKey = binding("key.ic2.mode_switch", GLFW.GLFW_KEY_M);
    private final KeyBinding sideInventoryKey = binding("key.ic2.side_inventory", GLFW.GLFW_KEY_C);
    private final KeyBinding hudModeKey = binding("key.ic2.hud_mode", GLFW.GLFW_KEY_X);

    private int lastKeyState = 0;

    private static KeyBinding binding(String translationKey, int code) {
        return KeyBindingHelper.registerKeyBinding(
                new KeyBinding(translationKey, InputUtil.Type.KEYSYM, code, CATEGORY));
    }

    /**
     * Collect the keys held this tick and, if anything changed, tell the server. The local player
     * is recorded here too, because the client side of the quantum suit asks the same questions.
     */
    @Override
    public void sendKeyUpdate() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        Set<Key> keys = EnumSet.noneOf(Key.class);

        // no key counts while a screen has the keyboard
        if (client.currentScreen == null) {
            if (altKey.isPressed()) keys.add(Key.alt);
            if (boostKey.isPressed()) keys.add(Key.boost);
            if (client.options.forwardKey.isPressed()) keys.add(Key.forward);
            if (modeSwitchKey.isPressed()) keys.add(Key.modeSwitch);
            if (client.options.jumpKey.isPressed()) keys.add(Key.jump);
            if (sideInventoryKey.isPressed()) keys.add(Key.sideInventory);
            if (hudModeKey.isPressed()) keys.add(Key.hubMode);
        }

        int currentKeyState = Key.toInt(keys);
        if (currentKeyState == lastKeyState) return;

        processKeyUpdate(client.player, currentKeyState);
        if (ClientPlayNetworking.canSend(KeyUpdatePayload.ID)) {
            ClientPlayNetworking.send(new KeyUpdatePayload(currentKeyState));
        }
        lastKeyState = currentKeyState;
    }
}
