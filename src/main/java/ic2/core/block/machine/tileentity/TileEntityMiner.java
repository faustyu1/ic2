package ic2.core.block.machine.tileentity;

import ic2.api.item.ElectricItem;
import ic2.api.item.IMiningDrill;
import ic2.api.upgrade.IUpgradableBlock;
import ic2.api.upgrade.UpgradableProperty;
import ic2.core.block.invslot.InvSlot;
import ic2.core.block.invslot.InvSlotConsumable;
import ic2.core.block.invslot.InvSlotDischarge;
import ic2.core.block.invslot.InvSlotUpgrade;
import ic2.core.block.machine.BlockMiningPipe;
import ic2.core.init.IC2Blocks;
import ic2.core.init.IC2ClassicBlocks;
import ic2.core.init.IC2ItemVariants;
import ic2.core.init.MainConfig;
import ic2.core.init.OreValues;
import ic2.core.item.tool.ItemScanner;
import ic2.core.ref.TeBlock;
import ic2.core.util.ConfigUtil;
import ic2.core.util.LiquidUtil;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FluidBlock;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * Fabric port of TileEntityMiner.
 *
 * <p>With a drill and mining pipes it digs a shaft straight down, laying pipe behind it; a block
 * takes 200 ticks at 6 EU a tick with a drill, 50 at 20 with a diamond drill, 20 at 200 with an
 * iridium drill (which mines with Fortune III), air 20 at 3, and the drill pays 50/80/800 EU and
 * the miner 2 EU per block of depth on top. With an OD or OV scanner it first looks for ores on
 * each layer within half the scanner's range and digs towards each in a straight line along the
 * layer. Fluid sources stop it until a pump next to it has taken them. What it mines goes into its
 * 15-slot buffer, the rest is dropped. Without a drill it pulls the pipe back up, 20 ticks and 3 EU
 * a tick per pipe, putting any block from its pipe slot back in place of the pipe.
 */
public class TileEntityMiner extends TileEntityElectricMachine implements IUpgradableBlock {
    enum MineResult { Working, Done, Failed_Temp, Failed_Perm }

    enum Mode { None, Withdraw, MineAir, MineDrill, MineDDrill, MineIDrill, MineCustomDrill }

    private Mode lastMode = Mode.None;
    public int progress;
    private int scannedLevel = -1;
    private int scanRange;
    private int lastX;
    private int lastZ;
    public boolean pumpMode;
    /** Whether the pipe ended in a fluid source a pump next to the miner can drain, and where. */
    public boolean canProvideLiquid;
    public BlockPos liquidPos;
    public final InvSlot buffer;
    public final InvSlotUpgrade upgradeSlot;
    public final InvSlotConsumable drillSlot;
    public final InvSlotConsumable pipeSlot;
    public final InvSlotConsumable scannerSlot;
    public final InvSlotDischarge dischargeSlot;
    boolean tickingUpgrades;

    public TileEntityMiner(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        this(type == null ? IC2Blocks.MINER_BE : type, pos, state, TeBlock.miner, 1000);
    }

    protected TileEntityMiner(BlockEntityType<?> type, BlockPos pos, BlockState state, TeBlock teBlock, int maxEnergy) {
        super(type, pos, state, teBlock, maxEnergy, ConfigUtil.getInt(MainConfig.get(), "balance/minerDischargeTier"));
        this.drillSlot = new ToolSlot("drill", InvSlot.InvSide.TOP) {
            @Override
            public boolean accepts(ItemStack stack) {
                return stack.getItem() instanceof IMiningDrill;
            }
        };
        this.pipeSlot = new ToolSlot("pipe", InvSlot.InvSide.TOP) {
            @Override
            public boolean accepts(ItemStack stack) {
                return isPipe(stack) || stack.getItem() instanceof BlockItem;
            }
        };
        this.scannerSlot = new ToolSlot("scanner", InvSlot.InvSide.BOTTOM) {
            @Override
            public boolean accepts(ItemStack stack) {
                return stack.getItem() instanceof ItemScanner;
            }
        };
        this.upgradeSlot = new InvSlotUpgrade(this, "upgrade", 1);
        this.buffer = new InvSlot(this, "buffer", InvSlot.Access.IO, 15, InvSlot.InvSide.SIDE);
        this.dischargeSlot = new InvSlotDischarge(this, "discharge", tier);
        this.lastX = pos.getX();
        this.lastZ = pos.getZ();
    }

    public TileEntityMiner(BlockPos pos, BlockState state) {
        this(null, pos, state);
    }

    /** The tool slots let nothing out while the upgrades run, so an ejector leaves the tools alone. */
    private abstract class ToolSlot extends InvSlotConsumable {
        ToolSlot(String name, InvSlot.InvSide side) {
            super(TileEntityMiner.this, name, InvSlot.Access.IO, 1, side);
        }

        @Override
        public boolean canOutput() {
            return !tickingUpgrades && super.canOutput();
        }
    }

    protected static boolean isPipe(ItemStack stack) {
        Item pipe = IC2ItemVariants.get("mining_pipe", "pipe");
        return pipe != null && stack.isOf(pipe);
    }

    protected static ItemStack pipeStack() {
        return new ItemStack(IC2ItemVariants.get("mining_pipe", "pipe"));
    }

    private static BlockState pipeState(BlockMiningPipe.MiningPipeType type) {
        return IC2ClassicBlocks.MINING_PIPE.getState(type);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        scannedLevel = -1;
        lastX = pos.getX();
        lastZ = pos.getZ();
        canProvideLiquid = false;
    }

    @Override
    public void tick() {
        if (world == null || world.isClient) {
            return;
        }

        drainDischargeSlot(dischargeSlot);
        chargeTools();
        tickingUpgrades = true;
        upgradeSlot.tick();
        tickingUpgrades = false;
        if (work()) {
            markDirty();
            setActive(true);
        } else {
            setActive(false);
        }
    }

    private void chargeTools() {
        if (ElectricItem.manager == null) {
            return;
        }
        if (!scannerSlot.isEmpty()) {
            useEnergy(ElectricItem.manager.charge(scannerSlot.get(), getEnergy(), 2, false, false));
        }
        if (!drillSlot.isEmpty()) {
            useEnergy(ElectricItem.manager.charge(drillSlot.get(), getEnergy(), 3, false, false));
        }
    }

    private boolean work() {
        BlockPos.Mutable operatingPos = getOperationPos();
        if (drillSlot.isEmpty()) {
            return withdrawPipe(operatingPos);
        }
        if (isBelowMap(operatingPos)) {
            return false;
        }

        BlockState state = world.getBlockState(operatingPos);
        if (state != pipeState(BlockMiningPipe.MiningPipeType.tip)) {
            return operatingPos.getY() > world.getBottomY() && digDown(operatingPos, state, false);
        }

        MineResult result = mineLevel(operatingPos.getY());
        if (result == MineResult.Done) {
            operatingPos.move(Direction.DOWN);
            return digDown(operatingPos, world.getBlockState(operatingPos), true);
        }
        return result == MineResult.Working;
    }

    private boolean isBelowMap(BlockPos pos) {
        return pos.getY() < world.getBottomY();
    }

    /** The first block under the miner that is not pipe: the tip, or where the next pipe goes. */
    private BlockPos.Mutable getOperationPos() {
        BlockPos.Mutable ret = pos.mutableCopy().move(Direction.DOWN);
        BlockState pipe = pipeState(BlockMiningPipe.MiningPipeType.pipe);
        while (!isBelowMap(ret) && world.getBlockState(ret) == pipe) {
            ret.move(Direction.DOWN);
        }
        return ret;
    }

    private boolean withdrawPipe(BlockPos.Mutable operatingPos) {
        if (lastMode != Mode.Withdraw) {
            lastMode = Mode.Withdraw;
            progress = 0;
        }

        if (isBelowMap(operatingPos) || world.getBlockState(operatingPos) != pipeState(BlockMiningPipe.MiningPipeType.tip)) {
            operatingPos.move(Direction.UP);
        }

        if (operatingPos.getY() == pos.getY() || getEnergy() < 3.0) {
            return false;
        }
        if (progress < 20) {
            useEnergy(3.0);
            progress++;
        } else {
            progress = 0;
            removePipe(operatingPos);
        }
        return true;
    }

    private void removePipe(BlockPos operatingPos) {
        world.removeBlock(operatingPos, false);
        storeDrop(pipeStack());

        ItemStack filler = pipeSlot.get();
        if (!filler.isEmpty() && !isPipe(filler) && filler.getItem() instanceof BlockItem blockItem) {
            pipeSlot.consume(1);
            world.setBlockState(operatingPos, blockItem.getBlock().getDefaultState());
        }
    }

    private boolean digDown(BlockPos.Mutable operatingPos, BlockState state, boolean removeTipAbove) {
        if (pipeSlot.isEmpty() || !isPipe(pipeSlot.get())) {
            return false;
        }
        if (isBelowMap(operatingPos)) {
            if (removeTipAbove) {
                world.setBlockState(operatingPos.setY(world.getBottomY()), pipeState(BlockMiningPipe.MiningPipeType.pipe));
            }
            return false;
        }

        MineResult result = mineBlock(operatingPos, state);
        if (result == MineResult.Failed_Temp || result == MineResult.Failed_Perm) {
            if (removeTipAbove) {
                world.setBlockState(operatingPos.move(Direction.UP), pipeState(BlockMiningPipe.MiningPipeType.tip));
            }
            return false;
        }

        if (result == MineResult.Done) {
            if (removeTipAbove) {
                world.setBlockState(operatingPos.up(), pipeState(BlockMiningPipe.MiningPipeType.pipe));
            }
            pipeSlot.consume(1);
            world.setBlockState(operatingPos, pipeState(BlockMiningPipe.MiningPipeType.tip));
        }
        return true;
    }

    private MineResult mineLevel(int y) {
        if (scannerSlot.isEmpty()) {
            return MineResult.Done;
        }
        if (scannedLevel != y) {
            scanRange = ((ItemScanner) scannerSlot.get().getItem()).startLayerScan(scannerSlot.get());
        }
        if (scanRange <= 0) {
            return MineResult.Failed_Temp;
        }

        scannedLevel = y;
        BlockPos.Mutable target = new BlockPos.Mutable();
        for (int x = pos.getX() - scanRange; x <= pos.getX() + scanRange; x++) {
            for (int z = pos.getZ() - scanRange; z <= pos.getZ() + scanRange; z++) {
                target.set(x, y, z);
                BlockState state = world.getBlockState(target);
                boolean isValidTarget = false;
                if (OreValues.isValuable(state) && canMine(target, state)) {
                    isValidTarget = true;
                } else if (pumpMode && LiquidUtil.getLiquid(world, target) != null && canPump(target)) {
                    isValidTarget = true;
                }

                if (isValidTarget) {
                    MineResult result = mineTowards(target.toImmutable());
                    if (result == MineResult.Done) {
                        return MineResult.Working;
                    }
                    if (result != MineResult.Failed_Perm) {
                        return result;
                    }
                }
            }
        }
        return MineResult.Done;
    }

    /** Digs along the layer towards the ore, one block of the straight line at a time. */
    private MineResult mineTowards(BlockPos dst) {
        int dx = Math.abs(dst.getX() - pos.getX());
        int sx = pos.getX() < dst.getX() ? 1 : -1;
        int dz = -Math.abs(dst.getZ() - pos.getZ());
        int sz = pos.getZ() < dst.getZ() ? 1 : -1;
        int err = dx + dz;
        BlockPos.Mutable target = new BlockPos.Mutable();
        int cx = pos.getX();
        int cz = pos.getZ();

        while (cx != dst.getX() || cz != dst.getZ()) {
            boolean isCurrentPos = cx == lastX && cz == lastZ;
            int e2 = 2 * err;
            if (e2 > dz) {
                err += dz;
                cx += sx;
            } else if (e2 < dx) {
                err += dx;
                cz += sz;
            }

            target.set(cx, dst.getY(), cz);
            BlockState state = world.getBlockState(target);
            boolean isBlocking = false;
            if (isCurrentPos) {
                isBlocking = true;
            } else if (!state.isAir()) {
                LiquidUtil.LiquidData liquid = LiquidUtil.getLiquid(world, target);
                if (liquid == null || liquid.isSource || pumpMode && canPump(target)) {
                    isBlocking = true;
                }
            }

            if (isBlocking) {
                MineResult result = mineBlock(target, state);
                if (result == MineResult.Done) {
                    lastX = cx;
                    lastZ = cz;
                }
                return result;
            }
        }

        lastX = pos.getX();
        lastZ = pos.getZ();
        return MineResult.Done;
    }

    private MineResult mineBlock(BlockPos target, BlockState state) {
        boolean isAirBlock = true;
        if (!state.isAir()) {
            isAirBlock = false;
            LiquidUtil.LiquidData liquidData = LiquidUtil.getLiquid(world, target);
            if (liquidData != null) {
                if (liquidData.isSource || pumpMode && canPump(target)) {
                    liquidPos = target.toImmutable();
                    canProvideLiquid = true;
                    return !pumpMode && !canMine(target, state) ? MineResult.Failed_Perm : MineResult.Failed_Temp;
                }
            } else if (!canMine(target, state)) {
                return MineResult.Failed_Perm;
            }
        }

        canProvideLiquid = false;
        int energyPerTick;
        int duration;
        Mode mode;
        ItemStack drill = drillSlot.get();
        if (isAirBlock) {
            mode = Mode.MineAir;
            energyPerTick = 3;
            duration = 20;
        } else if (isDrill(drill, "drill")) {
            mode = Mode.MineDrill;
            energyPerTick = 6;
            duration = 200;
        } else if (isDrill(drill, "diamond_drill")) {
            mode = Mode.MineDDrill;
            energyPerTick = 20;
            duration = 50;
        } else if (isDrill(drill, "iridium_drill")) {
            mode = Mode.MineIDrill;
            energyPerTick = 200;
            duration = 20;
        } else if (drill.getItem() instanceof IMiningDrill custom) {
            mode = Mode.MineCustomDrill;
            energyPerTick = custom.energyUse(drill, world, target, state);
            duration = custom.breakTime(drill, world, target, state);
        } else {
            return MineResult.Failed_Temp;
        }

        if (lastMode != mode) {
            lastMode = mode;
            progress = 0;
        }

        if (progress < duration) {
            if (useEnergy(energyPerTick)) {
                progress++;
                return MineResult.Working;
            }
        } else if (isAirBlock || harvestBlock(target, state)) {
            progress = 0;
            return MineResult.Done;
        }
        return MineResult.Failed_Temp;
    }

    private static boolean isDrill(ItemStack stack, String name) {
        return net.minecraft.registry.Registries.ITEM.getId(stack.getItem()).equals(net.minecraft.util.Identifier.of("ic2", name));
    }

    private boolean harvestBlock(BlockPos target, BlockState state) {
        int energyCost = 2 * (pos.getY() - target.getY());
        if (getEnergy() < energyCost) {
            return false;
        }

        ItemStack drill = drillSlot.get();
        boolean used = switch (lastMode) {
            case MineDrill -> ElectricItem.manager.use(drill, 50.0, null);
            case MineDDrill -> ElectricItem.manager.use(drill, 80.0, null);
            case MineIDrill -> ElectricItem.manager.use(drill, 800.0, null);
            case MineCustomDrill -> ((IMiningDrill) drill.getItem()).breakBlock(drill, world, target, state);
            default -> false;
        };
        if (!used) {
            return false;
        }

        useEnergy(energyCost);
        for (ItemStack drop : getDrops(target, state, lastMode == Mode.MineIDrill)) {
            storeDrop(drop);
        }
        world.removeBlock(target, false);
        return true;
    }

    /** What the block drops to a pickaxe - with Fortune III for the iridium drill. */
    private List<ItemStack> getDrops(BlockPos target, BlockState state, boolean fortune) {
        if (!(world instanceof ServerWorld serverWorld)) {
            return List.of();
        }
        ItemStack tool = new ItemStack(Items.DIAMOND_PICKAXE);
        if (fortune) {
            tool.addEnchantment(world.getRegistryManager().get(RegistryKeys.ENCHANTMENT).entryOf(Enchantments.FORTUNE), 3);
        }
        return Block.getDroppedStacks(state, serverWorld, target, world.getBlockEntity(target), null, tool);
    }

    /** Into the buffer as far as it goes, the rest onto the ground. */
    protected void storeDrop(ItemStack stack) {
        stack = stack.copy();
        for (int i = 0; i < buffer.size() && !stack.isEmpty(); i++) {
            ItemStack slot = buffer.get(i);
            if (slot.isEmpty()) {
                buffer.put(i, stack);
                stack = ItemStack.EMPTY;
            } else if (ItemStack.areItemsAndComponentsEqual(slot, stack) && slot.getCount() < slot.getMaxCount()) {
                int move = Math.min(stack.getCount(), slot.getMaxCount() - slot.getCount());
                slot.increment(move);
                stack.decrement(move);
                buffer.onChanged();
            }
        }
        if (!stack.isEmpty()) {
            ItemScatterer.spawn(world, pos.getX(), pos.getY(), pos.getZ(), stack);
        }
    }

    public boolean canPump(BlockPos target) {
        return false;
    }

    public boolean canMine(BlockPos target, BlockState state) {
        Block block = state.getBlock();
        if (state.isAir()) {
            return true;
        }
        if (block == IC2ClassicBlocks.MINING_PIPE || block == Blocks.CHEST) {
            return false;
        }
        if (block instanceof FluidBlock) {
            return isPumpConnected(target);
        }
        if (state.getHardness(world, target) < 0.0F) {
            return false;
        }
        if (!state.isToolRequired() || block == Blocks.COBWEB) {
            return true;
        }
        return !drillSlot.isEmpty() && drillSlot.get().isSuitableFor(state);
    }

    public boolean isPumpConnected(BlockPos target) {
        for (Direction dir : Direction.values()) {
            if (world.getBlockEntity(pos.offset(dir)) instanceof TileEntityPump pump && pump.pump(target, true, this) != null) {
                return true;
            }
        }
        return false;
    }

    public boolean isAnyPumpConnected() {
        for (Direction dir : Direction.values()) {
            if (world.getBlockEntity(pos.offset(dir)) instanceof TileEntityPump) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Set<UpgradableProperty> getUpgradableProperties() {
        return EnumSet.of(UpgradableProperty.ItemConsuming, UpgradableProperty.ItemProducing);
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.readNbt(nbt, lookup);
        int mode = nbt.getInt("lastMode");
        lastMode = mode >= 0 && mode < Mode.values().length ? Mode.values()[mode] : Mode.None;
        progress = nbt.getInt("progress");
    }

    @Override
    public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.writeNbt(nbt, lookup);
        nbt.putInt("lastMode", lastMode.ordinal());
        nbt.putInt("progress", progress);
    }
}
