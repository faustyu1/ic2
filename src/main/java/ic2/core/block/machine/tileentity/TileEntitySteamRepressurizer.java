package ic2.core.block.machine.tileentity;

import ic2.api.energy.tile.IHeatSource;
import ic2.core.block.TileEntityInventory;
import ic2.core.block.comp.Fluids;
import ic2.core.init.IC2Blocks;
import ic2.core.init.MainConfig;
import ic2.core.ref.FluidName;
import ic2.core.ref.TeBlock;
import ic2.core.util.ConfigUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

/**
 * Fabric port of TileEntitySteamRepressurizer: turns IC2 steam into the plain {@code steam} fluid
 * of other mods, 16 mB for every 10 mB of steam (22 for superheated steam, see
 * {@code balance/steamRepressurizer}), using 1 HU from its neighbours for each 10 mB.
 *
 * <p>Like in 1.12.2 it only works when some mod registers a fluid called {@code steam}; without
 * one the window says so and it does nothing.
 */
public class TileEntitySteamRepressurizer extends TileEntityInventory {
    protected static final int CONSUMPTION = 10;

    protected int currentHeat;
    protected final Fluids.InternalFluidTank output;
    protected final Fluids.InternalFluidTank input;

    public TileEntitySteamRepressurizer(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type == null ? IC2Blocks.STEAM_REPRESSURIZER_BE : type, pos, state, TeBlock.steam_repressurizer);
        this.input = fluids().addTankInsert("input", 10000,
                Fluids.fluidPredicate(FluidName.steam.getInstance(), FluidName.superheated_steam.getInstance()));
        this.output = fluids().addTankExtract("output", 10000);
    }

    public TileEntitySteamRepressurizer(BlockPos pos, BlockState state) {
        this(null, pos, state);
    }

    /** The other mods' steam, looked up when needed since it may be registered after IC2. */
    public static Fluid steam() {
        return FluidRegistry.getFluid("steam");
    }

    public static boolean hasSteam() {
        return steam() != null;
    }

    @Override
    public void tick() {
        if (world == null || world.isClient || !hasSteam() || input.getFluidAmount() < CONSUMPTION) {
            return;
        }

        if (currentHeat < input.getFluidAmount() / CONSUMPTION) {
            getHeat();
        }
        int amount = getOutput();
        while (currentHeat > 0 && input.getFluidAmount() >= CONSUMPTION && canOutput(amount)) {
            currentHeat--;
            input.drainInternal(CONSUMPTION, true);
            output.fillInternal(new FluidStack(steam(), amount), true);
        }
        markDirty();
    }

    protected void getHeat() {
        int aim = input.getFluidAmount() / CONSUMPTION;
        if (aim <= 0) {
            return;
        }
        int targetHeat = aim;
        for (Direction dir : Direction.values()) {
            BlockEntity target = world.getBlockEntity(pos.offset(dir));
            if (target instanceof IHeatSource hs) {
                int request = hs.drawHeat(dir.getOpposite(), targetHeat, true);
                if (request > 0) {
                    targetHeat -= hs.drawHeat(dir.getOpposite(), request, false);
                    if (targetHeat <= 0) {
                        break;
                    }
                }
            }
        }
        currentHeat += aim - targetHeat;
    }

    protected int getOutput() {
        Fluid fluid = input.getFluid().getFluid();
        if (fluid == FluidName.steam.getInstance()) {
            return ConfigUtil.getInt(MainConfig.get(), "balance/steamRepressurizer/steamPerSteam");
        }
        if (fluid == FluidName.superheated_steam.getInstance()) {
            return ConfigUtil.getInt(MainConfig.get(), "balance/steamRepressurizer/steamPerSuperSteam");
        }
        throw new IllegalStateException("Unknown tank contents: " + fluid);
    }

    protected boolean canOutput(int amount) {
        return output.fillInternal(new FluidStack(steam(), amount), false) == amount;
    }

    @Override
    public boolean getGuiCondition(String name) {
        return "valid".equals(name) ? hasSteam() : super.getGuiCondition(name);
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.readNbt(nbt, lookup);
        currentHeat = nbt.getInt("heat");
    }

    @Override
    public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.writeNbt(nbt, lookup);
        nbt.putInt("heat", currentHeat);
    }
}
