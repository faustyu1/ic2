package ic2.api.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

/**
 * Fabric port of {@code ic2.api.event.LaserEvent}: what a mining laser shot is about to do.
 *
 * <p>The fields are mutable, as in the original, and the shot takes them back after the event;
 * a listener cancels the step by returning {@code false} from {@link Listener#allow}. Every
 * subclass has its own {@code EVENT}, which is how the Forge bus told them apart.
 */
public class LaserEvent {
    public final World world;
    public final Entity lasershot;
    public LivingEntity owner;
    public float range;
    public float power;
    public int blockBreaks;
    public boolean explosive;
    public boolean smelt;

    public LaserEvent(World world, Entity lasershot, LivingEntity owner, float range, float power, int blockBreaks, boolean explosive, boolean smelt) {
        this.world = world;
        this.lasershot = lasershot;
        this.owner = owner;
        this.range = range;
        this.power = power;
        this.blockBreaks = blockBreaks;
        this.explosive = explosive;
        this.smelt = smelt;
    }

    @FunctionalInterface
    public interface Listener<T extends LaserEvent> {
        /** @return false to cancel */
        boolean allow(T event);
    }

    @SuppressWarnings("unchecked")
    private static <T extends LaserEvent> Event<Listener<T>> create() {
        return EventFactory.createArrayBacked((Class<Listener<T>>) (Class<?>) Listener.class, listeners -> event -> {
            for (Listener<T> listener : listeners) {
                if (!listener.allow(event)) {
                    return false;
                }
            }
            return true;
        });
    }

    /** A shot leaves the laser; cancelling it means no shot, though the energy is spent. */
    public static class LaserShootEvent extends LaserEvent {
        public static final Event<Listener<LaserShootEvent>> EVENT = create();

        public final ItemStack laserItem;

        public LaserShootEvent(World world, Entity lasershot, LivingEntity owner, float range, float power, int blockBreaks,
                               boolean explosive, boolean smelt, ItemStack laserItem) {
            super(world, lasershot, owner, range, power, blockBreaks, explosive, smelt);
            this.laserItem = laserItem;
        }
    }

    /** An explosive shot is about to blow up. */
    public static class LaserExplodesEvent extends LaserEvent {
        public static final Event<Listener<LaserExplodesEvent>> EVENT = create();

        public float explosionPower;
        public float explosionDropRate;
        public float explosionEntityDamage;

        public LaserExplodesEvent(World world, Entity lasershot, LivingEntity owner, float range, float power, int blockBreaks,
                                  boolean explosive, boolean smelt, float explosionPower, float explosionDropRate,
                                  float explosionEntityDamage) {
            super(world, lasershot, owner, range, power, blockBreaks, explosive, smelt);
            this.explosionPower = explosionPower;
            this.explosionDropRate = explosionDropRate;
            this.explosionEntityDamage = explosionEntityDamage;
        }
    }

    /** A shot reaches a block; cancelling it stops the shot there. */
    public static class LaserHitsBlockEvent extends LaserEvent {
        public static final Event<Listener<LaserHitsBlockEvent>> EVENT = create();

        public BlockPos pos;
        public final Direction side;
        public boolean removeBlock;
        public boolean dropBlock;
        public float dropChance;

        public LaserHitsBlockEvent(World world, Entity lasershot, LivingEntity owner, float range, float power, int blockBreaks,
                                   boolean explosive, boolean smelt, BlockPos pos, Direction side, float dropChance,
                                   boolean removeBlock, boolean dropBlock) {
            super(world, lasershot, owner, range, power, blockBreaks, explosive, smelt);
            this.pos = pos;
            this.side = side;
            this.removeBlock = removeBlock;
            this.dropBlock = dropBlock;
            this.dropChance = dropChance;
        }
    }

    /**
     * A shot reaches an entity; cancelling it stops the shot, unless {@link #passThrough} is set,
     * in which case it flies on as if the entity were not there.
     */
    public static class LaserHitsEntityEvent extends LaserEvent {
        public static final Event<Listener<LaserHitsEntityEvent>> EVENT = create();

        public Entity hitEntity;
        public boolean passThrough;

        public LaserHitsEntityEvent(World world, Entity lasershot, LivingEntity owner, float range, float power, int blockBreaks,
                                    boolean explosive, boolean smelt, Entity hitEntity) {
            super(world, lasershot, owner, range, power, blockBreaks, explosive, smelt);
            this.hitEntity = hitEntity;
        }
    }
}
