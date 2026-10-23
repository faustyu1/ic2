package ic2.api.energy.prefab;

import ic2.api.energy.tile.IEnergyEmitter;
import ic2.api.energy.tile.IEnergySink;
import ic2.api.info.ILocatable;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class BasicSink extends BasicEnergyTile implements IEnergySink {
    protected int tier;

    public BasicSink(BlockEntity parent, double capacity, int tier) {
        super(parent, capacity);
        if (tier < 0) {
            throw new IllegalArgumentException("invalid tier: " + tier);
        } else {
            this.tier = tier;
        }
    }

    public BasicSink(ILocatable parent, double capacity, int tier) {
        super(parent, capacity);
        if (tier < 0) {
            throw new IllegalArgumentException("invalid tier: " + tier);
        } else {
            this.tier = tier;
        }
    }

    public BasicSink(World world, BlockPos pos, double capacity, int tier) {
        super(world, pos, capacity);
        if (tier < 0) {
            throw new IllegalArgumentException("invalid tier: " + tier);
        } else {
            this.tier = tier;
        }
    }

    public void setSinkTier(int tier) {
        if (tier < 0) {
            throw new IllegalArgumentException("invalid tier: " + tier);
        } else {
            this.tier = tier;
        }
    }

    @Override
    public boolean acceptsEnergyFrom(IEnergyEmitter emitter, Direction direction) {
        return true;
    }

    @Override
    public double getDemandedEnergy() {
        return Math.max(0.0, getCapacity() - getEnergyStored());
    }

    @Override
    public double injectEnergy(Direction directionFrom, double amount, double voltage) {
        setEnergyStored(getEnergyStored() + amount);
        return 0.0;
    }

    @Override
    public int getSinkTier() {
        return tier;
    }

    @Override
    protected String getNbtTagName() {
        return "IC2BasicSink";
    }
}
