package ic2.core.block.machine.tileentity;

import ic2.core.IC2;
import ic2.core.block.comp.Fluids;
import ic2.core.block.invslot.InvSlot;
import ic2.core.block.invslot.InvSlotConsumableLiquid;
import ic2.core.block.invslot.InvSlotConsumableLiquidByTank;
import ic2.core.block.invslot.InvSlotDischarge;
import ic2.core.block.invslot.InvSlotOutput;
import ic2.core.init.IC2Blocks;
import ic2.core.ref.TeBlock;
import ic2.core.util.LiquidUtil;
import java.util.Collections;
import java.util.EnumSet;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraftforge.fluids.FluidStack;

/**
 * Fabric port of TileEntityFluidRegulator: pushes a set amount of fluid (0-1000 mB) out of its
 * face once a second, or every tick in tick mode, for 10 EU each time. It fills its 10 000 mB tank
 * from every other side and from containers. Only a whole portion moves: if the block in front
 * takes less, it still gets the amount it took, but the full portion leaves the tank, as in 1.12.2.
 */
public class TileEntityFluidRegulator extends TileEntityElectricMachine {
    private int mode;
    private int updateTicker = IC2.random.nextInt(getTickRate());
    private int outputmb;
    public final InvSlotOutput wasseroutputSlot;
    public final InvSlotConsumableLiquidByTank wasserinputSlot;
    public final InvSlotDischarge dischargeSlot;
    protected final Fluids.InternalFluidTank fluidTank;

    public TileEntityFluidRegulator(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type == null ? IC2Blocks.FLUID_REGULATOR_BE : type, pos, state, TeBlock.fluid_regulator, 10000, 4);
        this.fluidTank = fluids().addTank("fluidTank", 10000, InvSlot.Access.NONE, InvSlot.InvSide.ANY, fluid -> true);
        this.wasserinputSlot = new InvSlotConsumableLiquidByTank(this, "wasserinputSlot", InvSlot.Access.I, 1, InvSlot.InvSide.TOP,
                InvSlotConsumableLiquid.OpType.Drain, fluidTank);
        this.wasseroutputSlot = new InvSlotOutput(this, "wasseroutputSlot", 1);
        this.dischargeSlot = new InvSlotDischarge(this, "discharge", 4);
        updateConnectivity();
    }

    public TileEntityFluidRegulator(BlockPos pos, BlockState state) {
        this(null, pos, state);
    }

    @Override
    public void setFacing(Direction side) {
        super.setFacing(side);
        updateConnectivity();
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.readNbt(nbt, lookup);
        outputmb = nbt.getInt("outputmb");
        mode = nbt.getInt("mode");
        updateConnectivity();
    }

    @Override
    public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.writeNbt(nbt, lookup);
        nbt.putInt("outputmb", outputmb);
        nbt.putInt("mode", mode);
    }

    private void updateConnectivity() {
        if (fluidTank != null) {
            fluidTank.setSides(EnumSet.complementOf(EnumSet.of(getFacing())), Collections.emptySet());
        }
    }

    @Override
    public void tick() {
        if (world == null || world.isClient) {
            return;
        }
        drainDischargeSlot(dischargeSlot);
        wasserinputSlot.processIntoTank(fluidTank, wasseroutputSlot);
        if (updateTicker++ % getTickRate() == 0 || mode != 0) {
            setActive(work());
        }
    }

    private boolean work() {
        if (outputmb == 0 || getEnergy() < 10.0 || fluidTank.getFluidAmount() <= 0) {
            return false;
        }

        Direction dir = getFacing();
        BlockEntity te = world.getBlockEntity(pos.offset(dir));
        Direction side = dir.getOpposite();
        if (LiquidUtil.isFluidTile(te, side)) {
            FluidStack portion = fluidTank.drainInternal(outputmb, false);
            int amount = LiquidUtil.fillTile(te, side, portion, false);
            if (amount > 0) {
                fluidTank.drainInternal(outputmb, true);
                useEnergy(10.0);
                return true;
            }
        }
        return false;
    }

    /** The window's buttons: {@code out:<delta>} for the amount, {@code tick}/{@code sec} for the mode. */
    @Override
    public void onGuiEvent(PlayerEntity player, String event) {
        if ("tick".equals(event)) {
            mode = 1;
        } else if ("sec".equals(event)) {
            mode = 0;
        } else if (event.startsWith("out:")) {
            try {
                setOutputMb(outputmb + Integer.parseInt(event.substring(4)));
            } catch (NumberFormatException ignored) {
                return;
            }
        }
        markDirty();
    }

    public void setOutputMb(int outputmb) {
        this.outputmb = Math.max(0, Math.min(1000, outputmb));
    }

    public void setTickMode(boolean everyTick) {
        mode = everyTick ? 1 : 0;
    }

    public int getTickRate() {
        return 20;
    }

    public int getoutputmb() {
        return outputmb;
    }

    public Fluids.InternalFluidTank getFluidTank() {
        return fluidTank;
    }

    @Override
    public double getGuiAmount(String name) {
        return "output".equals(name) ? outputmb : super.getGuiAmount(name);
    }

    @Override
    public boolean getGuiCondition(String name) {
        return "tickMode".equals(name) ? mode == 1 : super.getGuiCondition(name);
    }
}
