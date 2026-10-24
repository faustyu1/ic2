package ic2.core.client.audio;

import ic2.core.init.IC2Sounds;
import ic2.core.item.IHeldToolSounds;
import ic2.core.item.armor.jetpack.JetpackLogic;
import ic2.core.item.tool.ItemDrill;
import ic2.core.item.tool.ItemNanoSaber;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.sound.MovingSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * The sounds of the tools and the jetpack, which 1.12.2 played through AudioSources tied to the
 * player: the hum of a chainsaw or a switched on nano saber in the hand (with its power-up and
 * stop sounds), the saber's swing, the jetpack's engine while it fires and the drill's own
 * sound in place of the block's when it breaks one.
 */
public final class ToolSoundHandler {
    /** The tool each player last held and the loop it plays. */
    private static final Map<UUID, Held> HELD = new HashMap<>();
    private static @Nullable Loop jetpackLoop;
    /** The block the local player is breaking with a drill, whose own break sound the drill replaces. */
    private static @Nullable BlockPos drillBreakPos;
    private static float drillBreakHardness;

    private ToolSoundHandler() {}

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(ToolSoundHandler::tick);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(ToolSoundHandler::clear));
    }

    private static void clear() {
        HELD.values().forEach(held -> stop(held.loop));
        HELD.clear();
        stop(jetpackLoop);
        jetpackLoop = null;
        // a world time from the last world would read as a thrust in the future in the next one
        JetpackLogic.lastClientThrust = Long.MIN_VALUE;
    }

    private static void tick(MinecraftClient client) {
        if (client.world == null) {
            clear();
            return;
        }
        if (client.isPaused()) {
            return;
        }
        Map<UUID, Held> seen = new HashMap<>();
        for (PlayerEntity player : client.world.getPlayers()) {
            Held held = HELD.remove(player.getUuid());
            seen.put(player.getUuid(), tickHeld(client, player, held == null ? new Held() : held));
            tickSwing(player);
        }
        HELD.values().forEach(held -> stop(held.loop));
        HELD.clear();
        HELD.putAll(seen);
        tickJetpack(client);
    }

    /** Starts, keeps or stops the loop of the tool in a player's main hand. */
    private static Held tickHeld(MinecraftClient client, PlayerEntity player, Held held) {
        ItemStack stack = player.getMainHandStack();
        IHeldToolSounds tool = stack.getItem() instanceof IHeldToolSounds sounds ? sounds : null;
        SoundEvent idle = tool == null ? null : tool.getIdleSound(stack);
        Item running = idle == null ? null : stack.getItem();
        if (running == held.item) {
            return held;
        }
        if (held.item != null) {
            stop(held.loop);
            held.loop = null;
            if (held.stopSound != null) {
                playAt(player, held.stopSound);
            }
        }
        held.item = running;
        held.stopSound = null;
        if (running != null) {
            held.loop = play(client, player, idle);
            SoundEvent start = tool.getStartSound(stack);
            if (start != null) {
                playAt(player, start);
            }
            held.stopSound = tool.getStopSound(stack);
        }
        return held;
    }

    /** A switched on nano saber whooshes as it starts a swing. */
    private static void tickSwing(PlayerEntity player) {
        ItemStack stack = player.getMainHandStack();
        if (player.handSwinging && player.handSwingTicks == 0
                && stack.getItem() instanceof ItemNanoSaber && ItemNanoSaber.isActive(stack)) {
            playAt(player, ItemNanoSaber.randomSwingSound(player.getRandom()));
        }
    }

    /** The engine loop follows the local player while the jetpack fires. */
    private static void tickJetpack(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        long sinceThrust = client.world.getTime() - JetpackLogic.lastClientThrust;
        boolean firing = player != null && sinceThrust >= 0 && sinceThrust <= 2;
        if (firing && (jetpackLoop == null || jetpackLoop.isDone())) {
            jetpackLoop = play(client, player, IC2Sounds.TOOLS_JETPACK_JETPACK_LOOP);
        } else if (!firing && jetpackLoop != null) {
            stop(jetpackLoop);
            jetpackLoop = null;
        }
    }

    // ---------------------------------------------------------------- the drill's break sound

    /** Called as the local player breaks a block: a drill in hand takes over its break sound. */
    public static void beginBreak(MinecraftClient client, BlockPos pos) {
        drillBreakPos = null;
        if (client.player != null && client.world != null && !client.player.isCreative()
                && client.player.getMainHandStack().getItem() instanceof ItemDrill) {
            drillBreakPos = pos.toImmutable();
            drillBreakHardness = client.world.getBlockState(pos).getHardness(client.world, pos);
        }
    }

    /** After the break: the drill's sound, soft up to the hardness of dirt and hard above. */
    public static void endBreak(MinecraftClient client, boolean broken) {
        BlockPos pos = drillBreakPos;
        drillBreakPos = null;
        if (pos != null && broken && client.world != null) {
            SoundEvent sound = drillBreakHardness > 1.0F || drillBreakHardness < 0.0F
                    ? IC2Sounds.TOOLS_DRILL_DRILL_HARD : IC2Sounds.TOOLS_DRILL_DRILL_SOFT;
            IC2Sounds.play(client.world, null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, sound);
        }
    }

    /** Whether the block break sound about to play is the one the drill replaces. */
    public static boolean suppressesBreakSound(World world, BlockPos pos, SoundCategory category) {
        return world.isClient && category == SoundCategory.BLOCKS && pos.equals(drillBreakPos);
    }

    // ---------------------------------------------------------------- playing

    private static void playAt(Entity entity, SoundEvent sound) {
        IC2Sounds.play(entity.getWorld(), null, entity.getX(), entity.getY() + 1.0, entity.getZ(), sound);
    }

    private static @Nullable Loop play(MinecraftClient client, Entity entity, SoundEvent sound) {
        float volume = IC2Sounds.volume();
        if (volume <= 0.0F) {
            return null;
        }
        Loop loop = new Loop(sound, entity, volume);
        client.getSoundManager().play(loop);
        return loop;
    }

    private static void stop(@Nullable Loop loop) {
        if (loop != null) {
            MinecraftClient.getInstance().getSoundManager().stop(loop);
        }
    }

    private static final class Held {
        @Nullable Item item;
        @Nullable Loop loop;
        @Nullable SoundEvent stopSound;
    }

    /** A repeating sound that follows an entity around, as PositionSpec.Hand / Backpack did. */
    private static final class Loop extends MovingSoundInstance {
        private final Entity entity;

        Loop(SoundEvent sound, Entity entity, float volume) {
            super(sound, SoundCategory.PLAYERS, SoundInstance.createRandom());
            this.entity = entity;
            this.repeat = true;
            this.repeatDelay = 0;
            this.volume = volume;
            this.x = entity.getX();
            this.y = entity.getY() + 1.0;
            this.z = entity.getZ();
        }

        @Override
        public void tick() {
            if (entity.isRemoved()) {
                setDone();
                return;
            }
            x = entity.getX();
            y = entity.getY() + 1.0;
            z = entity.getZ();
        }
    }
}
