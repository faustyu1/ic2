package ic2.core.item.armor.jetpack;

import ic2.core.IC2;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Fabric port of {@code ic2.core.item.armor.jetpack.JetpackLogic}: the flight itself.
 *
 * <p>Jump lifts, jump plus mode switch toggles hover mode, forward adds thrust, and in hover mode
 * jump and sneak set the ceiling and floor the pack holds. Everything above the world height
 * divided by the pack's divisor fades the thrust out over the last 25 blocks.
 *
 * <p>The client notes when it last fired the pack ({@link #lastClientThrust}); the engine loop
 * plays while it keeps doing so.
 */
public final class JetpackLogic {
    /** Ticks a hover toggle locks out the keys for. */
    private static final int TOGGLE_LOCKOUT = 10;
    /** The world tick the client last fired a jetpack on, which the client's jetpack loop follows. */
    public static long lastClientThrust = Long.MIN_VALUE;

    private JetpackLogic() {}

    /**
     * One tick of flight.
     *
     * @return true when the jetpack actually fired, which is what the sound and the energy cost
     *         hang off
     */
    public static boolean useJetpack(PlayerEntity player, boolean hoverMode, IJetpack jetpack, ItemStack stack) {
        if (jetpack.getChargeLevel(stack) <= 0.0) {
            return false;
        }

        IBoostingJetpack boosting = jetpack instanceof IBoostingJetpack b ? b : null;
        float power = jetpack.getPower(stack);
        float dropPercentage = jetpack.getDropPercentage(stack);

        // the last of the charge gives proportionally less thrust
        if (jetpack.getChargeLevel(stack) <= dropPercentage) {
            power = (float) (power * (jetpack.getChargeLevel(stack) / dropPercentage));
        }

        if (IC2.keyboard.isForwardKeyDown(player)) {
            float thrust;
            float boost;
            if (boosting != null) {
                thrust = boosting.getBaseThrust(stack, hoverMode);
                boost = boosting.getBoostThrust(player, stack, hoverMode);
            } else {
                thrust = hoverMode ? 1.0f : 0.15f;
                boost = 0.0f;
            }

            float forwardPower = power * thrust * 2.0f;
            if (forwardPower > 0.0f) {
                player.updateVelocity(0.02f + boost, new Vec3d(0.0, 0.0, 0.4f * forwardPower + boost));
                if (boost != 0.0f && !player.isOnGround()) {
                    boosting.useBoostPower(stack, boost);
                }
            }
        }

        int worldHeight = player.getWorld().getTopY();
        int maxFlightHeight = (int) (worldHeight / jetpack.getWorldHeightDivisor(stack));
        double y = player.getY();
        if (y > maxFlightHeight - 25) {
            if (y > maxFlightHeight) {
                y = maxFlightHeight;
            }
            power = (float) (power * ((maxFlightHeight - y) / 25.0));
        }

        double previousY = player.getVelocity().y;
        double newY = Math.min(previousY + power * 0.2f, 0.6f);

        if (hoverMode) {
            float maxHoverY = 0.0f;

            if (IC2.keyboard.isJumpKeyDown(player)) {
                maxHoverY += jetpack.getHoverMultiplier(stack, true);
                if (boosting != null) {
                    maxHoverY *= boosting.getHoverBoost(player, stack, true);
                }
            }

            if (IC2.keyboard.isSneakKeyDown(player)) {
                maxHoverY += -jetpack.getHoverMultiplier(stack, false);
                if (boosting != null) {
                    maxHoverY *= boosting.getHoverBoost(player, stack, false);
                }
            }

            if (newY > maxHoverY) {
                newY = maxHoverY;
                if (previousY > newY) {
                    newY = previousY;
                }
            }
        }

        player.setVelocity(player.getVelocity().x, newY, player.getVelocity().z);

        if (!player.isOnGround()) {
            jetpack.drainEnergy(stack, hoverMode ? 1 : 2);
        }

        player.fallDistance = 0.0f;
        player.horizontalSpeed = 0.0f;
        resetPlayerInAirTime(player);
        return true;
    }

    /**
     * The chest piece's share of {@code onArmorTick}. Both sides run it: the client moves the
     * player, the server charges for it and owns the hover flag.
     */
    public static void onArmorTick(World world, PlayerEntity player, ItemStack stack, IJetpack jetpack) {
        if (stack.isEmpty() || !jetpack.isJetpackActive(stack)) return;

        NbtCompound data = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
        boolean hoverMode = data.getBoolean("hoverMode");
        byte toggleTimer = data.getByte("toggleTimer");
        boolean changed = false;

        if (IC2.keyboard.isJumpKeyDown(player) && IC2.keyboard.isModeSwitchKeyDown(player) && toggleTimer == 0) {
            toggleTimer = TOGGLE_LOCKOUT;
            hoverMode = !hoverMode;
            if (!world.isClient) {
                data.putBoolean("hoverMode", hoverMode);
                player.sendMessage(Text.literal(hoverMode ? "Hover Mode enabled." : "Hover Mode disabled."), false);
                changed = true;
            }
        }

        if (IC2.keyboard.isJumpKeyDown(player) || hoverMode) {
            if (useJetpack(player, hoverMode, jetpack, stack) && world.isClient) {
                lastClientThrust = world.getTime();
            }

            // landing in hover mode switches it back off, so the pack does not idle on the ground
            if (player.isOnGround() && hoverMode && !world.isClient) {
                data.putBoolean("hoverMode", false);
                player.sendMessage(Text.literal("Hover Mode disabled."), false);
                changed = true;
            }
        }

        if (!world.isClient && toggleTimer > 0) {
            data.putByte("toggleTimer", (byte) (toggleTimer - 1));
            changed = true;
        }

        if (changed) {
            stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(data));
        }
    }

    /**
     * Keeps the server from kicking the player for flying. In 1.12.2 this was
     * {@code IC2.platform.resetPlayerInAirTime}; here it clears the floating counter the play
     * network handler keeps.
     */
    private static void resetPlayerInAirTime(PlayerEntity player) {
        if (player instanceof ServerPlayerEntity serverPlayer && serverPlayer.networkHandler != null) {
            ((ic2.core.mixin.ServerPlayNetworkHandlerAccessor) serverPlayer.networkHandler).ic2$setFloatingTicks(0);
        }
    }
}
