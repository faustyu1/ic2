package ic2.core.block.machine.tileentity;

import ic2.core.block.EntityIC2Explosive;
import ic2.core.block.TileEntityInventory;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import org.jetbrains.annotations.Nullable;

/**
 * Fabric port of {@code ic2.core.block.machine.tileentity.Explosive}: an IC2 explosive block,
 * which turns into its lit entity when it gets a redstone signal, is lit with flint and steel or
 * a fire charge, is hit by a burning arrow or is caught in another explosion - then with a short
 * fuse, between an eighth and three eighths of the usual.
 *
 * <p>The 1.12.2 Redstone component is not ported; the block looks at its signal itself when a
 * neighbour changes and when it is placed.
 */
public abstract class Explosive extends TileEntityInventory {
    private boolean exploded;

    protected Explosive(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void onNeighborUpdate() {
        if (world != null && world.isReceivingRedstonePower(pos)) {
            explode(null, false);
        }
    }

    @Override
    public void onPlaced(ItemStack stack, @Nullable LivingEntity placer) {
        super.onPlaced(stack, placer);
        onNeighborUpdate();
    }

    @Override
    public boolean onActivated(PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (stack.isOf(Items.FIRE_CHARGE)) {
            if (!player.getAbilities().creativeMode) {
                stack.decrement(1);
            }
        } else if (stack.isOf(Items.FLINT_AND_STEEL)) {
            stack.damage(1, player, hand == Hand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        } else {
            return super.onActivated(player, hand);
        }

        explode(player, false);
        return true;
    }

    @Override
    public boolean onExploded(Explosion explosion) {
        return explode(explosion.getCausingEntity(), true);
    }

    @Override
    public boolean onRemovedByPlayer(PlayerEntity player) {
        if (explodeOnRemoval()) {
            explode(player, false);
            return true;
        }
        return super.onRemovedByPlayer(player);
    }

    @Override
    public void onProjectileHit(ProjectileEntity projectile) {
        if (projectile instanceof PersistentProjectileEntity && projectile.isOnFire()) {
            Entity owner = projectile.getOwner();
            explode(owner instanceof LivingEntity living ? living : null, false);
        }
    }

    @Override
    protected ItemStack adjustDrop(ItemStack drop, boolean wrench) {
        return exploded ? null : super.adjustDrop(drop, wrench);
    }

    /**
     * Lights the explosive: takes the block away and puts its entity in its place.
     *
     * @return whether it went off; a nuke without TNT around it does not
     */
    protected boolean explode(@Nullable LivingEntity igniter, boolean shortFuse) {
        World world = getWorld();
        if (world == null || exploded) {
            return false;
        }

        EntityIC2Explosive entity = getEntity(igniter);
        if (entity == null) {
            return false;
        }
        if (world.isClient) {
            return true;
        }

        entity.setIgniter(igniter);
        onIgnite(igniter);
        exploded = true;
        world.removeBlock(pos, false);
        if (shortFuse) {
            int fuse = entity.getFuse();
            entity.setFuse(world.random.nextInt(Math.max(1, fuse / 4)) + fuse / 8);
        }

        world.spawnEntity(entity);
        world.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.ENTITY_TNT_PRIMED, SoundCategory.BLOCKS, 1.0F, 1.0F);
        return true;
    }

    protected boolean explodeOnRemoval() {
        return false;
    }

    /** The lit entity, or null when the explosive cannot go off. */
    protected abstract @Nullable EntityIC2Explosive getEntity(@Nullable LivingEntity igniter);

    protected void onIgnite(@Nullable LivingEntity igniter) {
    }
}
