package ic2.core;

import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Port of ic2.core.IC2DamageSource for 1.21.1.
 * <p>
 * In 1.12: {@code extends DamageSource} with string type "ic2.explosion" / nuke / electricity.
 * In 1.21.1: {@code DamageSource} requires {@code RegistryEntry<DamageType>}.
 * Custom type "ic2.explosion" does not exist in vanilla registry, so this stub
 * aliases to {@link DamageTypes#EXPLOSION} (and GENERIC for electricity/radiation)
 * and exposes factories via {@code world.getRegistryManager()} or
 * {@code world.getDamageSources()}.
 */
public class IC2DamageSource extends DamageSource {

    /** Logical id for the IC2 explosion damage type (stub -> vanilla EXPLOSION). */
    public static final String EXPLOSION_MSG_ID = "ic2.explosion";

    /**
     * The one IC2 damage type that is a real registered type rather than an alias:
     * {@code data/ic2/damage_type/radiation.json}. The hazmat suit keeps it out.
     */
    public static final RegistryKey<DamageType> RADIATION =
            RegistryKey.of(RegistryKeys.DAMAGE_TYPE, net.minecraft.util.Identifier.of("ic2", "radiation"));

    // Legacy static instances from 1.12 — kept as deprecated stubs (null until world available)
    @Deprecated
    public static IC2DamageSource electricity = null;
    @Deprecated
    public static IC2DamageSource nuke = null;
    @Deprecated
    public static IC2DamageSource radiation = null;

    public IC2DamageSource(RegistryEntry<DamageType> type) {
        super(type);
    }

    public IC2DamageSource(RegistryEntry<DamageType> type, Entity source) {
        super(type, source);
    }

    public IC2DamageSource(RegistryEntry<DamageType> type, Entity source, Entity attacker) {
        super(type, source, attacker);
    }

    // ---- factories ----

    /**
     * Create a damage source for the logical "ic2.explosion" type.
     * Uses {@link DamageTypes#EXPLOSION} as stub (or GENERIC if needed).
     */
    public static DamageSource explosion(World world, @Nullable Entity attacker) {
        return create(world, DamageTypes.EXPLOSION, null, attacker);
    }

    public static DamageSource explosion(World world) {
        return explosion(world, null);
    }

    /** Radiation damage, the {@code ic2:radiation} type. */
    public static DamageSource radiation(World world) {
        return create(world, RADIATION, null, null);
    }

    /** Electricity, the {@code ic2:electricity} type: the tesla coil and live cables. */
    public static final RegistryKey<DamageType> ELECTRICITY =
            RegistryKey.of(RegistryKeys.DAMAGE_TYPE, net.minecraft.util.Identifier.of("ic2", "electricity"));

    public static DamageSource getElectricity(World world) {
        return create(world, ELECTRICITY, null, null);
    }

    /** The nuke's own damage type, {@code ic2:nuke}: "was nuked" rather than "blew up". */
    public static final RegistryKey<DamageType> NUKE =
            RegistryKey.of(RegistryKeys.DAMAGE_TYPE, net.minecraft.util.Identifier.of("ic2", "nuke"));

    /** Damage from a nuclear explosion, caused by {@code igniter} when someone set it off. */
    public static DamageSource nuke(World world, @Nullable Entity source, @Nullable Entity igniter) {
        return create(world, NUKE, source, igniter);
    }

    /** Legacy helper: nuke damage (stub -> EXPLOSION). */
    public static DamageSource getNukeSource(World world) {
        return create(world, DamageTypes.EXPLOSION, null, null);
    }

    public static DamageSource getNukeSource(World world, @Nullable Entity causingEntity) {
        return create(world, DamageTypes.EXPLOSION, causingEntity, causingEntity);
    }

    /** Preferred 1.21.1 path via DamageSources registry (world.getDamageSources()). */
    public static DamageSource viaDamageSources(World world) {
        if (world == null) return null;
        // world.getDamageSources().explosion(null) would need an Explosion instance;
        // use generic create path instead for stub
        return world.getDamageSources().create(DamageTypes.GENERIC);
    }

    private static DamageSource create(World world, RegistryKey<DamageType> key, @Nullable Entity source, @Nullable Entity attacker) {
        if (world == null) return null;
        try {
            var registry = world.getRegistryManager().get(RegistryKeys.DAMAGE_TYPE);
            RegistryEntry<DamageType> entry = registry.entryOf(key);
            if (source == null && attacker == null) {
                return new IC2DamageSource(entry);
            }
            return new IC2DamageSource(entry, source, attacker);
        } catch (Exception e) {
            // Fallback via DamageSources helper
            try {
                return world.getDamageSources().create(key, attacker, source);
            } catch (Exception ex) {
                return null;
            }
        }
    }

    // Convenience no-arg fallback (uses GENERIC) for legacy tests
    public static DamageSource generic(World world) {
        return create(world, DamageTypes.GENERIC, null, null);
    }
}
