package ic2.api.util;

import net.minecraft.entity.player.PlayerEntity;

/**
 * Port of {@code ic2.api.util.IKeyboard}: what IC2 equipment asks about the keys a player holds.
 *
 * <p>The answers come from the client, which sends its key state to the server whenever it
 * changes, so the server can ask the same questions as the client.
 */
public interface IKeyboard {
    boolean isAltKeyDown(PlayerEntity player);

    boolean isBoostKeyDown(PlayerEntity player);

    boolean isForwardKeyDown(PlayerEntity player);

    boolean isJumpKeyDown(PlayerEntity player);

    boolean isModeSwitchKeyDown(PlayerEntity player);

    boolean isSideinventoryKeyDown(PlayerEntity player);

    boolean isHudModeKeyDown(PlayerEntity player);

    boolean isSneakKeyDown(PlayerEntity player);
}
