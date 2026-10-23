package ic2.core.block.generator.tileentity;

import ic2.api.recipe.ISemiFluidFuelManager;
import ic2.api.recipe.Recipes;
import ic2.core.SemiFluidFuelManager;
import ic2.core.block.comp.Fluids;
import ic2.core.block.invslot.InvSlot;
import ic2.core.block.invslot.InvSlotConsumableLiquid;
import ic2.core.block.invslot.InvSlotConsumableLiquidByManager;
import ic2.core.block.invslot.InvSlotOutput;
import ic2.core.init.IC2Blocks;
import ic2.core.init.MainConfig;
import ic2.core.ref.TeBlock;
import ic2.core.util.ConfigUtil;
import ic2.core.util.LiquidUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;

/**
 * Fabric port of TileEntitySemifluidGenerator - the generator that burns liquid fuels.
 *
 * <p>Every fuel says two things: how much energy a millibucket is worth and how much of it may
 * come out per tick. The second one becomes the generator's production for as long as that fuel
 * burns, which is why a tank of fuel runs at 32 EU/t and one of creosote at 8 EU/t off the same
 * block.
 */
public class TileEntitySemifluidGenerator extends TileEntityBaseGenerator {

    public final Fluids.InternalFluidTank fluidTank;
    public final InvSlotConsumableLiquid fluidSlot;
    public final InvSlotOutput outputSlot;

    public TileEntitySemifluidGenerator(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, TeBlock.semifluid_generator, 32.0, 1, 32000);

        this.fluidTank = fluids().addTankInsert("fluid", 10000,
                Fluids.fluidPredicate(Recipes.semiFluidGenerator));
        this.fluidSlot = new InvSlotConsumableLiquidByManager(this, "fluidSlot", InvSlot.Access.I, 1,
                InvSlot.InvSide.ANY, InvSlotConsumableLiquid.OpType.Drain, Recipes.semiFluidGenerator);
        this.outputSlot = new InvSlotOutput(this, "output", 1);
    }

    public TileEntitySemifluidGenerator(BlockPos pos, BlockState state) { this(IC2Blocks.SEMIFLUID_BE, pos, state); }
    public TileEntitySemifluidGenerator() { this(IC2Blocks.SEMIFLUID_BE, BlockPos.ORIGIN, null); }

    /** Fills {@link Recipes#semiFluidGenerator} with the 1.12.2 fuel table. */
    public static void init() {
        if (Recipes.semiFluidGenerator != null) {
            return;
        }

        Recipes.semiFluidGenerator = new SemiFluidFuelManager();
        if (ConfigUtil.getDouble(MainConfig.get(), "balance/energy/generator/semiFluidOil") > 0.0) {
            addFuel("oil", 16L, 8L);
        }
        if (ConfigUtil.getDouble(MainConfig.get(), "balance/energy/generator/semiFluidFuel") > 0.0) {
            addFuel("fuel", 128L, 32L);
        }
        if (ConfigUtil.getDouble(MainConfig.get(), "balance/energy/generator/semiFluidBiomass") > 0.0) {
            addFuel("biomass", 8L, 8L);
        }
        if (ConfigUtil.getDouble(MainConfig.get(), "balance/energy/generator/semiFluidBioethanol") > 0.0) {
            addFuel("bio.ethanol", 128L, 32L);
        }
        if (ConfigUtil.getDouble(MainConfig.get(), "balance/energy/generator/semiFluidBiogas") > 0.0) {
            addFuel("ic2biogas", 32L, 16L);
        }
        addFuel("ic2creosote", 3L, 8L);
    }

    public static void addFuel(String fluidName, long energyPerMb, long energyPerTick) {
        Recipes.semiFluidGenerator.addFluid(fluidName, energyPerMb, energyPerTick);
    }

    @Override
    public void tick() {
        // the generator runs first and fills its tank from the slot after, as in 1.12.2
        super.tick();
        if (world != null && !world.isClient && fluidSlot.processIntoTank(fluidTank, outputSlot)) {
            markDirty();
        }
    }

    @Override
    public boolean gainEnergy() {
        if (!isConverting()) {
            return false;
        }

        double burnt = Math.min(fuel, production);
        energyStored = Math.min(maxStorage, energyStored + burnt);
        fuel -= (int) burnt;
        markDirty();
        return true;
    }

    @Override
    public boolean needsFuel() {
        return fuel < production && getFreeEnergy() >= production;
    }

    @Override
    public boolean gainFuel() {
        if (fluidTank.isEmpty()) {
            return false;
        }
        long stored = LiquidUtil.toMb(fluidTank.getFluidAmount());

        ISemiFluidFuelManager.FuelProperty property = Recipes.semiFluidGenerator == null
                ? null : Recipes.semiFluidGenerator.getFuelProperty(fluidTank.getFluid().getFluid());
        if (property == null) {
            return false;
        }

        // enough millibuckets to cover one tick of output, but never less than one
        int toConsume = property.energyPerMb >= property.energyPerTick
                ? 1 : (int) Math.ceil((double) property.energyPerTick / property.energyPerMb);
        toConsume = (int) Math.min(toConsume, stored);
        if (toConsume <= 0 || stored < toConsume) {
            return false;
        }

        fluidTank.drainInternal(LiquidUtil.mb(toConsume), true);
        production = property.energyPerTick;
        fuel = (int) (fuel + (long) toConsume * property.energyPerMb);
        return true;
    }

    @Override
    public String getOperationSoundFile() {
        return "Generators/GeothermalLoop.ogg";
    }

    public Fluids.InternalFluidTank getFluidTank() {
        return fluidTank;
    }

    @Override
    public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.writeNbt(nbt, registries);
        nbt.putDouble("production", production);
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.readNbt(nbt, registries);
        if (nbt.contains("production")) {
            production = nbt.getDouble("production");
        }
    }
}
