package ic2.api.energy.prefab;

import ic2.api.energy.EnergyNet;
import ic2.api.energy.tile.IEnergyTile;
import ic2.api.info.ILocatable;
import ic2.api.info.Info;
import ic2.api.item.ElectricItem;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

abstract class BasicEnergyTile implements ILocatable, IEnergyTile {
    private final Object locationProvider;
    protected World world;
    protected BlockPos pos;
    protected double capacity;
    protected double energyStored;
    protected boolean addedToEnet;

    protected BasicEnergyTile(BlockEntity parent, double capacity) {
        this((Object) parent, capacity);
    }

    protected BasicEnergyTile(ILocatable parent, double capacity) {
        this((Object) parent, capacity);
    }

    private BasicEnergyTile(Object locationProvider, double capacity) {
        this.locationProvider = locationProvider;
        this.capacity = capacity;
    }

    protected BasicEnergyTile(World world, BlockPos pos, double capacity) {
        if (world == null) {
            throw new NullPointerException("null world");
        } else if (pos == null) {
            throw new NullPointerException("null pos");
        } else {
            this.locationProvider = null;
            this.world = world;
            this.pos = pos;
            this.capacity = capacity;
        }
    }

    public void update() {
        if (!addedToEnet) {
            onLoad();
        }
    }

    public void onLoad() {
        if (!addedToEnet && !getWorldObj().isClient && Info.isIc2Available()) {
            EnergyNet.instance.addTile(this);
            addedToEnet = true;
        }
    }

    public void invalidate() {
        onChunkUnload();
    }

    public void onChunkUnload() {
        if (addedToEnet && !getWorldObj().isClient && Info.isIc2Available()) {
            EnergyNet.instance.removeTile(this);
            addedToEnet = false;
        }
    }

    public void readFromNBT(NbtCompound tag) {
        NbtCompound data = tag.getCompound(getNbtTagName());
        setEnergyStored(data.getDouble("energy"));
    }

    public NbtCompound writeToNBT(NbtCompound tag) {
        NbtCompound data = new NbtCompound();
        data.putDouble("energy", getEnergyStored());
        tag.put(getNbtTagName(), data);
        return tag;
    }

    public double getCapacity() {
        return capacity;
    }

    public void setCapacity(double capacity) {
        this.capacity = capacity;
    }

    public double getEnergyStored() {
        return energyStored;
    }

    public void setEnergyStored(double amount) {
        this.energyStored = amount;
    }

    public double getFreeCapacity() {
        return getCapacity() - getEnergyStored();
    }

    public double addEnergy(double amount) {
        if (getWorldObj().isClient) {
            return 0.0;
        }

        double energyStored = getEnergyStored();
        double capacity = getCapacity();
        if (amount > capacity - energyStored) {
            amount = capacity - energyStored;
        }

        setEnergyStored(energyStored + amount);
        return amount;
    }

    public boolean canUseEnergy(double amount) {
        return getEnergyStored() >= amount;
    }

    public boolean useEnergy(double amount) {
        if (canUseEnergy(amount) && !getWorldObj().isClient) {
            setEnergyStored(getEnergyStored() - amount);
            return true;
        }

        return false;
    }

    public boolean charge(ItemStack stack) {
        if (stack != null && Info.isIc2Available() && !getWorldObj().isClient) {
            double energyStored = getEnergyStored();
            double amount = ElectricItem.manager.charge(stack, energyStored, Math.max(getSinkTier(), getSourceTier()), false, false);
            setEnergyStored(energyStored - amount);
            return amount > 0.0;
        }

        return false;
    }

    public boolean discharge(ItemStack stack, double limit) {
        if (stack != null && Info.isIc2Available() && !getWorldObj().isClient) {
            double energyStored = getEnergyStored();
            double amount = getCapacity() - energyStored;
            if (amount <= 0.0) {
                return false;
            }

            if (limit > 0.0 && limit < amount) {
                amount = limit;
            }

            amount = ElectricItem.manager.discharge(stack, amount, Math.max(getSinkTier(), getSourceTier()), limit > 0.0, true, false);
            setEnergyStored(energyStored + amount);
            return amount > 0.0;
        }

        return false;
    }

    @Override
    public World getWorldObj() {
        if (world == null) {
            initLocation();
        }

        return world;
    }

    @Override
    public BlockPos getPosition() {
        if (pos == null) {
            initLocation();
        }

        return pos;
    }

    private void initLocation() {
        if (locationProvider instanceof ILocatable) {
            ILocatable provider = (ILocatable) locationProvider;
            world = provider.getWorldObj();
            pos = provider.getPosition();
        } else {
            if (!(locationProvider instanceof BlockEntity)) {
                throw new IllegalStateException("no/incompatible location provider");
            }

            BlockEntity provider = (BlockEntity) locationProvider;
            world = provider.getWorld();
            pos = provider.getPos();
        }
    }

    protected abstract String getNbtTagName();

    protected int getSinkTier() {
        return 0;
    }

    protected int getSourceTier() {
        return 0;
    }
}
