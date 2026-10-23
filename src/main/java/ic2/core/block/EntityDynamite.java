package ic2.core.block;

import ic2.core.PointExplosion;
import ic2.core.init.IC2Entities;
import ic2.core.init.IC2Items;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.FlyingItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Fabric port of {@code ic2.core.block.EntityDynamite}: a thrown stick of dynamite.
 *
 * <p>It flies like a heavy snowball, bounces off what it hits and goes off after five seconds as a
 * {@link PointExplosion} - the 3x3x3 cube around it, 20 damage to anything within two blocks.
 * Water all but puts it out (the fuse grows by 2000 ticks a tick). The sticky kind stays where it
 * first lands and burns four times as fast from then on, until the block it stuck to is gone.
 */
public class EntityDynamite extends Entity implements FlyingItemEntity {
    public boolean sticky = false;
    @Nullable public BlockPos stickPos;
    public int fuse = 100;
    private boolean inGround = false;
    @Nullable public LivingEntity owner;
    private int ticksInGround;

    public EntityDynamite(EntityType<? extends EntityDynamite> type, World world) {
        super(type, world);
    }

    public EntityDynamite(World world, double x, double y, double z) {
        this(IC2Entities.DYNAMITE, world);
        setPosition(x, y, z);
    }

    /** Thrown by {@code owner}, from the eyes a little to the right, the way it is looking. */
    public EntityDynamite(World world, LivingEntity owner) {
        this(IC2Entities.DYNAMITE, world);
        initThrown(owner);
    }

    protected final void initThrown(LivingEntity owner) {
        this.owner = owner;
        Vec3d eye = owner.getEyePos();
        float yaw = owner.getYaw();
        float pitch = owner.getPitch();
        refreshPositionAndAngles(eye.x, eye.y, eye.z, yaw, pitch);
        setPosition(getX() - Math.cos(Math.toRadians(yaw)) * 0.16, getY() - 0.1, getZ() - Math.sin(Math.toRadians(yaw)) * 0.16);
        double motionX = -Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch));
        double motionZ = Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch));
        double motionY = -Math.sin(Math.toRadians(pitch));
        setVelocity(motionX, motionY, motionZ, 1.0F, 1.0F);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
    }

    @Override
    public ItemStack getStack() {
        return new ItemStack(sticky ? IC2Items.DYNAMITE_STICKY : IC2Items.DYNAMITE);
    }

    /** Aims it along the direction, with a spread of {@code inaccuracy}, at {@code velocity} blocks a tick. */
    public void setVelocity(double x, double y, double z, float velocity, float inaccuracy) {
        double len = Math.sqrt(x * x + y * y + z * z);
        x /= len;
        y /= len;
        z /= len;
        x += random.nextGaussian() * 0.0075 * inaccuracy;
        y += random.nextGaussian() * 0.0075 * inaccuracy;
        z += random.nextGaussian() * 0.0075 * inaccuracy;
        x *= velocity;
        y *= velocity;
        z *= velocity;
        setVelocity(x, y, z);
        double hLen = Math.sqrt(x * x + z * z);
        setYaw((float) (Math.atan2(x, z) * 180.0 / Math.PI));
        setPitch((float) (Math.atan2(y, hLen) * 180.0 / Math.PI));
        prevYaw = getYaw();
        prevPitch = getPitch();
        ticksInGround = 0;
    }

    @Override
    public void setVelocityClient(double x, double y, double z) {
        setVelocity(x, y, z);
        if (prevPitch == 0.0F && prevYaw == 0.0F) {
            double h = Math.sqrt(x * x + z * z);
            setYaw((float) (Math.atan2(x, z) * 180.0 / Math.PI));
            setPitch((float) (Math.atan2(y, h) * 180.0 / Math.PI));
            prevPitch = getPitch();
            prevYaw = getYaw();
            ticksInGround = 0;
        }
    }

    @Override
    public void tick() {
        super.tick();
        Vec3d motion = getVelocity();
        if (prevPitch == 0.0F && prevYaw == 0.0F) {
            double hLen = motion.horizontalLength();
            setYaw((float) Math.toDegrees(Math.atan2(motion.x, motion.z)));
            setPitch((float) Math.toDegrees(Math.atan2(motion.y, hLen)));
            prevYaw = getYaw();
            prevPitch = getPitch();
        }

        World world = getWorld();
        if (fuse-- <= 0) {
            discard();
            if (!world.isClient) {
                explode();
            }
        } else if (fuse < 100 && fuse % 2 == 0) {
            world.addParticle(ParticleTypes.SMOKE, getX(), getY() + 0.5, getZ(), 0.0, 0.0, 0.0);
        }

        if (inGround) {
            ticksInGround++;
            if (ticksInGround >= 200) {
                discard();
            }

            if (sticky) {
                fuse -= 3;
                setVelocity(Vec3d.ZERO);
                if (stickPos != null && !world.isAir(stickPos)) {
                    return;
                }
            }
        }

        motion = getVelocity();
        double x = getX();
        double y = getY();
        double z = getZ();
        Vec3d start = getPos();
        Vec3d end = start.add(motion);
        BlockHitResult result = world.raycast(new RaycastContext(start, end, RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE, this));
        if (result.getType() != HitResult.Type.MISS) {
            float remainX = (float) (result.getPos().x - x);
            float remainY = (float) (result.getPos().y - y);
            float remainZ = (float) (result.getPos().z - z);
            double remDist = Math.sqrt(remainX * remainX + remainY * remainY + remainZ * remainZ);
            stickPos = result.getBlockPos();
            if (remDist > 0.0) {
                x -= remainX / remDist * 0.05;
                y -= remainY / remDist * 0.05;
                z -= remainZ / remDist * 0.05;
            }
            x += remainX;
            y += remainY;
            z += remainZ;
            motion = new Vec3d(motion.x * (0.75F - random.nextFloat()), motion.y * -0.3F, motion.z * (0.75F - random.nextFloat()));
            inGround = true;
        } else {
            x += motion.x;
            y += motion.y;
            z += motion.z;
            inGround = false;
        }

        double hMotion = motion.horizontalLength();
        float yaw = (float) Math.toDegrees(Math.atan2(motion.x, motion.z));
        float pitch = (float) Math.toDegrees(Math.atan2(motion.y, hMotion));
        while (pitch - prevPitch < -180.0F) prevPitch -= 360.0F;
        while (pitch - prevPitch >= 180.0F) prevPitch += 360.0F;
        while (yaw - prevYaw < -180.0F) prevYaw -= 360.0F;
        while (yaw - prevYaw >= 180.0F) prevYaw += 360.0F;
        setPitch(prevPitch + (pitch - prevPitch) * 0.2F);
        setYaw(prevYaw + (yaw - prevYaw) * 0.2F);

        float drag = 0.98F;
        float gravity = 0.04F;
        if (isTouchingWater()) {
            fuse += 2000;
            for (int i = 0; i < 4; i++) {
                float f = 0.25F;
                world.addParticle(ParticleTypes.BUBBLE, x - motion.x * f, y - motion.y * f, z - motion.z * f, motion.x, motion.y, motion.z);
            }
            drag = 0.75F;
        }

        setVelocity(motion.x * drag, motion.y * drag - gravity, motion.z * drag);
        setPosition(x, y, z);
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putByte("inGround", (byte) (inGround ? 1 : 0));
        nbt.putShort("Fuse", (short) fuse);
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        inGround = nbt.getByte("inGround") == 1;
        if (nbt.contains("Fuse")) fuse = nbt.getShort("Fuse");
    }

    public void explode() {
        new PointExplosion(getWorld(), this, owner, getX(), getY(), getZ(), 1.0F, 20).explode();
    }
}
