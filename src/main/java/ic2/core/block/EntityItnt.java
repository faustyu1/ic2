package ic2.core.block;

import ic2.core.init.IC2Blocks;
import ic2.core.init.IC2Entities;
import net.minecraft.entity.EntityType;
import net.minecraft.world.World;

/** Fabric port of {@code ic2.core.block.EntityItnt}: lit industrial TNT, power 5.5, 90% drops. */
public class EntityItnt extends EntityIC2Explosive {
    public EntityItnt(EntityType<? extends EntityItnt> type, World world) {
        super(type, world);
        // a summoned one has no block that made it
        setRenderBlockState(IC2Blocks.ITNT_BLOCK.getDefaultState());
        explosivePower = 5.5F;
        dropRate = 0.9F;
        damageVsEntitys = 0.3F;
    }

    public EntityItnt(World world, double x, double y, double z) {
        super(IC2Entities.ITNT, world, x, y, z, 60, 5.5F, 0.9F, 0.3F, IC2Blocks.ITNT_BLOCK.getDefaultState(), 0);
    }
}
