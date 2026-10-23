package ic2.core.block;

import ic2.api.energy.tile.IHeatSource;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * Fabric port of {@code ic2.core.block.TileEntityHeatSourceInventory}: the base of the heat
 * generators.
 *
 * <p>Every tick the generator tops its heat buffer up to what it may emit in a tick; whatever sits
 * in front of it ({@link #getFacing()}) draws from that buffer through {@link IHeatSource}. What
 * was drawn last is kept for the window, which shows it next to the maximum - the gui values
 * {@code heat} (amount: drawn, capacity: maximum).
 */
public abstract class TileEntityHeatSourceInventory extends TileEntityInventory implements IHeatSource {
    protected int transmitHeat;
    protected int heatBuffer;

    public TileEntityHeatSourceInventory(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void tick() {
        if (world == null || world.isClient) {
            return;
        }
        updateEntityServer();
    }

    @Override
    public void updateEntityServer() {
        int amount = getMaxHeatEmittedPerTick() - heatBuffer;
        if (amount > 0) {
            heatBuffer += fillHeatBuffer(amount);
        }
    }

    @Override
    public int maxrequestHeatTick(Direction direction) {
        return getConnectionBandwidth(direction);
    }

    @Override
    public int getConnectionBandwidth(Direction side) {
        return facingMatchesDirection(side) ? getMaxHeatEmittedPerTick() : 0;
    }

    @Override
    public int requestHeat(Direction direction, int amount) {
        return drawHeat(direction, amount, false);
    }

    @Override
    public int drawHeat(Direction side, int request, boolean simulate) {
        if (!facingMatchesDirection(side)) {
            return 0;
        }

        int drawn = Math.min(heatBuffer, request);
        if (!simulate) {
            heatBuffer -= drawn;
            transmitHeat = drawn;
        }
        return drawn;
    }

    public boolean facingMatchesDirection(Direction direction) {
        return direction == getFacing();
    }

    public int getHeatBuffer() {
        return heatBuffer;
    }

    public void setHeatBuffer(int heatBuffer) {
        this.heatBuffer = heatBuffer;
    }

    public void addToHeatBuffer(int heat) {
        heatBuffer += heat;
    }

    public int getTransmitHeat() {
        return transmitHeat;
    }

    /** {@code drawn / maximum}, as the windows print it. */
    public String getOutput() {
        return transmitHeat + " / " + getMaxHeatEmittedPerTick();
    }

    @Override
    public double getGuiAmount(String name) {
        return "heat".equals(name) ? transmitHeat : super.getGuiAmount(name);
    }

    @Override
    public double getGuiCapacity(String name) {
        return "heat".equals(name) ? getMaxHeatEmittedPerTick() : super.getGuiCapacity(name);
    }

    @Override
    public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.writeNbt(nbt, registries);
        nbt.putInt("HeatBuffer", heatBuffer);
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.readNbt(nbt, registries);
        heatBuffer = nbt.getInt("HeatBuffer");
    }

    /**
     * Makes up to {@code maxAmount} HU, spending fuel for it.
     *
     * @return the HU made
     */
    protected abstract int fillHeatBuffer(int maxAmount);

    public abstract int getMaxHeatEmittedPerTick();
}
