package ic2.core.audio;

import ic2.core.init.IC2Sounds;
import ic2.core.ref.TeBlock;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.sound.SoundEvent;
import org.jetbrains.annotations.Nullable;

/**
 * What a machine sounds like while it runs - the AudioSource set-up each 1.12.2 tile entity did
 * for itself in onNetworkUpdate("active") or onNetworkEvent.
 *
 * <p>Three kinds:
 * <ul>
 * <li>a loop that plays as long as the machine is active (generators, iron furnace, miner, pump,
 *     terraformer, teleporter, mass fabricator, reactor);</li>
 * <li>a start sound running into a loop, and a stop sound when the machine stops (electric and
 *     induction furnace);</li>
 * <li>an operation sound played from the start of every operation, and an interrupt sound when
 *     the machine stops in the middle of one (macerator, extractor, compressor, recycler).</li>
 * </ul>
 * Some add a second sound repeated on a period, such as the scrap rattling of the mass
 * fabricator, or a second loop (the reactor's Geiger counter).
 *
 * @param loop          the sound repeated while active; for an operation sound, the sound of one operation
 * @param start         played once when the machine starts, before the loop; null for none
 * @param stop          played once when the machine stops; null for none
 * @param perOperation  whether {@code loop} is an operation sound, restarted every operation and
 *                      interrupted by {@code stop} only when an operation is cut short
 * @param extra         a second sound while active: a loop when {@code extraPeriod} is 0, else played every
 *                      {@code extraPeriod} ticks; null for none
 */
public record MachineSound(SoundEvent loop, @Nullable SoundEvent start, @Nullable SoundEvent stop, boolean perOperation,
                           @Nullable SoundEvent extra, int extraPeriod) {

    /** The volume 1.12.2 gave every machine source (AudioManager.getDefaultVolume). */
    public static final float DEFAULT_VOLUME = 1.2F;
    /** The mass fabricator rattles its scrap every 41 client ticks while it runs. */
    public static final int SCRAP_PERIOD = 41;

    private static final Map<TeBlock, MachineSound> BY_BLOCK = new EnumMap<>(TeBlock.class);

    static {
        MachineSound generator = loop(IC2Sounds.GENERATORS_GENERATOR_LOOP);
        MachineSound geothermal = loop(IC2Sounds.GENERATORS_GEOTHERMAL_LOOP);
        BY_BLOCK.put(TeBlock.generator, generator);
        BY_BLOCK.put(TeBlock.geo_generator, geothermal);
        // the semifluid generator borrowed the geothermal generator's loop
        BY_BLOCK.put(TeBlock.semifluid_generator, geothermal);
        BY_BLOCK.put(TeBlock.water_generator, loop(IC2Sounds.GENERATORS_WATERMILL_LOOP));
        BY_BLOCK.put(TeBlock.wind_generator, loop(IC2Sounds.GENERATORS_WIND_GEN_LOOP));

        BY_BLOCK.put(TeBlock.electric_furnace, new MachineSound(IC2Sounds.MACHINES_ELECTRO_FURNACE_ELECTRO_FURNACE_LOOP,
                IC2Sounds.MACHINES_ELECTRO_FURNACE_ELECTRO_FURNACE_START, IC2Sounds.MACHINES_ELECTRO_FURNACE_ELECTRO_FURNACE_STOP, false, null, 0));
        BY_BLOCK.put(TeBlock.induction_furnace, new MachineSound(IC2Sounds.MACHINES_INDUCTION_FURNACE_INDUCTION_LOOP,
                IC2Sounds.MACHINES_INDUCTION_FURNACE_INDUCTION_START, IC2Sounds.MACHINES_INDUCTION_FURNACE_INDUCTION_STOP, false, null, 0));

        BY_BLOCK.put(TeBlock.macerator, operation(IC2Sounds.MACHINES_MACERATOR_OP));
        BY_BLOCK.put(TeBlock.extractor, operation(IC2Sounds.MACHINES_EXTRACTOR_OP));
        BY_BLOCK.put(TeBlock.compressor, operation(IC2Sounds.MACHINES_COMPRESSOR_OP));
        BY_BLOCK.put(TeBlock.recycler, operation(IC2Sounds.MACHINES_RECYCLER_OP));

        BY_BLOCK.put(TeBlock.iron_furnace, loop(IC2Sounds.MACHINES_IRON_FURNACE_OP));
        BY_BLOCK.put(TeBlock.miner, loop(IC2Sounds.MACHINES_MINER_OP));
        BY_BLOCK.put(TeBlock.pump, loop(IC2Sounds.MACHINES_PUMP_OP));
        BY_BLOCK.put(TeBlock.terraformer, loop(IC2Sounds.TERRAFORMERS_TERRAFORMER_GENERICLOOP));
        BY_BLOCK.put(TeBlock.teleporter, loop(IC2Sounds.MACHINES_TELEPORTER_TELE_CHARGED_LOOP));

        BY_BLOCK.put(TeBlock.mass_fabricator, new MachineSound(IC2Sounds.GENERATORS_MASS_FABRICATOR_MASS_FAB_LOOP, null, null, false,
                IC2Sounds.GENERATORS_MASS_FABRICATOR_MASS_FAB_SCRAP_SOLO, SCRAP_PERIOD));
        BY_BLOCK.put(TeBlock.matter_generator, loop(IC2Sounds.GENERATORS_MASS_FABRICATOR_MASS_FAB_LOOP));
        // 1.12.2 picked the Geiger counter by the EU output (low under 40, medium under 80);
        // the low Geiger counter by default; the reactor swaps in a faster one as its output rises
        BY_BLOCK.put(TeBlock.nuclear_reactor, new MachineSound(IC2Sounds.GENERATORS_NUCLEAR_REACTOR_NUCLEAR_REACTOR_LOOP, null, null, false,
                IC2Sounds.GENERATORS_NUCLEAR_REACTOR_GEIGER_LOW_EU, 0));
    }

    private static MachineSound loop(SoundEvent loop) {
        return new MachineSound(loop, null, null, false, null, 0);
    }

    private static MachineSound operation(SoundEvent op) {
        return new MachineSound(op, null, IC2Sounds.MACHINES_INTERRUPT_ONE, true, null, 0);
    }

    /** The sound of the machine, or null for a block that makes none while it runs. */
    public static @Nullable MachineSound get(TeBlock teBlock) {
        return BY_BLOCK.get(teBlock);
    }

    public static Map<TeBlock, MachineSound> all() {
        return Collections.unmodifiableMap(BY_BLOCK);
    }
}
