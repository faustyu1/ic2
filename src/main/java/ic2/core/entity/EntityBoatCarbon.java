package ic2.core.entity;

import ic2.core.init.IC2ItemVariants;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/** Fabric port of EntityBoatCarbon - the carbon fiber canoe, a boat that survives a fall as itself. */
public class EntityBoatCarbon extends EntityIC2Boat {

    public EntityBoatCarbon(EntityType<? extends BoatEntity> type, World world) {
        super(type, world);
    }

    @Override
    protected ItemStack getItem() {
        return new ItemStack(IC2ItemVariants.get("boat", "carbon"));
    }
}
