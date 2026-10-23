package ic2.core.util;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import ic2.api.util.IKeyboard;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Fabric port of {@code ic2.core.util.Keyboard}: the key state of every player, as the clients
 * last reported it.
 *
 * <p>The client subclass {@code ic2.core.client.KeyboardClient} reads the actual bindings and
 * sends the state; this class only remembers what arrived, which is what the server side needs.
 */
public class Keyboard implements IKeyboard {
    private final Map<PlayerEntity, Set<Key>> playerKeys = new WeakHashMap<>();

    @Override
    public boolean isAltKeyDown(PlayerEntity player) {
        return get(player, Key.alt);
    }

    @Override
    public boolean isBoostKeyDown(PlayerEntity player) {
        return get(player, Key.boost);
    }

    @Override
    public boolean isForwardKeyDown(PlayerEntity player) {
        return get(player, Key.forward);
    }

    @Override
    public boolean isJumpKeyDown(PlayerEntity player) {
        return get(player, Key.jump);
    }

    @Override
    public boolean isModeSwitchKeyDown(PlayerEntity player) {
        return get(player, Key.modeSwitch);
    }

    @Override
    public boolean isSideinventoryKeyDown(PlayerEntity player) {
        return get(player, Key.sideInventory);
    }

    @Override
    public boolean isHudModeKeyDown(PlayerEntity player) {
        return get(player, Key.hubMode);
    }

    @Override
    public boolean isSneakKeyDown(PlayerEntity player) {
        return player.isSneaking();
    }

    /** Called on the server when a key update arrives, and on the client for the local player. */
    public void processKeyUpdate(PlayerEntity player, int keyState) {
        if (player == null) return;
        playerKeys.put(player, Key.fromInt(keyState));
    }

    public void removePlayerReferences(PlayerEntity player) {
        playerKeys.remove(player);
    }

    /** The client subclass overrides this; on the server there is nothing to send. */
    public void sendKeyUpdate() {
    }

    private boolean get(PlayerEntity player, Key key) {
        Set<Key> keys = playerKeys.get(player);
        return keys != null && keys.contains(key);
    }

    /** The keys IC2 tracks, packed into one int by ordinal, exactly as in 1.12.2. */
    public enum Key {
        alt,
        boost,
        forward,
        modeSwitch,
        jump,
        sideInventory,
        hubMode;

        public static final Key[] keys = values();

        public static int toInt(Iterable<Key> keySet) {
            int ret = 0;
            for (Key key : keySet) {
                ret |= 1 << key.ordinal();
            }
            return ret;
        }

        public static Set<Key> fromInt(int keyState) {
            Set<Key> ret = EnumSet.noneOf(Key.class);
            int i = 0;

            while (keyState != 0 && i < keys.length) {
                if ((keyState & 1) != 0) {
                    ret.add(keys[i]);
                }
                i++;
                keyState >>= 1;
            }

            return ret;
        }
    }
}
