package ic2.core.block.generator.tileentity;

import ic2.core.block.comp.Fluids;
import ic2.core.block.invslot.InvSlot;
import ic2.core.block.invslot.InvSlotConsumableLiquid;
import ic2.core.block.invslot.InvSlotConsumableLiquidByTank;
import ic2.core.block.invslot.InvSlotOutput;
import ic2.core.init.IC2Blocks;
import ic2.core.init.MainConfig;
import ic2.core.ref.TeBlock;
import ic2.core.util.ConfigUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

/**
 * Fabric port of TileEntityGeoGenerator - the geothermal generator, which burns lava.
 *
 * <p>Two millibuckets of lava make one unit of fuel, so a bucket runs the generator for 500 ticks
 * at 20 EU/t. The lava comes in through the fluid slot, from a bucket or a cell, and what is left
 * of the container goes to the output slot.
 */
public class TileEntityGeoGenerator extends TileEntityBaseGenerator {
    private static final int FLUID_PER_TICK = 2;

    protected final Fluids.InternalFluidTank fluidTank;
    public final InvSlotConsumableLiquid fluidSlot;
    public final InvSlotOutput outputSlot;

    public TileEntityGeoGenerator(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, TeBlock.geo_generator, production(), 1, 2400);

        this.fluidTank = fluids().addTankInsert("fluid", 8000, Fluids.fluidPredicate(lava()));
        this.fluidSlot = new InvSlotConsumableLiquidByTank(this, "fluidSlot", InvSlot.Access.I, 1,
                InvSlot.InvSide.ANY, InvSlotConsumableLiquid.OpType.Drain, fluidTank);
        this.outputSlot = new InvSlotOutput(this, "output", 1);
    }

    public TileEntityGeoGenerator(BlockPos pos, BlockState state) { this(IC2Blocks.GEOTHERMAL_BE, pos, state); }
    public TileEntityGeoGenerator() { this(IC2Blocks.GEOTHERMAL_BE, BlockPos.ORIGIN, null); }

    private static double production() {
        return Math.round(20.0 * ConfigUtil.getDouble(MainConfig.get(), "balance/energy/generator/geothermal"));
    }

    private static Fluid lava() {
        Fluid known = FluidRegistry.getRegisteredFluids().get("lava");
        if (known != null) {
            return known;
        }

        Fluid lava = new Fluid("lava").setColor(0xD45A12);
        FluidRegistry.registerFluid(lava);
        return lava;
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
    public boolean gainFuel() {
        FluidStack drained = fluidTank.drainInternal(FLUID_PER_TICK, false);
        if (drained == null || drained.amount < FLUID_PER_TICK) {
            return false;
        }

        fluidTank.drainInternal(FLUID_PER_TICK, true);
        fuel++;
        return true;
    }

    @Override
    public String getOperationSoundFile() {
        return "Generators/GeothermalLoop.ogg";
    }

    public Fluids.InternalFluidTank getFluidTank() {
        return fluidTank;
    }
}
