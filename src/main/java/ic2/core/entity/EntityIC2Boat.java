package ic2.core.entity;

import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;

/**
 * Fabric port of EntityIC2Boat - the base of the IC2 boats.
 *
 * <p>They move like a vanilla boat, but break into their own item instead of planks and sticks
 * - when hit, and when they hit the ground from more than three blocks. 1.12.2 copied the
 * vanilla boat's update through reflection to change how fast a boat speeds up and what it
 * floats on; the port changes those two things in the vanilla boat itself
 * ({@code ic2.core.mixin.BoatEntityMixin}) through {@link #getAccelerationFactor()} and
 * {@link #isWater(FluidState)}.
 */
public abstract class EntityIC2Boat extends BoatEntity {

    protected EntityIC2Boat(EntityType<? extends BoatEntity> type, World world) {
        super(type, world);
    }

    /** The item the boat is, which it drops when broken. */
    protected abstract ItemStack getItem();

    /** What the boat leaves when it breaks on hitting the ground. */
    protected ItemStack getBrokenItem() {
        return getItem();
    }

    /** How strongly paddling speeds the boat up, 1 for a vanilla boat. */
    public double getAccelerationFactor() {
        return 1.0;
    }

    /** What the boat floats on. */
    public boolean isWater(FluidState fluid) {
        return fluid.isIn(FluidTags.WATER);
    }

    @Override
    public Item asItem() {
        return getItem().getItem();
    }

    @Override
    public ItemStack getPickBlockStack() {
        return getItem();
    }

    /** Puts the boat at a position, as the boat items place it. */
    public void place(double x, double y, double z, float yaw) {
        refreshPositionAndAngles(x, y, z, yaw, 0.0F);
        prevX = x;
        prevY = y;
        prevZ = z;
    }

    @Override
    protected void fall(double heightDifference, boolean onGround, BlockState state, BlockPos landedPosition) {
        boolean expectDeath = fallDistance > 3.0F && !isRemoved();
        super.fall(heightDifference, onGround, state, landedPosition);
        if (expectDeath && isRemoved() && !getWorld().isClient && getWorld().getGameRules().getBoolean(GameRules.DO_ENTITY_DROPS)) {
            super.dropStack(getBrokenItem(), 0.0F);
        }
    }

    /** The planks and sticks a vanilla boat breaks into are not dropped; the boat's own item is. */
    @Override
    public ItemEntity dropStack(ItemStack stack, float yOffset) {
        if (stack.isOf(Items.STICK) || stack.isOf(getVariant().getBaseBlock().asItem())) {
            return null;
        }
        return super.dropStack(stack, yOffset);
    }
}
