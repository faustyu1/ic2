package ic2.core.item.tool;

import ic2.api.event.LaserEvent;
import ic2.core.ExplosionIC2;
import ic2.core.Ic2Player;
import ic2.core.block.ITnt;
import ic2.core.init.IC2ClassicBlocks;
import ic2.core.init.IC2Entities;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.input.SingleStackRecipeInput;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;

/**
 * Fabric port of {@code ic2.core.item.tool.EntityMiningLaser}: one shot of the mining laser.
 *
 * <p>A shot flies a block a tick in a straight line, without gravity, and carries a range, a power
 * and a number of blocks it may break. Every block it passes costs its hardness / 1.5 of the
 * power - glass lets it through for free, unbreakable blocks stop it - and every tick in the air
 * costs half a point; it ends when one of the three runs out, or in water. An entity it hits takes
 * the power as damage, is set on fire for as many seconds (twice as long for the super-heat shot)
 * and stops the shot. A super-heat shot smelts what it breaks and burns wood away; an explosive
 * shot blows up on the first thing it hits. Breaking a block asks the protection hooks first, as
 * the owner or, without one, as IC2's fake player.
 *
 * <p>Each step goes through {@link LaserEvent}, so other mods can change or stop it.
 */
public class EntityMiningLaser extends Entity {
    public static final double LASER_SPEED = 1.0;

    public float range = 0.0F;
    public float power = 0.0F;
    public int blockBreaks = 0;
    public boolean explosive = false;
    public LivingEntity owner;
    public boolean headingSet = false;
    public boolean smelt = false;
    private int ticksInAir = 0;

    public EntityMiningLaser(EntityType<? extends EntityMiningLaser> type, World world) {
        super(type, world);
        noClip = true;
    }

    public EntityMiningLaser(World world, Vec3d start, Vec3d dir, LivingEntity owner, float range, float power, int blockBreaks, boolean explosive) {
        this(IC2Entities.MINING_LASER, world);
        this.owner = owner;
        setPosition(start.x, start.y, start.z);
        setLaserHeading(dir.x, dir.y, dir.z, LASER_SPEED);
        this.range = range;
        this.power = power;
        this.blockBreaks = blockBreaks;
        this.explosive = explosive;
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
    }

    public void setLaserHeading(double motionX, double motionY, double motionZ, double speed) {
        double currentSpeed = Math.sqrt(motionX * motionX + motionY * motionY + motionZ * motionZ);
        if (currentSpeed == 0.0) {
            // a summoned shot without motion stands still rather than flying off to NaN
            setVelocity(Vec3d.ZERO);
            return;
        }
        setVelocity(motionX / currentSpeed * speed, motionY / currentSpeed * speed, motionZ / currentSpeed * speed);
        float yaw = (float) Math.toDegrees(Math.atan2(motionX, motionZ));
        float pitch = (float) Math.toDegrees(Math.atan2(motionY, Math.sqrt(motionX * motionX + motionZ * motionZ)));
        setYaw(yaw);
        setPitch(pitch);
        prevYaw = yaw;
        prevPitch = pitch;
        headingSet = true;
    }

    @Override
    public void setVelocityClient(double x, double y, double z) {
        setLaserHeading(x, y, z, LASER_SPEED);
    }

    @Override
    public void tick() {
        super.tick();
        World world = getWorld();

        if (!world.isClient && (range < 1.0F || power <= 0.0F || blockBreaks <= 0)) {
            if (explosive) {
                explode();
            }
            discard();
            return;
        }

        ticksInAir++;
        Vec3d velocity = getVelocity();
        Vec3d oldPosition = getPos();
        Vec3d newPosition = oldPosition.add(velocity);
        BlockHitResult result = world.raycast(new RaycastContext(oldPosition, newPosition,
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, this));
        boolean blockHit = result.getType() != HitResult.Type.MISS;
        if (blockHit) {
            newPosition = result.getPos();
        }

        Entity hitEntity = null;
        double distance = 0.0;
        for (Entity entity : world.getOtherEntities(this, getBoundingBox().stretch(velocity).expand(1.0))) {
            if (entity.canHit() && (entity != owner || ticksInAir >= 5)) {
                Box hitBox = entity.getBoundingBox().expand(0.3);
                Optional<Vec3d> intercept = hitBox.raycast(oldPosition, newPosition);
                if (intercept.isPresent()) {
                    double newDistance = oldPosition.distanceTo(intercept.get());
                    if (newDistance < distance || distance == 0.0) {
                        hitEntity = entity;
                        distance = newDistance;
                    }
                }
            }
        }

        if ((hitEntity != null || blockHit) && !world.isClient) {
            if (explosive) {
                explode();
                discard();
                return;
            }

            boolean handled = false;
            if (hitEntity != null) {
                handled = hitEntity(hitEntity);
                if (!handled && !blockHit) {
                    power -= 0.5F;
                    handled = true;
                }
            }
            if (!handled && !hitBlock(result.getBlockPos(), result.getSide())) {
                power -= 0.5F;
            }
            if (isRemoved()) {
                return;
            }
        } else {
            power -= 0.5F;
        }

        setPosition(getX() + velocity.x, getY() + velocity.y, getZ() + velocity.z);
        range -= (float) velocity.length();
        if (isTouchingWater()) {
            discard();
        }
    }

    private void explode() {
        World world = getWorld();
        LaserEvent.LaserExplodesEvent event = new LaserEvent.LaserExplodesEvent(world, this, owner, range, power, blockBreaks,
                explosive, smelt, 5.0F, 0.85F, 0.55F);
        if (!LaserEvent.LaserExplodesEvent.EVENT.invoker().allow(event)) {
            discard();
            return;
        }

        copyDataFromEvent(event);
        new ExplosionIC2(world, this, getX(), getY(), getZ(), event.explosionPower, event.explosionDropRate).doExplosion();
    }

    /** @return whether the shot is done with this entity; false lets it fly on */
    private boolean hitEntity(Entity entity) {
        LaserEvent.LaserHitsEntityEvent event = new LaserEvent.LaserHitsEntityEvent(getWorld(), this, owner, range, power,
                blockBreaks, explosive, smelt, entity);
        if (!LaserEvent.LaserHitsEntityEvent.EVENT.invoker().allow(event)) {
            if (event.passThrough) {
                return false;
            }
            discard();
            return true;
        }

        copyDataFromEvent(event);
        entity = event.hitEntity;
        int damage = (int) power;
        if (damage > 0) {
            entity.setOnFireFor(damage * (smelt ? 2 : 1));
            entity.damage(getWorld().getDamageSources().create(DamageTypes.ARROW, this, owner), damage);
        }

        discard();
        return true;
    }

    /** @return whether the block took anything from the shot; false costs it half a point of power */
    private boolean hitBlock(BlockPos pos, Direction side) {
        World world = getWorld();
        LaserEvent.LaserHitsBlockEvent event = new LaserEvent.LaserHitsBlockEvent(world, this, owner, range, power, blockBreaks,
                explosive, smelt, pos, side, 0.9F, true, true);
        if (!LaserEvent.LaserHitsBlockEvent.EVENT.invoker().allow(event)) {
            discard();
            return true;
        }

        copyDataFromEvent(event);
        pos = event.pos;
        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();

        PlayerEntity playerOwner = owner instanceof PlayerEntity player ? player : Ic2Player.get(world);
        if (playerOwner != null && !PlayerBlockBreakEvents.BEFORE.invoker()
                .beforeBlockBreak(world, playerOwner, pos, state, world.getBlockEntity(pos))) {
            discard();
            return true;
        }

        if (state.isAir() || block == Blocks.GLASS || block == Blocks.GLASS_PANE || block == IC2ClassicBlocks.REINFORCED_GLASS) {
            return false;
        }

        float hardness = state.getHardness(world, pos);
        if (hardness < 0.0F) {
            discard();
            return true;
        }

        power -= hardness / 1.5F;
        if (power < 0.0F) {
            return true;
        }

        List<ItemStack> replacements = new ArrayList<>();
        boolean setOff = false;
        if (ITnt.isExplosive(state)) {
            // the explosive goes off as if caught in a blast; what is left of it does not drop
            block.onDestroyedByExplosion(world, pos,
                    new Explosion(world, this, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 1.0F, false,
                            Explosion.DestructionType.DESTROY));
            setOff = true;
        } else if (smelt) {
            if (isWood(state)) {
                event.dropBlock = false;
            } else if (world instanceof ServerWorld serverWorld) {
                for (ItemStack drop : Block.getDroppedStacks(state, serverWorld, pos, world.getBlockEntity(pos), this, ItemStack.EMPTY)) {
                    ItemStack smelted = smelt(serverWorld, drop);
                    if (!smelted.isEmpty()) {
                        replacements.add(smelted);
                    }
                }
                event.dropBlock = replacements.isEmpty();
            }
        }

        if (event.removeBlock) {
            if (event.dropBlock && !setOff && world instanceof ServerWorld serverWorld) {
                BlockEntity be = world.getBlockEntity(pos);
                for (ItemStack drop : Block.getDroppedStacks(state, serverWorld, pos, be, this, ItemStack.EMPTY)) {
                    if (world.random.nextFloat() <= event.dropChance) {
                        Block.dropStack(world, pos, drop);
                    }
                }
            }

            world.removeBlock(pos, false);

            for (ItemStack replacement : replacements) {
                if (!placeBlock(replacement, world, pos)) {
                    ItemScatterer.spawn(world, pos.getX(), pos.getY(), pos.getZ(), replacement);
                }
                power = 0.0F;
            }

            if (world.random.nextInt(10) == 0 && state.isBurnable()) {
                world.setBlockState(pos, Blocks.FIRE.getDefaultState());
            }
        }

        blockBreaks--;
        return true;
    }

    /** What 1.12.2 called {@code Material.WOOD}: the blocks the super-heat shot burns to nothing. */
    private static boolean isWood(BlockState state) {
        return state.isIn(BlockTags.LOGS) || state.isIn(BlockTags.PLANKS) || state.isIn(BlockTags.WOODEN_SLABS)
                || state.isIn(BlockTags.WOODEN_STAIRS) || state.isIn(BlockTags.WOODEN_FENCES)
                || state.isIn(BlockTags.WOODEN_DOORS) || state.isIn(BlockTags.WOODEN_TRAPDOORS);
    }

    /** The furnace result of a drop, the whole stack of it, or empty if it does not smelt. */
    private static ItemStack smelt(ServerWorld world, ItemStack drop) {
        return world.getRecipeManager().getFirstMatch(RecipeType.SMELTING, new SingleStackRecipeInput(drop), world)
                .map(recipe -> {
                    ItemStack result = recipe.value().getResult(world.getRegistryManager()).copy();
                    result.setCount(MathHelper.clamp(result.getCount() * drop.getCount(), 1, result.getMaxCount()));
                    return result;
                })
                .orElse(ItemStack.EMPTY);
    }

    /** Puts a smelted block back where the shot found it, as StackUtil.placeBlock did. */
    private static boolean placeBlock(ItemStack stack, World world, BlockPos pos) {
        if (!(stack.getItem() instanceof BlockItem blockItem) || !world.getBlockState(pos).isReplaceable()) {
            return false;
        }

        BlockState state = blockItem.getBlock().getDefaultState();
        if (!state.canPlaceAt(world, pos)) {
            return false;
        }
        if (!world.setBlockState(pos, state)) {
            return false;
        }
        stack.decrement(1);
        if (!stack.isEmpty()) {
            ItemScatterer.spawn(world, pos.getX(), pos.getY(), pos.getZ(), stack);
        }
        return true;
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
    }

    /** A shot is not worth keeping over a reload, as in 1.12.2. */
    @Override
    public boolean shouldSave() {
        return false;
    }

    void copyDataFromEvent(LaserEvent event) {
        owner = event.owner;
        range = event.range;
        power = event.power;
        blockBreaks = event.blockBreaks;
        explosive = event.explosive;
        smelt = event.smelt;
    }

    public Entity getThrower() {
        return owner;
    }

    public void setThrower(Entity entity) {
        if (entity instanceof LivingEntity living) {
            owner = living;
        }
    }
}
