package ic2.core.block;

import ic2.core.init.IC2Entities;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;

/** Fabric port of {@code ic2.core.block.EntityStickyDynamite}: dynamite that stays where it lands. */
public class EntityStickyDynamite extends EntityDynamite {
    public EntityStickyDynamite(EntityType<? extends EntityStickyDynamite> type, World world) {
        super(type, world);
        sticky = true;
    }

    public EntityStickyDynamite(World world, LivingEntity owner) {
        this(IC2Entities.STICKY_DYNAMITE, world);
        initThrown(owner);
    }

    public EntityStickyDynamite(World world, double x, double y, double z) {
        this(IC2Entities.STICKY_DYNAMITE, world);
        setPosition(x, y, z);
    }
}
