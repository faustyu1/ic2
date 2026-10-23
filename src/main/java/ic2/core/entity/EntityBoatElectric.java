package ic2.core.entity;

import ic2.api.item.ElectricItem;
import ic2.core.IC2;
import ic2.core.init.IC2ItemVariants;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.world.World;

/**
 * Fabric port of EntityBoatElectric - the electric boat.
 *
 * <p>It does not burn and floats on lava as well as on water, keeping its passengers from
 * burning too. Paddled by hand it speeds up at a quarter of a normal boat's rate; while its
 * driver holds the forward key and wears armor with at least 4 EU in it (a battery pack, say),
 * it takes those 4 EU every tick and speeds up at one and a half times the normal rate.
 */
public class EntityBoatElectric extends EntityIC2Boat {
    private static final double EU_CONSUME = 4.0;

    private boolean accelerated = false;

    public EntityBoatElectric(EntityType<? extends BoatEntity> type, World world) {
        super(type, world);
    }

    @Override
    protected ItemStack getItem() {
        return new ItemStack(IC2ItemVariants.get("boat", "electric"));
    }

    @Override
    public double getAccelerationFactor() {
        return accelerated ? 1.5 : 0.25;
    }

    public boolean isAccelerated() {
        return accelerated;
    }

    @Override
    public boolean isWater(FluidState fluid) {
        return fluid.isIn(FluidTags.WATER) || fluid.isIn(FluidTags.LAVA);
    }

    @Override
    public void tick() {
        extinguish();
        for (Entity passenger : getPassengerList()) {
            passenger.extinguish();
        }

        accelerated = false;
        if (getControllingPassenger() instanceof PlayerEntity driver && IC2.keyboard.isForwardKeyDown(driver)) {
            for (ItemStack stack : driver.getInventory().armor) {
                if (!stack.isEmpty() && ElectricItem.manager.discharge(stack, EU_CONSUME, Integer.MAX_VALUE, true, true, true) == EU_CONSUME) {
                    ElectricItem.manager.discharge(stack, EU_CONSUME, Integer.MAX_VALUE, true, true, false);
                    accelerated = true;
                    break;
                }
            }
        }

        super.tick();
    }
}
