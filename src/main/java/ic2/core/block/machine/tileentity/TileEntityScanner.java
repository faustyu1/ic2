package ic2.core.block.machine.tileentity;

import ic2.api.recipe.IPatternStorage;
import ic2.core.block.invslot.InvSlot;
import ic2.core.block.invslot.InvSlotConsumable;
import ic2.core.block.invslot.InvSlotDischarge;
import ic2.core.init.IC2Blocks;
import ic2.core.item.ItemCrystalMemory;
import ic2.core.ref.TeBlock;
import ic2.core.uu.UuGraph;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * Fabric port of TileEntityScanner.
 *
 * <p>It scans one item with a UU value in 3300 ticks at 256 EU a tick, using up the item, and
 * keeps the result until it is saved - into the crystal memory in its slot, or else into a
 * pattern storage next to it - or deleted with the window's buttons. Without a place to store the
 * pattern it does not start; an item already recorded there is refused. 512 000 EU, tier 4.
 */
public class TileEntityScanner extends TileEntityElectricMachine {
    public enum State { IDLE, SCANNING, COMPLETED, FAILED, NO_STORAGE, NO_ENERGY, TRANSFER_ERROR, ALREADY_RECORDED }

    private static final int ENERGY_PER_TICK = 256;
    public static final int DURATION = 3300;

    private ItemStack currentStack = ItemStack.EMPTY;
    private ItemStack pattern = ItemStack.EMPTY;
    public int progress;
    public final InvSlotConsumable inputSlot;
    public final InvSlotConsumable diskSlot;
    public final InvSlotDischarge dischargeSlot;
    private State state = State.IDLE;
    public double patternUu;
    public double patternEu;

    public TileEntityScanner(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type == null ? IC2Blocks.SCANNER_BE : type, pos, state, TeBlock.scanner, 512000, 4);
        this.inputSlot = new InvSlotConsumable(this, "input", 1) {
            @Override
            public boolean accepts(ItemStack stack) {
                return !UuGraph.find(stack).isEmpty() && UuGraph.get(stack) < Double.POSITIVE_INFINITY;
            }
        };
        this.inputSlot.setStackSizeLimit(1);
        this.diskSlot = new InvSlotConsumable(this, "disk", InvSlot.Access.IO, 1, InvSlot.InvSide.ANY) {
            @Override
            public boolean accepts(ItemStack stack) {
                return stack.getItem() instanceof ItemCrystalMemory;
            }
        };
        this.dischargeSlot = new InvSlotDischarge(this, "discharge", 4);
    }

    public TileEntityScanner(BlockPos pos, BlockState state) {
        this(null, pos, state);
    }

    @Override
    public void tick() {
        if (world == null || world.isClient) {
            return;
        }

        drainDischargeSlot(dischargeSlot);
        boolean newActive = false;
        if (progress < DURATION) {
            if (!inputSlot.isEmpty() && (currentStack.isEmpty() || currentStack.isOf(inputSlot.get().getItem()))) {
                if (getPatternStorage() == null && diskSlot.isEmpty()) {
                    state = State.NO_STORAGE;
                    reset();
                } else if (getEnergy() >= ENERGY_PER_TICK) {
                    if (currentStack.isEmpty()) {
                        currentStack = inputSlot.get().copyWithCount(1);
                    }
                    pattern = UuGraph.find(currentStack);
                    if (pattern.isEmpty()) {
                        state = State.FAILED;
                    } else if (isPatternRecorded(pattern)) {
                        state = State.ALREADY_RECORDED;
                        reset();
                    } else {
                        newActive = true;
                        state = State.SCANNING;
                        useEnergy(ENERGY_PER_TICK);
                        progress++;
                        if (progress >= DURATION) {
                            refreshInfo();
                            if (patternUu != Double.POSITIVE_INFINITY) {
                                state = State.COMPLETED;
                                inputSlot.consume(1);
                                markDirty();
                            } else {
                                state = State.FAILED;
                            }
                        }
                    }
                } else {
                    state = State.NO_ENERGY;
                }
            } else {
                state = State.IDLE;
                reset();
            }
        } else if (pattern.isEmpty()) {
            state = State.IDLE;
            progress = 0;
        }
        setActive(newActive);
    }

    public void reset() {
        progress = 0;
        currentStack = ItemStack.EMPTY;
        pattern = ItemStack.EMPTY;
    }

    private boolean isPatternRecorded(ItemStack stack) {
        ItemStack disk = diskSlot.get();
        if (disk.getItem() instanceof ItemCrystalMemory memory && memory.readItemStack(disk).isOf(stack.getItem())) {
            return true;
        }
        IPatternStorage storage = getPatternStorage();
        if (storage != null) {
            for (ItemStack stored : storage.getPatterns()) {
                if (stored.isOf(stack.getItem())) {
                    return true;
                }
            }
        }
        return false;
    }

    private void record() {
        if (!pattern.isEmpty() && patternUu != Double.POSITIVE_INFINITY && !saveToDisk(pattern)) {
            IPatternStorage storage = getPatternStorage();
            if (storage == null || !storage.addPattern(pattern)) {
                state = State.TRANSFER_ERROR;
                return;
            }
        }
        reset();
    }

    public IPatternStorage getPatternStorage() {
        for (Direction dir : Direction.values()) {
            if (world.getBlockEntity(pos.offset(dir)) instanceof IPatternStorage storage) {
                return storage;
            }
        }
        return null;
    }

    public boolean saveToDisk(ItemStack stack) {
        ItemStack disk = diskSlot.get();
        if (stack == null || !(disk.getItem() instanceof ItemCrystalMemory memory)) {
            return false;
        }
        memory.writecontentsTag(disk, stack);
        diskSlot.onChanged();
        return true;
    }

    /** The window's buttons: {@code delete} drops the result, {@code save} stores it. */
    @Override
    public void onGuiEvent(PlayerEntity player, String event) {
        if ("delete".equals(event)) {
            reset();
        } else if ("save".equals(event) && progress >= DURATION) {
            record();
        }
        markDirty();
    }

    private void refreshInfo() {
        if (!pattern.isEmpty()) {
            patternUu = UuGraph.getInBuckets(pattern);
        }
    }

    public int getPercentageDone() {
        return 100 * progress / DURATION;
    }

    public boolean isDone() {
        return progress >= DURATION;
    }

    public State getState() {
        return state;
    }

    public ItemStack getPattern() {
        return pattern;
    }

    @Override
    public double getGuiValue(String name) {
        // the bar sweeps once for every per cent, as GuiScanner drew it
        return "progress".equals(name) ? (100.0 * progress % DURATION) / DURATION : super.getGuiValue(name);
    }

    @Override
    public double getGuiAmount(String name) {
        return switch (name) {
            case "percent" -> getPercentageDone();
            case "patternUuMicro" -> Math.round(patternUu * 1.0E6);
            default -> super.getGuiAmount(name);
        };
    }

    @Override
    public boolean getGuiCondition(String name) {
        if (name.startsWith("state_")) {
            return state.name().equalsIgnoreCase(name.substring(6));
        }
        return switch (name) {
            case "canDelete" -> state == State.COMPLETED || state == State.TRANSFER_ERROR || state == State.FAILED;
            case "canSave" -> state == State.COMPLETED || state == State.TRANSFER_ERROR;
            case "showResult" -> state == State.COMPLETED || state == State.TRANSFER_ERROR;
            default -> super.getGuiCondition(name);
        };
    }

    @Override
    public ItemStack getGuiItem(String name) {
        return "pattern".equals(name) ? pattern : super.getGuiItem(name);
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.readNbt(nbt, lookup);
        progress = nbt.getInt("progress");
        currentStack = readItem(nbt.getString("currentStack"));
        pattern = readItem(nbt.getString("pattern"));
        int stateIdx = nbt.getInt("state");
        state = stateIdx >= 0 && stateIdx < State.values().length ? State.values()[stateIdx] : State.IDLE;
        refreshInfo();
    }

    @Override
    public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.writeNbt(nbt, lookup);
        nbt.putInt("progress", progress);
        if (!currentStack.isEmpty()) {
            nbt.putString("currentStack", Registries.ITEM.getId(currentStack.getItem()).toString());
        }
        if (!pattern.isEmpty()) {
            nbt.putString("pattern", Registries.ITEM.getId(pattern.getItem()).toString());
        }
        nbt.putInt("state", state.ordinal());
    }

    static ItemStack readItem(String id) {
        Identifier identifier = id.isEmpty() ? null : Identifier.tryParse(id);
        return identifier == null || !Registries.ITEM.containsId(identifier) ? ItemStack.EMPTY : new ItemStack(Registries.ITEM.get(identifier));
    }
}
