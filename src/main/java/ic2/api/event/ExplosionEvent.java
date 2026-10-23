package ic2.api.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Fabric port of {@code ic2.api.event.ExplosionEvent}: fired before an IC2 explosion (the
 * IC2 explosion, the dynamite's point explosion) touches the world.
 *
 * <p>The Forge event was cancelable; here a listener cancels it by returning {@code false} from
 * {@link Listener#allow}.
 */
public class ExplosionEvent {
    public static final Event<Listener> EVENT = EventFactory.createArrayBacked(Listener.class, listeners -> event -> {
        for (Listener listener : listeners) {
            if (!listener.allow(event)) {
                return false;
            }
        }
        return true;
    });

    public final World world;
    public final Entity entity;
    public final Vec3d pos;
    public final double power;
    public final LivingEntity igniter;
    public final int radiationRange;
    public final double rangeLimit;

    public ExplosionEvent(World world, Entity entity, Vec3d pos, double power, LivingEntity igniter, int radiationRange, double rangeLimit) {
        this.world = world;
        this.entity = entity;
        this.pos = pos;
        this.power = power;
        this.igniter = igniter;
        this.radiationRange = radiationRange;
        this.rangeLimit = rangeLimit;
    }

    /** @return whether the explosion goes ahead */
    public static boolean post(ExplosionEvent event) {
        return EVENT.invoker().allow(event);
    }

    @FunctionalInterface
    public interface Listener {
        /** @return false to cancel the explosion */
        boolean allow(ExplosionEvent event);
    }
}
