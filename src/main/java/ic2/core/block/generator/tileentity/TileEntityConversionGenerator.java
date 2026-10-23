package ic2.core.block.generator.tileentity;

import ic2.api.energy.EnergyNet;
import ic2.api.energy.tile.IEnergyAcceptor;
import ic2.api.energy.tile.IEnergySource;
import ic2.core.block.TileEntityInventory;
import ic2.core.ref.TeBlock;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * Fabric port of TileEntityConversionGenerator - a generator that turns another kind of energy
 * into EU only as fast as the energy net takes it.
 *
 * <p>It offers what its source could give times its rate, and only draws from the source what the
 * net actually took. The window shows what it could give and what it gave the last tick. It emits
 * on every side but the one its source is on.
 */
public abstract class TileEntityConversionGenerator extends TileEntityInventory implements IEnergySource {
    private static final NumberFormat FORMAT = new DecimalFormat("#.#");

    private double lastProduction;
    private double maxProduction;
    private double production;

    protected TileEntityConversionGenerator(BlockEntityType<?> type, BlockPos pos, BlockState state, TeBlock teBlock) {
        super(type, pos, state, teBlock);
    }

    @Override
    public void tick() {
        if (world == null || world.isClient) {
            return;
        }
        lastProduction = production;
        production = 0.0;
        setActive(maxProduction > 0.0);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        updateSource();
    }

    @Override
    public void setFacing(Direction facing) {
        super.setFacing(facing);
        updateSource();
    }

    @Override
    public void onNeighborUpdate() {
        super.onNeighborUpdate();
        updateSource();
    }

    /** Looks for the source in front again, after the facing or a neighbour changed. */
    protected abstract void updateSource();

    public String getProduction() {
        return FORMAT.format(lastProduction);
    }

    public String getMaxProduction() {
        return FORMAT.format(maxProduction);
    }

    @Override
    public double getGuiAmount(String name) {
        return switch (name) {
            case "production" -> lastProduction;
            case "maxProduction" -> maxProduction;
            default -> super.getGuiAmount(name);
        };
    }

    protected abstract int getEnergyAvailable();

    protected abstract void drawEnergyAvailable(int amount);

    protected abstract double getMultiplier();

    @Override
    public double getOfferedEnergy() {
        return maxProduction = getEnergyAvailable() * getMultiplier();
    }

    @Override
    public void drawEnergy(double amount) {
        production += amount;
        drawEnergyAvailable((int) Math.ceil(amount / getMultiplier()));
    }

    @Override
    public int getSourceTier() {
        return Math.max(EnergyNet.instance.getTierFromPower(maxProduction), 2);
    }

    @Override
    public boolean emitsEnergyTo(IEnergyAcceptor receiver, Direction side) {
        return side != getFacing();
    }
}
