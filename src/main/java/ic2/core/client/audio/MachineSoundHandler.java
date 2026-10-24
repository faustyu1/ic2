package ic2.core.client.audio;

import ic2.core.audio.MachineSound;
import ic2.core.block.BlockTileEntity;
import ic2.core.block.TileEntityBlock;
import ic2.core.block.machine.tileentity.TileEntityStandardMachine;
import ic2.core.init.MainConfig;
import ic2.core.util.ConfigUtil;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Map;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientBlockEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.MovingSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Plays the machines' running sounds on the client, the job of AudioManagerClient and the
 * machines' AudioSources in 1.12.2.
 *
 * <p>Every loaded IC2 block entity is tracked; each client tick the handler compares a machine's
 * {@link BlockTileEntity#ACTIVE} block state with what it last saw and starts or stops the
 * machine's {@link MachineSound}. 1.12.2 drove the same sounds from the networked "active" field
 * and the machines' start/interrupt/finish events; the port has neither, so the operation sound
 * of a macerator-like machine is restarted every operation length (as the server's operation
 * start did) and the interrupt sound plays when the machine stops away from the end of an
 * operation.
 *
 * <p>The volume is {@code audio/volume} of the IC2 config times the 1.2 IC2 gave every machine
 * (a volume over 1 carries further rather than louder, as in 1.12.2), and {@code audio/enabled =
 * false} silences the machines. Run with {@code -Dic2.debugAudio=true} to log every sound
 * started and stopped.
 */
public final class MachineSoundHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("IC2/Audio");
    private static final boolean DEBUG = Boolean.getBoolean("ic2.debugAudio");

    private static final Map<TileEntityBlock, Source> SOURCES = new IdentityHashMap<>();

    private MachineSoundHandler() {}

    public static void register() {
        ClientBlockEntityEvents.BLOCK_ENTITY_LOAD.register((blockEntity, world) -> {
            // the block says which machine it is; some block entities still carry an older TeBlock
            if (blockEntity instanceof TileEntityBlock tile && blockEntity.getCachedState().getBlock() instanceof BlockTileEntity block
                    && MachineSound.get(block.teBlock) != null) {
                SOURCES.putIfAbsent(tile, new Source(tile, MachineSound.get(block.teBlock)));
            }
        });
        ClientBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register((blockEntity, world) -> {
            Source source = blockEntity instanceof TileEntityBlock tile ? SOURCES.remove(tile) : null;
            if (source != null) {
                source.remove();
            }
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(MachineSoundHandler::clear));
        ClientTickEvents.END_CLIENT_TICK.register(MachineSoundHandler::tick);
    }

    private static void clear() {
        for (Source source : SOURCES.values()) {
            source.remove();
        }
        SOURCES.clear();
    }

    private static void tick(MinecraftClient client) {
        if (client.world == null) {
            if (!SOURCES.isEmpty()) {
                clear();
            }
            return;
        }
        if (client.isPaused()) {
            return;
        }
        for (Source source : new ArrayList<>(SOURCES.values())) {
            if (source.tile.isRemoved() || source.tile.getWorld() != client.world) {
                SOURCES.remove(source.tile);
                source.remove();
            } else {
                source.tick(client.getSoundManager());
            }
        }
    }

    private static float volume() {
        if (!ConfigUtil.getBool(MainConfig.get(), "audio/enabled")) {
            return 0.0F;
        }
        return (float) ConfigUtil.getDouble(MainConfig.get(), "audio/volume") * MachineSound.DEFAULT_VOLUME;
    }

    /** The sounds of one machine. */
    private static final class Source {
        final TileEntityBlock tile;
        final MachineSound sound;
        boolean active;
        Instance start;
        Instance loop;
        Instance extraLoop;
        SoundEvent extraLoopEvent;
        int ticksActive;

        Source(TileEntityBlock tile, MachineSound sound) {
            this.tile = tile;
            this.sound = sound;
        }

        void tick(SoundManager manager) {
            BlockState state = tile.getWorld().getBlockState(tile.getPos());
            boolean nowActive = state.contains(BlockTileEntity.ACTIVE) && state.get(BlockTileEntity.ACTIVE);
            if (nowActive != active) {
                active = nowActive;
                if (active) {
                    onStart(manager);
                } else {
                    onStop(manager);
                }
                return;
            }
            if (!active) {
                return;
            }

            ticksActive++;
            if (start != null && !manager.isPlaying(start)) {
                // the start sound ran out: on into the loop, as the FutureSound chain did
                start = null;
                loop = play(manager, sound.loop(), true);
            }
            if (sound.perOperation() && ticksActive % operationLength() == 0) {
                // a new operation began, which restarted the operation sound
                stop(manager, loop);
                loop = play(manager, sound.loop(), false);
            }
            if (sound.extra() != null && sound.extraPeriod() == 0 && extraLoopEvent() != extraLoopEvent) {
                // the block asks for another loop now, e.g. a faster Geiger counter
                stop(manager, extraLoop);
                extraLoopEvent = extraLoopEvent();
                extraLoop = play(manager, extraLoopEvent, true);
            }
            if (sound.extra() != null && sound.extraPeriod() > 0 && ticksActive % sound.extraPeriod() == 0) {
                play(manager, sound.extra(), false);
            }
        }

        private void onStart(SoundManager manager) {
            ticksActive = 0;
            if (sound.start() != null) {
                start = play(manager, sound.start(), false);
            } else {
                loop = play(manager, sound.loop(), !sound.perOperation());
            }
            if (sound.extra() != null && sound.extraPeriod() == 0) {
                extraLoopEvent = extraLoopEvent();
                extraLoop = play(manager, extraLoopEvent, true);
            }
        }

        private void onStop(SoundManager manager) {
            stopAll(manager);
            if (sound.stop() != null && (!sound.perOperation() || cutShort())) {
                play(manager, sound.stop(), false);
            }
        }

        private SoundEvent extraLoopEvent() {
            SoundEvent event = tile.getExtraLoopSound();
            return event != null ? event : sound.extra();
        }

        /** Whether the machine stopped in the middle of an operation rather than at its end. */
        private boolean cutShort() {
            int length = operationLength();
            int intoOperation = ticksActive % length;
            // the server's and the client's ticks drift by a tick or two
            return intoOperation > 2 && intoOperation < length - 2;
        }

        private int operationLength() {
            return tile instanceof TileEntityStandardMachine machine ? Math.max(1, machine.getOperationLength()) : 1;
        }

        void remove() {
            stopAll(MinecraftClient.getInstance().getSoundManager());
        }

        private void stopAll(SoundManager manager) {
            stop(manager, start);
            stop(manager, loop);
            stop(manager, extraLoop);
            start = loop = extraLoop = null;
        }

        private Instance play(SoundManager manager, SoundEvent event, boolean repeat) {
            float volume = volume();
            if (volume <= 0.0F) {
                return null;
            }
            Instance instance = new Instance(event, tile.getPos(), volume, repeat);
            manager.play(instance);
            if (DEBUG) {
                LOGGER.info("start {} at {} ({}, repeat={}, volume={})", event.getId(), tile.getPos(), tile.getCachedState().getBlock(), repeat, volume);
            }
            return instance;
        }

        private void stop(SoundManager manager, Instance instance) {
            if (instance != null) {
                instance.finish();
                manager.stop(instance);
                if (DEBUG) {
                    LOGGER.info("stop {} at {}", instance.getId(), tile.getPos());
                }
            }
        }
    }

    /** A machine sound at the centre of the machine's block. */
    private static final class Instance extends MovingSoundInstance {
        Instance(SoundEvent event, BlockPos pos, float volume, boolean repeat) {
            super(event, SoundCategory.BLOCKS, SoundInstance.createRandom());
            this.x = pos.getX() + 0.5;
            this.y = pos.getY() + 0.5;
            this.z = pos.getZ() + 0.5;
            this.volume = volume;
            this.repeat = repeat;
            this.repeatDelay = 0;
            this.attenuationType = AttenuationType.LINEAR;
        }

        void finish() {
            setDone();
        }

        @Override
        public void tick() {
        }
    }
}
