package ic2.core.init;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

/**
 * IC2 sound events.
 *
 * <p>Generated from the original 1.12 sound tree; the 1.12 code addressed sounds by
 * raw file path ("Machines/MaceratorOp.ogg"), so {@link #byLegacyPath(String)} keeps
 * that mapping available while the machine classes are being ported.
 */
public final class IC2Sounds {

    private static final Map<String, SoundEvent> BY_LEGACY_PATH = new LinkedHashMap<>();

    public static final SoundEvent GENERATORS_GENERATOR_LOOP = register("generators.generator_loop", "Generators/GeneratorLoop.ogg");
    public static final SoundEvent GENERATORS_GEOTHERMAL_LOOP = register("generators.geothermal_loop", "Generators/GeothermalLoop.ogg");
    public static final SoundEvent GENERATORS_MASS_FABRICATOR_MASS_FAB_LOOP = register("generators.mass_fabricator.mass_fab_loop", "Generators/MassFabricator/MassFabLoop.ogg");
    public static final SoundEvent GENERATORS_MASS_FABRICATOR_MASS_FAB_SCRAP_SOLO = register("generators.mass_fabricator.mass_fab_scrap_solo", "Generators/MassFabricator/MassFabScrapSolo.ogg");
    public static final SoundEvent GENERATORS_NUCLEAR_REACTOR_GEIGER_HIGH_EU = register("generators.nuclear_reactor.geiger_high_eu", "Generators/NuclearReactor/GeigerHighEU.ogg");
    public static final SoundEvent GENERATORS_NUCLEAR_REACTOR_GEIGER_LOW_EU = register("generators.nuclear_reactor.geiger_low_eu", "Generators/NuclearReactor/GeigerLowEU.ogg");
    public static final SoundEvent GENERATORS_NUCLEAR_REACTOR_GEIGER_MED_EU = register("generators.nuclear_reactor.geiger_med_eu", "Generators/NuclearReactor/GeigerMedEU.ogg");
    public static final SoundEvent GENERATORS_NUCLEAR_REACTOR_NUCLEAR_REACTOR_LOOP = register("generators.nuclear_reactor.nuclear_reactor_loop", "Generators/NuclearReactor/NuclearReactorLoop.ogg");
    public static final SoundEvent GENERATORS_WATERMILL_LOOP = register("generators.watermill_loop", "Generators/WatermillLoop.ogg");
    public static final SoundEvent GENERATORS_WIND_GEN_LOOP = register("generators.wind_gen_loop", "Generators/WindGenLoop.ogg");
    public static final SoundEvent MACHINES_CANNER_OP = register("machines.canner_op", "Machines/CannerOp.ogg");
    public static final SoundEvent MACHINES_CANNER_REVERSE_OP = register("machines.canner_reverse_op", "Machines/CannerReverseOp.ogg");
    public static final SoundEvent MACHINES_COMPRESSOR_OP = register("machines.compressor_op", "Machines/CompressorOp.ogg");
    public static final SoundEvent MACHINES_ELECTRO_FURNACE_ELECTRO_FURNACE_LOOP = register("machines.electro_furnace.electro_furnace_loop", "Machines/Electro Furnace/ElectroFurnaceLoop.ogg");
    public static final SoundEvent MACHINES_ELECTRO_FURNACE_ELECTRO_FURNACE_START = register("machines.electro_furnace.electro_furnace_start", "Machines/Electro Furnace/ElectroFurnaceStart.ogg");
    public static final SoundEvent MACHINES_ELECTRO_FURNACE_ELECTRO_FURNACE_STOP = register("machines.electro_furnace.electro_furnace_stop", "Machines/Electro Furnace/ElectroFurnaceStop.ogg");
    public static final SoundEvent MACHINES_ELECTROLYZER_LOOP = register("machines.electrolyzer_loop", "Machines/ElectrolyzerLoop.ogg");
    public static final SoundEvent MACHINES_EXTRACTOR_OP = register("machines.extractor_op", "Machines/ExtractorOp.ogg");
    public static final SoundEvent MACHINES_FORCEFIELD_LOOP = register("machines.forcefield_loop", "Machines/ForcefieldLoop.ogg");
    public static final SoundEvent MACHINES_INDUCTION_FURNACE_INDUCTION_LOOP = register("machines.induction_furnace.induction_loop", "Machines/Induction Furnace/InductionLoop.ogg");
    public static final SoundEvent MACHINES_INDUCTION_FURNACE_INDUCTION_START = register("machines.induction_furnace.induction_start", "Machines/Induction Furnace/InductionStart.ogg");
    public static final SoundEvent MACHINES_INDUCTION_FURNACE_INDUCTION_STOP = register("machines.induction_furnace.induction_stop", "Machines/Induction Furnace/InductionStop.ogg");
    public static final SoundEvent MACHINES_INTERRUPT_ONE = register("machines.interrupt_one", "Machines/InterruptOne.ogg");
    public static final SoundEvent MACHINES_IRON_FURNACE_OP = register("machines.iron_furnace_op", "Machines/IronFurnaceOp.ogg");
    public static final SoundEvent MACHINES_MACERATOR_OP = register("machines.macerator_op", "Machines/MaceratorOp.ogg");
    public static final SoundEvent MACHINES_MACHINE_OVERLOAD = register("machines.machine_overload", "Machines/MachineOverload.ogg");
    public static final SoundEvent MACHINES_MAGNETIZER_LOOP = register("machines.magnetizer_loop", "Machines/MagnetizerLoop.ogg");
    public static final SoundEvent MACHINES_MINER_OP = register("machines.miner_op", "Machines/MinerOp.ogg");
    public static final SoundEvent MACHINES_O_MAT = register("machines.o_mat", "Machines/o-mat.ogg");
    public static final SoundEvent MACHINES_PUMP_OP = register("machines.pump_op", "Machines/PumpOp.ogg");
    public static final SoundEvent MACHINES_RECYCLER_OP = register("machines.recycler_op", "Machines/RecyclerOp.ogg");
    public static final SoundEvent MACHINES_TELEPORTER_TELE_CHARGED_LOOP = register("machines.teleporter.tele_charged_loop", "Machines/Teleporter/TeleChargedLoop.ogg");
    public static final SoundEvent MACHINES_TELEPORTER_TELE_USE = register("machines.teleporter.tele_use", "Machines/Teleporter/TeleUse.ogg");
    public static final SoundEvent TERRAFORMERS_TERRAFORMER_GENERICLOOP = register("terraformers.terraformer_genericloop", "Terraformers/TerraformerGenericloop.ogg");
    public static final SoundEvent TOOLS_BATTERY_USE = register("tools.battery_use", "Tools/BatteryUse.ogg");
    public static final SoundEvent TOOLS_CHAINSAW_CHAINSAW_IDLE = register("tools.chainsaw.chainsaw_idle", "Tools/Chainsaw/ChainsawIdle.ogg");
    public static final SoundEvent TOOLS_CHAINSAW_CHAINSAW_STOP = register("tools.chainsaw.chainsaw_stop", "Tools/Chainsaw/ChainsawStop.ogg");
    public static final SoundEvent TOOLS_CHAINSAW_CHAINSAW_USE_ONE = register("tools.chainsaw.chainsaw_use_one", "Tools/Chainsaw/ChainsawUseOne.ogg");
    public static final SoundEvent TOOLS_CHAINSAW_CHAINSAW_USE_TWO = register("tools.chainsaw.chainsaw_use_two", "Tools/Chainsaw/ChainsawUseTwo.ogg");
    public static final SoundEvent TOOLS_CROWBAR = register("tools.crowbar", "Tools/Crowbar.ogg");
    public static final SoundEvent TOOLS_DRILL_DRILL_HARD = register("tools.drill.drill_hard", "Tools/Drill/DrillHard.ogg");
    public static final SoundEvent TOOLS_DRILL_DRILL_SOFT = register("tools.drill.drill_soft", "Tools/Drill/DrillSoft.ogg");
    public static final SoundEvent TOOLS_DRILL_DRILL_USE_LOOP = register("tools.drill.drill_use_loop", "Tools/Drill/DrillUseLoop.ogg");
    public static final SoundEvent TOOLS_DYNAMITEOMOTE = register("tools.dynamiteomote", "Tools/dynamiteomote.ogg");
    public static final SoundEvent TOOLS_EAT = register("tools.eat", "Tools/eat.ogg");
    public static final SoundEvent TOOLS_INSULATION_CUTTERS = register("tools.insulation_cutters", "Tools/InsulationCutters.ogg");
    public static final SoundEvent TOOLS_JETPACK_JETPACK_FIRE = register("tools.jetpack.jetpack_fire", "Tools/Jetpack/JetpackFire.ogg");
    public static final SoundEvent TOOLS_JETPACK_JETPACK_LOOP = register("tools.jetpack.jetpack_loop", "Tools/Jetpack/JetpackLoop.ogg");
    public static final SoundEvent TOOLS_MINING_LASER_MINING_LASER = register("tools.mining_laser.mining_laser", "Tools/MiningLaser/MiningLaser.ogg");
    public static final SoundEvent TOOLS_MINING_LASER_MINING_LASER_EXPLOSIVE = register("tools.mining_laser.mining_laser_explosive", "Tools/MiningLaser/MiningLaserExplosive.ogg");
    public static final SoundEvent TOOLS_MINING_LASER_MINING_LASER_LONG_RANGE = register("tools.mining_laser.mining_laser_long_range", "Tools/MiningLaser/MiningLaserLongRange.ogg");
    public static final SoundEvent TOOLS_MINING_LASER_MINING_LASER_LOW_FOCUS = register("tools.mining_laser.mining_laser_low_focus", "Tools/MiningLaser/MiningLaserLowFocus.ogg");
    public static final SoundEvent TOOLS_MINING_LASER_MINING_LASER_SCATTER = register("tools.mining_laser.mining_laser_scatter", "Tools/MiningLaser/MiningLaserScatter.ogg");
    public static final SoundEvent TOOLS_NANOSABRE_NANOSABRE_IDLE = register("tools.nanosabre.nanosabre_idle", "Tools/Nanosabre/NanosabreIdle.ogg");
    public static final SoundEvent TOOLS_NANOSABRE_NANOSABRE_POWERUP = register("tools.nanosabre.nanosabre_powerup", "Tools/Nanosabre/NanosabrePowerup.ogg");
    public static final SoundEvent TOOLS_NANOSABRE_NANOSABRE_SWING_1 = register("tools.nanosabre.nanosabre_swing_1", "Tools/Nanosabre/NanosabreSwing1.ogg");
    public static final SoundEvent TOOLS_NANOSABRE_NANOSABRE_SWING_2 = register("tools.nanosabre.nanosabre_swing_2", "Tools/Nanosabre/NanosabreSwing2.ogg");
    public static final SoundEvent TOOLS_NANOSABRE_NANOSABRE_SWING_3 = register("tools.nanosabre.nanosabre_swing_3", "Tools/Nanosabre/NanosabreSwing3.ogg");
    public static final SoundEvent TOOLS_NUKE_EXPLOSION = register("tools.nuke_explosion", "Tools/NukeExplosion.ogg");
    public static final SoundEvent TOOLS_OD_SCANNER = register("tools.od_scanner", "Tools/ODScanner.ogg");
    public static final SoundEvent TOOLS_PAINTER = register("tools.painter", "Tools/Painter.ogg");
    public static final SoundEvent TOOLS_QUANTUM_SUIT_HELMET_LOOP = register("tools.quantum_suit.helmet_loop", "Tools/QuantumSuit/HelmetLoop.ogg");
    public static final SoundEvent TOOLS_QUANTUM_SUIT_QUANTUMSUIT_BOOTS = register("tools.quantum_suit.quantumsuit_boots", "Tools/QuantumSuit/QuantumsuitBoots.ogg");
    public static final SoundEvent TOOLS_RUBBER_TRAMPOLINE = register("tools.rubber_trampoline", "Tools/RubberTrampoline.ogg");
    public static final SoundEvent TOOLS_TREETAP = register("tools.treetap", "Tools/Treetap.ogg");
    public static final SoundEvent TOOLS_WRENCH = register("tools.wrench", "Tools/wrench.ogg");

    private IC2Sounds() {}

    /**
     * The volume IC2 plays its one-off sounds at: {@code audio/volume} of the IC2 config times the
     * 1.2 the 1.12.2 audio manager used, or 0 when {@code audio/enabled} is false.
     */
    public static float volume() {
        ic2.core.util.Config config = ic2.core.init.MainConfig.get();
        if (!ic2.core.util.ConfigUtil.getBool(config, "audio/enabled")) {
            return 0.0F;
        }
        return (float) ic2.core.util.ConfigUtil.getDouble(config, "audio/volume") * 1.2F;
    }

    /**
     * Plays a sound at a spot for everyone near it. Called on the server it reaches every client;
     * called on the client it only plays there, which is what the 1.12.2 playOnce did.
     */
    public static void play(net.minecraft.world.World world, @org.jetbrains.annotations.Nullable net.minecraft.entity.player.PlayerEntity except,
                            double x, double y, double z, SoundEvent event) {
        float volume = volume();
        if (volume <= 0.0F) {
            return;
        }
        if (world.isClient) {
            // the client's playSound with a source only plays for the local player as that source
            world.playSound(x, y, z, event, net.minecraft.sound.SoundCategory.PLAYERS, volume, 1.0F, false);
        } else {
            world.playSound(except, x, y, z, event, net.minecraft.sound.SoundCategory.PLAYERS, volume, 1.0F);
        }
    }

    /** Forces class initialisation so every sound event lands in the registry. */
    public static void registerAll() {}

    /** @return the sound the 1.12 code referenced as e.g. "Machines/MaceratorOp.ogg" */
    public static SoundEvent byLegacyPath(String path) {
        return BY_LEGACY_PATH.get(path);
    }

    public static Map<String, SoundEvent> legacyPaths() {
        return Collections.unmodifiableMap(BY_LEGACY_PATH);
    }

    private static SoundEvent register(String name, String legacyPath) {
        Identifier id = Identifier.of("ic2", name);
        SoundEvent event = Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
        BY_LEGACY_PATH.put(legacyPath, event);
        return event;
    }
}
