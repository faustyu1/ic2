package ic2.core.block.machine.tileentity;

import ic2.api.recipe.IPatternStorage;
import ic2.api.upgrade.IUpgradableBlock;
import ic2.api.upgrade.UpgradableProperty;
import ic2.core.block.comp.Fluids;
import ic2.core.block.invslot.InvSlot;
import ic2.core.block.invslot.InvSlotConsumableLiquid;
import ic2.core.block.invslot.InvSlotConsumableLiquidByTank;
import ic2.core.block.invslot.InvSlotDischarge;
import ic2.core.block.invslot.InvSlotOutput;
import ic2.core.block.invslot.InvSlotUpgrade;
import ic2.core.init.IC2Blocks;
import ic2.core.ref.FluidName;
import ic2.core.ref.TeBlock;
import ic2.core.util.LiquidUtil;
import ic2.core.uu.UuGraph;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * Fabric port of TileEntityReplicator.
 *
 * <p>It makes copies of a pattern from the pattern storage next to it out of UU-matter: 0.1 mB of
 * UU a tick at 512 EU a tick until the pattern's cost is paid (overclockers speed it up and make
 * each tick dearer), once or over and over. UU-matter comes from cells or pipes into its
 * 16 000 mB tank. 2 000 000 EU, tier 4.
 */
public class TileEntityReplicator extends TileEntityElectricMachine implements IUpgradableBlock, InvSlotUpgrade.Listener {
    public enum Mode { STOPPED, SINGLE, CONTINUOUS }

    private static final double UU_PER_TICK_BASE = 1.0E-4;
    private static final double EU_PER_TICK_BASE = 512.0;

    private double uuPerTick = UU_PER_TICK_BASE;
    private double euPerTick = EU_PER_TICK_BASE;
    private double extraUuStored;
    public double uuProcessed;
    public ItemStack pattern;
    private Mode mode = Mode.STOPPED;
    public int index;
    public int maxIndex;
    public double patternUu;
    public double patternEu;
    public final InvSlotConsumableLiquid fluidSlot;
    public final InvSlotOutput cellSlot;
    public final InvSlotOutput outputSlot;
    public final InvSlotUpgrade upgradeSlot;
    public final InvSlotDischarge dischargeSlot;
    public final Fluids.InternalFluidTank fluidTank;

    public TileEntityReplicator(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type == null ? IC2Blocks.REPLICATOR_BE : type, pos, state, TeBlock.replicator, 2000000, 4);
        this.fluidTank = fluids().addTank("fluidTank", 16000, Fluids.fluidPredicate(FluidName.uu_matter.getInstance()));
        this.fluidSlot = new InvSlotConsumableLiquidByTank(this, "fluid", InvSlot.Access.I, 1, InvSlot.InvSide.ANY,
                InvSlotConsumableLiquid.OpType.Drain, fluidTank);
        this.cellSlot = new InvSlotOutput(this, "cell", 1);
        this.outputSlot = new InvSlotOutput(this, "output", 1);
        this.upgradeSlot = new InvSlotUpgrade(this, "upgrade", 4);
        this.dischargeSlot = new InvSlotDischarge(this, "discharge", 4);
    }

    public TileEntityReplicator(BlockPos pos, BlockState state) {
        this(null, pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (world != null && !world.isClient) {
            onUpgradesChanged();
            refreshInfo();
        }
    }

    @Override
    public void onUpgradesChanged() {
        uuPerTick = UU_PER_TICK_BASE / upgradeSlot.processTimeMultiplier;
        euPerTick = (EU_PER_TICK_BASE + upgradeSlot.extraEnergyDemand) * upgradeSlot.energyDemandMultiplier;
        setTier(upgradeSlot.getTier(defaultTier));
        setCapacity((int) Math.min(Integer.MAX_VALUE,
                Math.round((defaultEnergyStorage + (double) upgradeSlot.extraEnergyStorage) * upgradeSlot.energyStorageMultiplier)));
    }

    @Override
    public void tick() {
        if (world == null || world.isClient) {
            return;
        }

        drainDischargeSlot(dischargeSlot);
        boolean needsInvUpdate = false;
        if (fluidTank.getFluidAmount() < fluidTank.getCapacity()) {
            needsInvUpdate = fluidSlot.processIntoTank(fluidTank, cellSlot);
        }

        boolean newActive = false;
        if (mode != Mode.STOPPED && getEnergy() >= euPerTick && pattern != null && outputSlot.canAdd(pattern)) {
            double uuRemaining = patternUu - uuProcessed;
            boolean finish;
            if (uuRemaining <= uuPerTick) {
                finish = true;
            } else {
                uuRemaining = uuPerTick;
                finish = false;
            }

            if (consumeUu(uuRemaining)) {
                newActive = true;
                useEnergy(euPerTick);
                uuProcessed += uuRemaining;
                if (finish) {
                    uuProcessed = 0.0;
                    if (mode == Mode.SINGLE) {
                        mode = Mode.STOPPED;
                    } else {
                        refreshInfo();
                    }
                    if (pattern != null) {
                        outputSlot.add(pattern.copy());
                        needsInvUpdate = true;
                    }
                }
            }
        }

        setActive(newActive);
        needsInvUpdate |= upgradeSlot.tickNoMark();
        if (needsInvUpdate || newActive) {
            markDirty();
        }
    }

    private boolean consumeUu(double amount) {
        if (amount <= extraUuStored) {
            extraUuStored -= amount;
            return true;
        }
        amount -= extraUuStored;
        long toDrain = LiquidUtil.mb((long) Math.ceil(amount * 1000.0));
        if (!fluidTank.getFluid().isOf(FluidName.uu_matter.getInstance()) || fluidTank.drainInternal(toDrain, false) != toDrain) {
            return false;
        }
        fluidTank.drainInternal(toDrain, true);
        amount -= LiquidUtil.toMb(toDrain) / 1000.0;
        extraUuStored = amount < 0.0 ? -amount : 0.0;
        return true;
    }

    public void refreshInfo() {
        IPatternStorage storage = getPatternStorage();
        ItemStack oldPattern = pattern;
        if (storage == null) {
            pattern = null;
        } else {
            List<ItemStack> patterns = storage.getPatterns();
            if (index < 0 || index >= patterns.size()) {
                index = 0;
            }
            maxIndex = patterns.size();
            if (patterns.isEmpty()) {
                pattern = null;
            } else {
                pattern = patterns.get(index);
                patternUu = UuGraph.getInBuckets(pattern);
                if (oldPattern == null || !ItemStack.areItemsAndComponentsEqual(pattern, oldPattern)) {
                    uuProcessed = 0.0;
                    mode = Mode.STOPPED;
                }
            }
        }
        if (pattern == null) {
            uuProcessed = 0.0;
            mode = Mode.STOPPED;
        }
    }

    public IPatternStorage getPatternStorage() {
        for (Direction dir : Direction.values()) {
            if (world.getBlockEntity(pos.offset(dir)) instanceof IPatternStorage storage) {
                return storage;
            }
        }
        return null;
    }

    /** The window's buttons: {@code last}/{@code next} pick the pattern, {@code stop}, {@code single}, {@code repeat}. */
    @Override
    public void onGuiEvent(PlayerEntity player, String event) {
        switch (event) {
            case "last", "next" -> {
                if (mode == Mode.STOPPED) {
                    IPatternStorage storage = getPatternStorage();
                    if (storage != null && !storage.getPatterns().isEmpty()) {
                        int size = storage.getPatterns().size();
                        if ("last".equals(event)) {
                            index = index <= 0 ? size - 1 : index - 1;
                        } else {
                            index = index >= size - 1 ? 0 : index + 1;
                        }
                        refreshInfo();
                    }
                }
            }
            case "stop" -> {
                if (mode != Mode.STOPPED) {
                    uuProcessed = 0.0;
                    mode = Mode.STOPPED;
                }
            }
            case "single" -> {
                refreshIfNeeded();
                if (pattern != null) {
                    mode = Mode.SINGLE;
                }
            }
            case "repeat" -> {
                refreshIfNeeded();
                if (pattern != null) {
                    mode = Mode.CONTINUOUS;
                }
            }
            default -> {
                return;
            }
        }
        markDirty();
    }

    /** A pattern storage placed after the replicator was loaded is only seen once the pattern is refreshed. */
    private void refreshIfNeeded() {
        if (pattern == null) {
            refreshInfo();
        }
    }

    public Mode getMode() {
        return mode;
    }

    @Override
    public double getGuiAmount(String name) {
        return switch (name) {
            case "uuProgress" -> patternUu == 0.0 ? 0 : Math.min((int) Math.round(100.0 * uuProcessed / patternUu), 100);
            default -> super.getGuiAmount(name);
        };
    }

    @Override
    public boolean getGuiCondition(String name) {
        return switch (name) {
            case "stopped" -> mode == Mode.STOPPED;
            case "single" -> mode == Mode.SINGLE;
            default -> super.getGuiCondition(name);
        };
    }

    @Override
    public ItemStack getGuiItem(String name) {
        return "pattern".equals(name) && pattern != null ? pattern : super.getGuiItem(name);
    }

    @Override
    public Set<UpgradableProperty> getUpgradableProperties() {
        return EnumSet.of(UpgradableProperty.Processing, UpgradableProperty.RedstoneSensitive, UpgradableProperty.Transformer,
                UpgradableProperty.EnergyStorage, UpgradableProperty.ItemConsuming, UpgradableProperty.ItemProducing,
                UpgradableProperty.FluidConsuming);
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.readNbt(nbt, lookup);
        extraUuStored = nbt.getDouble("extraUuStored");
        uuProcessed = nbt.getDouble("uuProcessed");
        index = nbt.getInt("index");
        int modeIdx = nbt.getInt("mode");
        mode = modeIdx >= 0 && modeIdx < Mode.values().length ? Mode.values()[modeIdx] : Mode.STOPPED;
        ItemStack stored = TileEntityScanner.readItem(nbt.getString("pattern"));
        pattern = stored.isEmpty() ? null : stored;
        if (pattern != null) {
            patternUu = UuGraph.getInBuckets(pattern);
        }
    }

    @Override
    public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.writeNbt(nbt, lookup);
        nbt.putDouble("extraUuStored", extraUuStored);
        nbt.putDouble("uuProcessed", uuProcessed);
        nbt.putInt("index", index);
        nbt.putInt("mode", mode.ordinal());
        if (pattern != null) {
            nbt.putString("pattern", Registries.ITEM.getId(pattern.getItem()).toString());
        }
    }
}
