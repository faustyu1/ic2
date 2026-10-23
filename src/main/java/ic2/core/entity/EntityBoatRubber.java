package ic2.core.entity;

import ic2.core.init.IC2ItemVariants;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;

/**
 * Fabric port of EntityBoatRubber - the rubber dinghy. Hitting the ground from too high it
 * bursts with a pop into a damaged dinghy, which has to be repaired before it floats again.
 */
public class EntityBoatRubber extends EntityIC2Boat {

    public EntityBoatRubber(EntityType<? extends BoatEntity> type, World world) {
        super(type, world);
    }

    @Override
    protected ItemStack getItem() {
        return new ItemStack(IC2ItemVariants.get("boat", "rubber"));
    }

    @Override
    protected ItemStack getBrokenItem() {
        playSound(SoundEvents.ENTITY_ITEM_PICKUP, 16.0F, 8.0F);
        return new ItemStack(IC2ItemVariants.get("boat", "broken_rubber"));
    }
}
