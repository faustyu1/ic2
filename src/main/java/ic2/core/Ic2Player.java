package ic2.core;

import com.mojang.authlib.GameProfile;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

/**
 * Fabric port of {@code ic2.core.Ic2Player}: the stand-in player IC2 acts as when nobody is
 * behind an action - a mining laser fired by a dispenser, say - so protection mods that only
 * listen for players still get asked.
 *
 * <p>One fake player per dimension, named {@code [IC2 <dimension>]} as before; 1.21 names
 * dimensions by id rather than number.
 */
public final class Ic2Player {
    private Ic2Player() {}

    /** @return the fake player of the world's dimension, or null on the client */
    public static PlayerEntity get(World world) {
        return world instanceof ServerWorld serverWorld ? FakePlayer.get(serverWorld, getGameProfile(serverWorld)) : null;
    }

    private static GameProfile getGameProfile(ServerWorld world) {
        String name = "[IC2 " + world.getRegistryKey().getValue() + "]";
        // a game profile name is limited to 16 characters
        String shortName = name.length() > 16 ? name.substring(0, 16) : name;
        UUID uuid = UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8));
        return new GameProfile(uuid, shortName);
    }
}
