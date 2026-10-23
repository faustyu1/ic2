package ic2.core.block.generator.tileentity;

import ic2.core.block.invslot.InvSlotConsumableFuel;
import ic2.core.init.IC2Blocks;
import ic2.core.init.MainConfig;
import ic2.core.ref.TeBlock;
import ic2.core.util.ConfigUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;

/**
 * Fabric port of TileEntityGenerator - the solid fuel generator.
 *
 * <p>Burn values are the vanilla ones divided by four, as in 1.12.2, so a piece of coal still runs
 * the generator for 400 ticks at 10 EU/t.
 */
public class TileEntityGenerator extends TileEntityBaseGenerator {

    public final InvSlotConsumableFuel fuelSlot;
    /** Burn value of the item currently being consumed; the fuel gauge is the ratio to it. */
    protected int totalFuel = 0;

    public TileEntityGenerator(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, TeBlock.generator, production(), 1, 4000);
        this.fuelSlot = new InvSlotConsumableFuel(this, "fuel", 1, false);
    }

    public TileEntityGenerator() {
        this(null, BlockPos.ORIGIN, null);
    }

    public TileEntityGenerator(BlockPos pos, BlockState state) {
        this(IC2Blocks.GENERATOR_BE, pos, state);
    }

    private static double production() {
        return Math.round(10.0 * ConfigUtil.getDouble(MainConfig.get(), "balance/energy/generator/generator"));
    }

    @Override
    public boolean gainFuel() {
        // the original divided the vanilla burn time by four to get its EU worth
        int fuelValue = fuelSlot.consumeFuel() / 4;
        if (fuelValue == 0) {
            return false;
        }

        fuel += fuelValue;
        totalFuel = fuelValue;
        return true;
    }

    /** The solid fuel generator burns whatever it has, even with a full buffer. */
    @Override
    public boolean isConverting() {
        return fuel > 0;
    }

    public double getFuelRatio() {
        return fuel <= 0 || totalFuel <= 0 ? 0.0 : (double) fuel / (double) totalFuel;
    }

    @Override
    public double getGuiValue(String name) {
        return switch (name) {
            case "fuel" -> getFuelRatio();
            default -> super.getGuiValue(name);
        };
    }

    @Override
    public String getOperationSoundFile() {
        return "Generators/GeneratorLoop.ogg";
    }

    @Override
    public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.writeNbt(nbt, registries);
        nbt.putInt("totalFuel", totalFuel);
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.readNbt(nbt, registries);
        totalFuel = nbt.getInt("totalFuel");
    }

    // For TeBlock compatibility
    public static Class<? extends TileEntityGenerator> getTeClass() { return TileEntityGenerator.class; }
}
