package ic2.core.block;

import ic2.core.ExplosionIC2;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Fabric port of {@code ic2.core.block.EntityIC2Explosive}: a lit IC2 explosive, the industrial
 * TNT or the nuke, falling and bouncing like primed TNT until its fuse runs out and it goes off
 * as an {@link ExplosionIC2} - nuclear when it carries a radiation range.
 *
 * <p>The fuse and the block it looks like are tracked, so the client draws the right block and
 * flashes it as vanilla TNT does.
 */
public class EntityIC2Explosive extends Entity {
    private static final TrackedData<Integer> FUSE = DataTracker.registerData(EntityIC2Explosive.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<BlockState> BLOCK_STATE = DataTracker.registerData(EntityIC2Explosive.class, TrackedDataHandlerRegistry.BLOCK_STATE);

    @Nullable public LivingEntity igniter;
    public float explosivePower = 4.0F;
    public int radiationRange = 0;
    public float dropRate = 0.3F;
    public float damageVsEntitys = 1.0F;

    public EntityIC2Explosive(EntityType<? extends EntityIC2Explosive> type, World world) {
        super(type, world);
        intersectionChecked = true;
    }

    protected EntityIC2Explosive(EntityType<? extends EntityIC2Explosive> type, World world, double x, double y, double z,
                                 int fuse, float power, float dropRate, float damage, BlockState renderBlockState, int radiationRange) {
        this(type, world);
        setPosition(x, y, z);
        // the original fed radians into a sine that wanted degrees, so the push is tiny
        float f = (float) (Math.random() * Math.PI * 2.0);
        setVelocity(-MathHelper.sin(f * (float) Math.PI / 180.0F) * 0.02F, 0.2F, -MathHelper.cos(f * (float) Math.PI / 180.0F) * 0.02F);
        prevX = x;
        prevY = y;
        prevZ = z;
        setFuse(fuse);
        this.explosivePower = power;
        this.radiationRange = radiationRange;
        this.dropRate = dropRate;
        this.damageVsEntitys = damage;
        setRenderBlockState(renderBlockState);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(FUSE, 80);
        builder.add(BLOCK_STATE, Blocks.DIRT.getDefaultState());
    }

    public int getFuse() {
        return dataTracker.get(FUSE);
    }

    public void setFuse(int fuse) {
        dataTracker.set(FUSE, fuse);
    }

    public BlockState getRenderBlockState() {
        return dataTracker.get(BLOCK_STATE);
    }

    public void setRenderBlockState(BlockState state) {
        dataTracker.set(BLOCK_STATE, state);
    }

    @Override
    protected MoveEffect getMoveEffect() {
        return MoveEffect.NONE;
    }

    @Override
    public boolean canHit() {
        return !isRemoved();
    }

    @Override
    public void tick() {
        prevX = getX();
        prevY = getY();
        prevZ = getZ();
        setVelocity(getVelocity().add(0.0, -0.04, 0.0));
        move(MovementType.SELF, getVelocity());
        setVelocity(getVelocity().multiply(0.98));
        if (isOnGround()) {
            setVelocity(getVelocity().multiply(0.7, -0.5, 0.7));
        }

        int fuse = getFuse() - 1;
        setFuse(fuse);
        if (fuse + 1 <= 0) {
            discard();
            if (!getWorld().isClient) {
                explode();
            }
        } else {
            getWorld().addParticle(ParticleTypes.SMOKE, getX(), getY() + 0.5, getZ(), 0.0, 0.0, 0.0);
        }
    }

    private void explode() {
        new ExplosionIC2(getWorld(), this, getX(), getY(), getZ(), explosivePower, dropRate,
                radiationRange > 0 ? ExplosionIC2.Type.Nuclear : ExplosionIC2.Type.Normal, igniter, radiationRange)
                .doExplosion();
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putByte("Fuse", (byte) getFuse());
        nbt.putFloat("Power", explosivePower);
        nbt.putInt("RadiationRange", radiationRange);
        nbt.putFloat("DropRate", dropRate);
        nbt.put("BlockState", NbtHelper.fromBlockState(getRenderBlockState()));
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        setFuse(nbt.getByte("Fuse"));
        // 1.12.2 kept only the fuse; the rest came back as the defaults of the kind
        if (nbt.contains("Power")) explosivePower = nbt.getFloat("Power");
        if (nbt.contains("RadiationRange")) radiationRange = nbt.getInt("RadiationRange");
        if (nbt.contains("DropRate")) dropRate = nbt.getFloat("DropRate");
        if (nbt.contains("BlockState")) {
            setRenderBlockState(NbtHelper.toBlockState(getWorld().createCommandRegistryWrapper(RegistryKeys.BLOCK), nbt.getCompound("BlockState")));
        }
    }

    public EntityIC2Explosive setIgniter(@Nullable LivingEntity igniter) {
        this.igniter = igniter;
        return this;
    }
}
