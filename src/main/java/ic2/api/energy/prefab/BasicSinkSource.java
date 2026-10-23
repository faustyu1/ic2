package ic2.api.energy.prefab;

import ic2.api.energy.EnergyNet;
import ic2.api.energy.tile.IEnergySink;
import ic2.api.energy.tile.IEnergySource;
import ic2.api.info.ILocatable;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public abstract class BasicSinkSource extends BasicEnergyTile implements IEnergySink, IEnergySource {
    protected int sinkTier;
    protected int sourceTier;

    public BasicSinkSource(BlockEntity parent, double capacity, int sinkTier, int sourceTier) {
        super(parent, capacity);
        if (sinkTier < 0) {
            throw new IllegalArgumentException("invalid sink tier: " + sinkTier);
        } else if (sourceTier < 0) {
            throw new IllegalArgumentException("invalid source tier: " + sourceTier);
        } else {
            this.sinkTier = sinkTier;
            this.sourceTier = sourceTier;
            double power = EnergyNet.instance.getPowerFromTier(sourceTier);
            if (getCapacity() < power) {
                setCapacity(power);
            }
        }
    }

    public BasicSinkSource(ILocatable parent, double capacity, int sinkTier, int sourceTier) {
        super(parent, capacity);
        if (sinkTier < 0) {
            throw new IllegalArgumentException("invalid sink tier: " + sinkTier);
        } else if (sourceTier < 0) {
            throw new IllegalArgumentException("invalid source tier: " + sourceTier);
        } else {
            this.sinkTier = sinkTier;
            this.sourceTier = sourceTier;
            double power = EnergyNet.instance.getPowerFromTier(sourceTier);
            if (getCapacity() < power) {
                setCapacity(power);
            }
        }
    }

    public BasicSinkSource(World world, BlockPos pos, double capacity, int sinkTier, int sourceTier) {
        super(world, pos, capacity);
        if (sinkTier < 0) {
            throw new IllegalArgumentException("invalid sink tier: " + sinkTier);
        } else if (sourceTier < 0) {
            throw new IllegalArgumentException("invalid source tier: " + sourceTier);
        } else {
            this.sinkTier = sinkTier;
            this.sourceTier = sourceTier;
            double power = EnergyNet.instance.getPowerFromTier(sourceTier);
            if (getCapacity() < power) {
                setCapacity(power);
            }
        }
    }

    public void setSinkTier(int tier) {
        if (tier < 0) {
            throw new IllegalArgumentException("invalid tier: " + tier);
        } else {
            this.sinkTier = tier;
        }
    }

    public void setSourceTier(int tier) {
        if (tier < 0) {
            throw new IllegalArgumentException("invalid tier: " + tier);
        } else {
            double power = EnergyNet.instance.getPowerFromTier(tier);
            if (getCapacity() < power) {
                setCapacity(power);
            }

            this.sourceTier = tier;
        }
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
        return sinkTier;
    }

    @Override
    public double getOfferedEnergy() {
        return getEnergyStored();
    }

    @Override
    public void drawEnergy(double amount) {
        setEnergyStored(getEnergyStored() - amount);
    }

    @Override
    public int getSourceTier() {
        return sourceTier;
    }

    @Override
    protected String getNbtTagName() {
        return "IC2BasicSinkSource";
    }
}
