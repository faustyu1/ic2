package ic2.core.block.transport;

import ic2.api.transport.IFluidPipe;
import ic2.core.IC2;
import ic2.core.block.BlockIC2Fluid;
import ic2.core.block.comp.Fluids;
import ic2.core.block.transport.cover.CoverProperty;
import ic2.core.block.transport.cover.ICoverItem;
import ic2.core.block.transport.items.PipeSize;
import ic2.core.block.transport.items.PipeType;
import ic2.core.init.IC2Blocks;
import ic2.core.util.LiquidUtil;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.server.ServerTask;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.capability.IFluidHandler;

/**
 * The fluid pipe: bronze or steel, in four sizes. Each pipe holds as much fluid as it moves in a
 * second - {@code transferRate * size multiplier} mB, so 400/800/2400/4800 mB for tiny to large
 * bronze and twice that for steel - and every tick:
 *
 * <ul>
 * <li>evens out its fluid with the connected pipes that hold no more than it does (the remainder
 *     of the division goes to them one mB at a time, at random, when nothing else takes fluid);</li>
 * <li>hands up to a twentieth of its capacity (20/40/120/240 mB for bronze, 40/80/240/480 mB for
 *     steel) to the connected blocks that are not pipes, split evenly among them.</li>
 * </ul>
 *
 * <p>Fluid only passes the sides the wrench connected, and a cover on a side decides what may
 * pass it: a pump cover lets fluid in but none out, and pulls it from the block beside it. A pipe
 * broken with at least a bucket and more inside leaves a source block of its fluid behind.
 */
public class TileEntityFluidPipe extends TileEntityPipe implements IFluidPipe {
    protected final PipeType type;
    protected final PipeSize size;
    protected final Fluids.InternalFluidTank tank;

    public TileEntityFluidPipe(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type == null ? IC2Blocks.FLUID_PIPE_BE : type, pos, state);
        if (state != null && state.getBlock() instanceof BlockPipe pipe) {
            this.type = pipe.getPipeType();
            this.size = pipe.getPipeSize();
        } else {
            this.type = PipeType.bronze;
            this.size = PipeSize.small;
        }
        this.tank = fluids().addTank("tank", getCapacity(this.type, this.size));
    }

    public TileEntityFluidPipe(BlockPos pos, BlockState state) {
        this(null, pos, state);
    }

    /** How much a pipe holds, which is also what it moves in a second. */
    public static int getCapacity(PipeType type, PipeSize size) {
        return (int) ((float) type.transferRate * size.multiplier);
    }

    /** What a pipe hands to the blocks that are not pipes in one tick, at most. */
    public static int getTransferPerTick(PipeType type, PipeSize size) {
        return (int) ((float) type.transferRate * size.multiplier / 20.0F);
    }

    public PipeType getPipeType() {
        return type;
    }

    public PipeSize getPipeSize() {
        return size;
    }

    @Override
    public Set<CoverProperty> getCoverProperties() {
        return EnumSet.of(CoverProperty.FluidConsuming);
    }

    @Override
    public int getTransferRate() {
        return type.transferRate;
    }

    @Override
    public FluidTank getTank() {
        return tank;
    }

    @Override
    public int getCurrentInnerCapacity() {
        return tank.getFluidAmount();
    }

    @Override
    public int getMaxInnerCapacity() {
        return tank.getCapacity();
    }

    @Override
    public void flipConnection(Direction facing) {
        if (world == null || world.isClient) {
            return;
        }
        connectivity = (byte) (connectivity ^ 1 << facing.ordinal());
        markDirty();
        updatePipeState();
        world.updateNeighbors(pos, getCachedState().getBlock());
    }

    @Override
    public void updateEntityServer() {
        if (tank.getFluid() == null || tank.getFluidAmount() <= 0) {
            return;
        }
        int before = tank.getFluidAmount();

        int availableFluidAmount = tank.getFluidAmount();
        int cPipes = 1;
        List<LiquidUtil.AdjacentFluidHandler> adjacentFluidHandlers = new ArrayList<>();

        for (Direction facing : Direction.values()) {
            if (LiquidUtil.drainTile(this, facing, Integer.MAX_VALUE, true) == null) {
                continue;
            }
            LiquidUtil.AdjacentFluidHandler target = LiquidUtil.getAdjacentHandler(this, facing);
            if (target == null) {
                continue;
            }
            if (target.handler instanceof IFluidPipe pipe) {
                int targetAmount = pipe.getTank().getFluidAmount();
                if (tank.getFluidAmount() >= targetAmount) {
                    availableFluidAmount += targetAmount;
                    cPipes++;
                    adjacentFluidHandlers.add(target);
                }
            } else if (LiquidUtil.fillTile(target.handler, facing.getOpposite(), tank.getFluid().copy(), true) > 0) {
                adjacentFluidHandlers.add(target);
            }
        }

        // even out with the pipes that hold less
        int extraFluid = availableFluidAmount % cPipes;
        availableFluidAmount /= cPipes;
        List<LiquidUtil.AdjacentFluidHandler> adjacentPipes = new ArrayList<>();
        Iterator<LiquidUtil.AdjacentFluidHandler> it = adjacentFluidHandlers.iterator();
        while (it.hasNext()) {
            LiquidUtil.AdjacentFluidHandler target = it.next();
            if (target.handler instanceof IFluidPipe pipe) {
                LiquidUtil.transfer(this, target.dir, target.handler, availableFluidAmount - pipe.getTank().getFluidAmount());
                adjacentPipes.add(target);
                it.remove();
            }
        }

        if (adjacentFluidHandlers.isEmpty()) {
            while (extraFluid > 0 && !adjacentPipes.isEmpty()) {
                int bullet = IC2.random.nextInt(adjacentPipes.size());
                LiquidUtil.AdjacentFluidHandler target = adjacentPipes.get(bullet);
                LiquidUtil.transfer(this, target.dir, target.handler, 1);
                extraFluid--;
                adjacentPipes.remove(bullet);
            }
        } else {
            availableFluidAmount += extraFluid;
        }

        // feed the blocks that are not pipes
        if (!adjacentFluidHandlers.isEmpty()) {
            int maxTransfer = Math.min(availableFluidAmount, getTransferPerTick(type, size));
            int maxAmountPerOutput = maxTransfer / adjacentFluidHandlers.size();
            if (maxAmountPerOutput <= 0) {
                while (maxTransfer > 0 && !adjacentFluidHandlers.isEmpty()) {
                    int bullet = IC2.random.nextInt(adjacentFluidHandlers.size());
                    LiquidUtil.AdjacentFluidHandler target = adjacentFluidHandlers.get(bullet);
                    LiquidUtil.transfer(this, target.dir, target.handler, 1);
                    maxTransfer--;
                    adjacentFluidHandlers.remove(bullet);
                }
            } else {
                for (LiquidUtil.AdjacentFluidHandler target : adjacentFluidHandlers) {
                    LiquidUtil.transfer(this, target.dir, target.handler, maxAmountPerOutput);
                }
            }
        }

        if (tank.getFluidAmount() != before) {
            markDirty();
        }
    }

    /**
     * A side gives a handler only while it is connected; a cover on it may refuse what passes.
     */
    @Override
    public IFluidHandler getFluidHandler(Direction side) {
        return side != null && isConnected(side) ? new PipeFluidHandler(side) : null;
    }

    /**
     * The pipe joins the block it was put against if that block is a pipe or holds fluid - the
     * block chose that connection on placement; here the other pipe connects back.
     */
    @Override
    public void onPlaced(ItemStack stack, LivingEntity placer) {
        super.onPlaced(stack, placer);
        if (world == null || world.isClient) {
            return;
        }
        for (Direction facing : Direction.values()) {
            if (isConnected(facing) && world.getBlockEntity(pos.offset(facing)) instanceof IFluidPipe other
                    && !other.isConnected(facing.getOpposite())) {
                other.flipConnection(facing.getOpposite());
            }
        }
    }

    /** More than a bucket left in a broken pipe spills as a source block where it stood. */
    @Override
    public void onBlockRemoved() {
        super.onBlockRemoved();
        if (tank.getFluidAmount() <= 1000 || !(world instanceof ServerWorld serverWorld)) {
            return;
        }
        FluidStack fluid = tank.getFluid().copy();
        BlockPos at = pos;
        // the pipe is still being taken out of the world here, so the fluid goes in right after
        serverWorld.getServer().send(new ServerTask(serverWorld.getServer().getTicks(),
                () -> fillBlock(fluid, serverWorld, at)));
    }

    /** LiquidUtil.fillBlock of 1.12.2: puts a source block of the fluid into an empty space. */
    static boolean fillBlock(FluidStack fs, World world, BlockPos pos) {
        if (fs == null || fs.amount < 1000) {
            return false;
        }
        BlockState fluidState = sourceBlockOf(fs.getFluid());
        if (fluidState == null) {
            return false;
        }
        BlockState state = world.getBlockState(pos);
        if (!state.isAir() && state.isSolid()) {
            return false;
        }
        if (state == fluidState) {
            return false;
        }
        if (world.getDimension().ultrawarm() && fluidState.getFluidState().isOf(net.minecraft.fluid.Fluids.WATER)) {
            world.playSound(null, pos, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 0.5F,
                    2.6F + (world.random.nextFloat() - world.random.nextFloat()) * 0.8F);
            if (world instanceof ServerWorld serverWorld) {
                serverWorld.spawnParticles(ParticleTypes.LARGE_SMOKE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        8, 0.5, 0.5, 0.5, 0.0);
            }
        } else {
            if (!state.isAir() && !state.isLiquid()) {
                world.breakBlock(pos, true);
            }
            if (!world.setBlockState(pos, fluidState)) {
                return false;
            }
        }
        fs.amount -= 1000;
        return true;
    }

    private static BlockState sourceBlockOf(Fluid fluid) {
        if (fluid == null) {
            return null;
        }
        net.minecraft.fluid.Fluid mc = fluid.getMinecraftFluid();
        if (mc != null && mc != net.minecraft.fluid.Fluids.EMPTY) {
            return mc.getDefaultState().getBlockState();
        }
        for (Block block : Registries.BLOCK) {
            if (block instanceof BlockIC2Fluid ic2Fluid && ic2Fluid.getIc2Fluid() == fluid) {
                return block.getDefaultState();
            }
        }
        return null;
    }

    @Override
    public void addInformation(ItemStack stack, List<Text> tooltip) {
        int amount = getCapacity(type, size);
        tooltip.add(Text.literal("Transfer rate: " + amount + " mb/sec").formatted(Formatting.WHITE));
        tooltip.add(Text.literal("Inner capacity: " + amount + " mb").formatted(Formatting.WHITE));
        tooltip.add(Text.literal("Make connections with a wrench").formatted(Formatting.GOLD));
    }

    /** One side of the pipe; the cover on that side has a say in what passes. */
    private class PipeFluidHandler implements IFluidHandler {
        private final Direction side;

        PipeFluidHandler(Direction side) {
            this.side = side;
        }

        @Override
        public int fill(FluidStack resource, boolean doFill) {
            ICoverItem cover = coversComponent.getCoverItem(side);
            if (cover != null && !cover.allowsInput(resource)) {
                return 0;
            }
            int ret = tank.fill(resource, doFill);
            if (doFill && ret > 0) {
                markDirty();
            }
            return ret;
        }

        @Override
        public FluidStack drain(FluidStack resource, boolean doDrain) {
            ICoverItem cover = coversComponent.getCoverItem(side);
            if (cover != null && !cover.allowsOutput(resource)) {
                return null;
            }
            // Forge's FluidTank.drain(FluidStack): only the fluid the tank holds
            if (resource == null || tank.getFluid() == null || !tank.getFluid().isFluidEqual(resource)) {
                return null;
            }
            return drain(resource.amount, doDrain);
        }

        @Override
        public FluidStack drain(int maxDrain, boolean doDrain) {
            FluidStack inTank = tank.getFluid();
            if (inTank == null || maxDrain <= 0) {
                return null;
            }
            ICoverItem cover = coversComponent.getCoverItem(side);
            if (cover != null && !cover.allowsOutput(new FluidStack(inTank.getFluid(), maxDrain))) {
                return null;
            }
            FluidStack ret = tank.drain(maxDrain, doDrain);
            if (doDrain && ret != null && ret.amount > 0) {
                markDirty();
            }
            return ret;
        }
    }
}
