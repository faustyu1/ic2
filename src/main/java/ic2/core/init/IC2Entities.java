package ic2.core.init;

import ic2.core.block.EntityDynamite;
import ic2.core.block.EntityItnt;
import ic2.core.block.EntityNuke;
import ic2.core.block.EntityStickyDynamite;
import ic2.core.item.tool.EntityMiningLaser;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/**
 * The IC2 entities, registered under the names {@code IC2.registerEntities} gave them in 1.12.2,
 * with the same tracking range (160 blocks, 10 chunks) and update interval (5 ticks).
 *
 * <p>The boats keep the vanilla boat's size and tracking. The beam particles are not here yet.
 */
public final class IC2Entities {
    private IC2Entities() {}

    public static EntityType<EntityMiningLaser> MINING_LASER;
    public static EntityType<EntityDynamite> DYNAMITE;
    public static EntityType<EntityStickyDynamite> STICKY_DYNAMITE;
    public static EntityType<EntityItnt> ITNT;
    public static EntityType<EntityNuke> NUKE;
    public static EntityType<ic2.core.entity.EntityBoatCarbon> CARBON_BOAT;
    public static EntityType<ic2.core.entity.EntityBoatRubber> RUBBER_BOAT;
    public static EntityType<ic2.core.entity.EntityBoatElectric> ELECTRIC_BOAT;

    public static void registerAll() {
        MINING_LASER = register("mining_laser", EntityType.Builder.<EntityMiningLaser>create(EntityMiningLaser::new, SpawnGroup.MISC)
                .dimensions(0.8F, 0.8F).maxTrackingRange(10).trackingTickInterval(5).disableSaving().makeFireImmune());
        DYNAMITE = register("dynamite", EntityType.Builder.<EntityDynamite>create(EntityDynamite::new, SpawnGroup.MISC)
                .dimensions(0.5F, 0.5F).maxTrackingRange(10).trackingTickInterval(5));
        STICKY_DYNAMITE = register("sticky_dynamite", EntityType.Builder.<EntityStickyDynamite>create(EntityStickyDynamite::new, SpawnGroup.MISC)
                .dimensions(0.5F, 0.5F).maxTrackingRange(10).trackingTickInterval(5));
        ITNT = register("itnt", EntityType.Builder.<EntityItnt>create(EntityItnt::new, SpawnGroup.MISC)
                .dimensions(0.98F, 0.98F).maxTrackingRange(10).trackingTickInterval(5).makeFireImmune());
        NUKE = register("nuke", EntityType.Builder.<EntityNuke>create(EntityNuke::new, SpawnGroup.MISC)
                .dimensions(0.98F, 0.98F).maxTrackingRange(10).trackingTickInterval(5).makeFireImmune());
        CARBON_BOAT = register("carbon_boat", EntityType.Builder.<ic2.core.entity.EntityBoatCarbon>create(ic2.core.entity.EntityBoatCarbon::new, SpawnGroup.MISC)
                .dimensions(1.375F, 0.5625F).eyeHeight(0.5625F).maxTrackingRange(10));
        RUBBER_BOAT = register("rubber_boat", EntityType.Builder.<ic2.core.entity.EntityBoatRubber>create(ic2.core.entity.EntityBoatRubber::new, SpawnGroup.MISC)
                .dimensions(1.375F, 0.5625F).eyeHeight(0.5625F).maxTrackingRange(10));
        ELECTRIC_BOAT = register("electric_boat", EntityType.Builder.<ic2.core.entity.EntityBoatElectric>create(ic2.core.entity.EntityBoatElectric::new, SpawnGroup.MISC)
                .dimensions(1.375F, 0.5625F).eyeHeight(0.5625F).maxTrackingRange(10).makeFireImmune());
    }

    private static <T extends net.minecraft.entity.Entity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
        Identifier id = Identifier.of("ic2", name);
        return Registry.register(Registries.ENTITY_TYPE, id, builder.build(id.toString()));
    }
}
