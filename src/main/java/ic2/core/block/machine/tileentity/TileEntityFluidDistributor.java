package ic2.core.block.machine.tileentity;

import ic2.core.block.TileEntityInventory;
import ic2.core.block.comp.Fluids;
import ic2.core.block.invslot.InvSlot;
import ic2.core.block.invslot.InvSlotConsumableLiquid;
import ic2.core.block.invslot.InvSlotConsumableLiquidByTank;
import ic2.core.block.invslot.InvSlotOutput;
import ic2.core.init.IC2Blocks;
import ic2.core.ref.TeBlock;
import ic2.core.util.LiquidUtil;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Map;
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
 * Fabric port of TileEntityFluidDistributor.
 *
 * <p>Distributing, it takes fluid into its 1000 mB tank through its face and shares it evenly
 * between the fluid blocks on its other five sides. Concentrating (the window's button), it is the
 * other way round: it takes from the five sides and pushes everything out of its face.
 */
public class TileEntityFluidDistributor extends TileEntityInventory {
    public final InvSlotConsumableLiquidByTank inputSlot;
    public final InvSlotOutput OutputSlot;
    public final Fluids.InternalFluidTank fluidTank;

    public TileEntityFluidDistributor(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type == null ? IC2Blocks.FLUID_DISTRIBUTOR_BE : type, pos, state, TeBlock.fluid_distributor);
        this.fluidTank = fluids().addTank("fluidTank", 1000);
        this.inputSlot = new InvSlotConsumableLiquidByTank(this, "inputSlot", InvSlot.Access.I, 1, InvSlot.InvSide.BOTTOM,
                InvSlotConsumableLiquid.OpType.Fill, fluidTank);
        this.OutputSlot = new InvSlotOutput(this, "OutputSlot", 1);
        updateConnectivity();
    }

    public TileEntityFluidDistributor(BlockPos pos, BlockState state) {
        this(null, pos, state);
    }

    @Override
    public void setActive(boolean active) {
        super.setActive(active);
        updateConnectivity();
    }

    @Override
    public void setFacing(Direction facing) {
        super.setFacing(facing);
        updateConnectivity();
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.readNbt(nbt, lookup);
        updateConnectivity();
    }

    /** Distributing takes fluid in through the face only, concentrating through every other side. */
    protected void updateConnectivity() {
        if (fluidTank == null) {
            return; // the base class sets the facing before the tank exists
        }
        EnumSet<Direction> acceptingSides = EnumSet.of(getFacing());
        if (getActive()) {
            acceptingSides = EnumSet.complementOf(acceptingSides);
        }
        fluidTank.setSides(acceptingSides, Collections.emptySet());
    }

    public boolean isConcentrating() {
        return getActive();
    }

    @Override
    public void tick() {
        if (world == null || world.isClient) {
            return;
        }
        inputSlot.processFromTank(fluidTank, OutputSlot);
        if (fluidTank.getFluidAmount() > 0) {
            moveFluid();
        }
    }

    protected void moveFluid() {
        if (getActive()) {
            BlockEntity target = world.getBlockEntity(pos.offset(getFacing()));
            Direction side = getFacing().getOpposite();
            if (LiquidUtil.isFluidTile(target, side)) {
                int amount = LiquidUtil.fillTile(target, side, fluidTank.getFluid(), false);
                if (amount > 0) {
                    fluidTank.drainInternal(amount, true);
                }
            }
            return;
        }

        Map<Direction, BlockEntity> acceptingNeighbors = new EnumMap<>(Direction.class);
        int acceptedVolume = 0;
        for (Direction dir : Direction.values()) {
            if (dir == getFacing()) {
                continue;
            }
            BlockEntity target = world.getBlockEntity(pos.offset(dir));
            Direction side = dir.getOpposite();
            if (LiquidUtil.isFluidTile(target, side)) {
                int amount = LiquidUtil.fillTile(target, side, fluidTank.getFluid(), true);
                if (amount > 0) {
                    acceptingNeighbors.put(dir, target);
                    acceptedVolume += amount;
                }
            }
        }

        while (!acceptingNeighbors.isEmpty()) {
            int amount = Math.min(acceptedVolume, fluidTank.getFluidAmount());
            if (amount <= 0) {
                break;
            }

            amount /= acceptingNeighbors.size();
            if (amount <= 0) {
                // less left than neighbours: hand out what remains in turn
                for (Map.Entry<Direction, BlockEntity> entry : acceptingNeighbors.entrySet()) {
                    FluidStack fs = fluidTank.getFluid();
                    if (fs == null) {
                        return;
                    }
                    fs = fs.copy();
                    fs.amount = Math.min(acceptedVolume, fs.amount);
                    if (fs.amount <= 0) {
                        return;
                    }
                    int cAmount = LiquidUtil.fillTile(entry.getValue(), entry.getKey().getOpposite(), fs, false);
                    fluidTank.drainInternal(cAmount, true);
                    acceptedVolume -= cAmount;
                }
                break;
            }

            Iterator<Map.Entry<Direction, BlockEntity>> it = acceptingNeighbors.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Direction, BlockEntity> entry = it.next();
                FluidStack fs = fluidTank.getFluid();
                if (fs == null || fs.amount <= 0) {
                    break;
                }
                fs = fs.copy();
                fs.amount = Math.min(amount, fs.amount);
                int cAmount = LiquidUtil.fillTile(entry.getValue(), entry.getKey().getOpposite(), fs, false);
                fluidTank.drainInternal(cAmount, true);
                acceptedVolume -= cAmount;
                if (cAmount < fs.amount) {
                    it.remove();
                }
            }
        }
    }

    @Override
    public void onGuiEvent(PlayerEntity player, String event) {
        if ("mode".equals(event)) {
            setActive(!getActive());
        }
    }

    @Override
    public boolean getGuiCondition(String name) {
        return "concentrate".equals(name) ? getActive() : super.getGuiCondition(name);
    }
}
