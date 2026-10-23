package ic2.core.block.machine.tileentity;

import ic2.api.energy.tile.IEnergyEmitter;
import ic2.api.energy.tile.IEnergySink;
import ic2.api.item.IHazmatLike;
import ic2.core.IC2;
import ic2.core.IC2DamageSource;
import ic2.core.block.TileEntityBlock;
import ic2.core.init.IC2Blocks;
import ic2.core.ref.TeBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;

/**
 * Fabric port of TileEntityTesla - the tesla coil.
 *
 * <p>While it gets a redstone signal it uses 1 EU a tick, and every 32 ticks shocks one living
 * thing within 4 blocks without a full hazmat suit, 1 damage for every 400 EU it holds, paying
 * those 400 EU each. 10 000 EU, tier 2.
 */
public class TileEntityTesla extends TileEntityBlock implements IEnergySink {
    private static final double CAPACITY = 10000.0;

    private double energy;
    private int ticker = IC2.random.nextInt(32);

    public TileEntityTesla(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type == null ? IC2Blocks.TESLA_BE : type, pos, state, TeBlock.tesla_coil);
    }

    public TileEntityTesla(BlockPos pos, BlockState state) {
        this(null, pos, state);
    }

    @Override
    public void tick() {
        if (world == null || world.isClient || !world.isReceivingRedstonePower(pos)) {
            return;
        }
        if (energy >= 1.0) {
            energy -= 1.0;
            markDirty();
            if (++ticker % 32 == 0) {
                int damage = (int) energy / 400;
                if (damage > 0 && shock(damage)) {
                    energy -= damage * 400;
                }
            }
        }
    }

    protected boolean shock(int damage) {
        int r = 4;
        Box area = new Box(pos.getX() - r, pos.getY() - r, pos.getZ() - r, pos.getX() + r + 1, pos.getY() + r + 1, pos.getZ() + r + 1);
        for (LivingEntity entity : world.getEntitiesByClass(LivingEntity.class, area, e -> true)) {
            if (!IHazmatLike.hasCompleteHazmat(entity) && entity.damage(IC2DamageSource.getElectricity(world), damage)) {
                if (world instanceof ServerWorld serverWorld) {
                    for (int i = 0; i < damage; i++) {
                        serverWorld.spawnParticles(DustParticleEffect.DEFAULT,
                                entity.getX() + world.random.nextFloat() - 0.5,
                                entity.getY() + world.random.nextFloat() * 2.0F - 1.0,
                                entity.getZ() + world.random.nextFloat() - 0.5, 1, 0.1, 0.1, 1.0, 1.0);
                    }
                }
                return true;
            }
        }
        return false;
    }

    public double getEnergy() {
        return energy;
    }

    @Override
    public boolean acceptsEnergyFrom(IEnergyEmitter emitter, Direction side) {
        return true;
    }

    @Override
    public double getDemandedEnergy() {
        return Math.max(0.0, CAPACITY - energy);
    }

    @Override
    public double injectEnergy(Direction directionFrom, double amount, double voltage) {
        double accepted = Math.min(amount, getDemandedEnergy());
        energy += accepted;
        markDirty();
        return amount - accepted;
    }

    @Override
    public int getSinkTier() {
        return 2;
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.readNbt(nbt, lookup);
        energy = nbt.getDouble("energy");
    }

    @Override
    public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.writeNbt(nbt, lookup);
        nbt.putDouble("energy", energy);
    }
}
