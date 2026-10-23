package ic2.core.block.machine.tileentity;

import ic2.api.recipe.IPatternStorage;
import ic2.core.block.TileEntityInventory;
import ic2.core.block.invslot.InvSlot;
import ic2.core.block.invslot.InvSlotConsumable;
import ic2.core.init.IC2Blocks;
import ic2.core.item.ItemCrystalMemory;
import ic2.core.ref.TeBlock;
import ic2.core.uu.UuGraph;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;

/**
 * Fabric port of TileEntityPatternStorage: keeps the UU patterns a scanner next to it records,
 * for a replicator next to it to use. Patterns can be moved to and from a crystal memory with
 * the window's buttons, and stay in the item when the block is wrenched.
 */
public class TileEntityPatternStorage extends TileEntityInventory implements IPatternStorage {
    public final InvSlotConsumable diskSlot;
    private final List<ItemStack> patterns = new ArrayList<>();
    public int index;
    public int maxIndex;
    public ItemStack pattern;
    public double patternUu;
    public double patternEu;

    public TileEntityPatternStorage(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type == null ? IC2Blocks.PATTERN_STORAGE_BE : type, pos, state, TeBlock.pattern_storage);
        this.diskSlot = new InvSlotConsumable(this, "SaveSlot", InvSlot.Access.IO, 1, InvSlot.InvSide.ANY) {
            @Override
            public boolean accepts(ItemStack stack) {
                return stack.getItem() instanceof ItemCrystalMemory;
            }
        };
    }

    public TileEntityPatternStorage(BlockPos pos, BlockState state) {
        this(null, pos, state);
    }

    public void readContents(NbtCompound nbt) {
        NbtList list = nbt.getList("patterns", NbtElement.STRING_TYPE);
        for (int i = 0; i < list.size(); i++) {
            ItemStack stack = TileEntityScanner.readItem(list.getString(i));
            if (!stack.isEmpty()) {
                addPattern(stack);
            }
        }
        refreshInfo();
    }

    private void writeContents(NbtCompound nbt) {
        NbtList list = new NbtList();
        for (ItemStack stack : patterns) {
            list.add(NbtString.of(Registries.ITEM.getId(stack.getItem()).toString()));
        }
        nbt.put("patterns", list);
    }

    @Override
    public void onPlaced(ItemStack stack, LivingEntity placer) {
        super.onPlaced(stack, placer);
        readContents(dropData(stack));
    }

    @Override
    protected ItemStack adjustDrop(ItemStack drop, boolean wrench) {
        drop = super.adjustDrop(drop, wrench);
        if (drop != null && (wrench || getDropTeBlock().getDefaultDrop() == TeBlock.DefaultDrop.Self) && !patterns.isEmpty()) {
            editDropData(drop, this::writeContents);
        }
        return drop;
    }

    /** The window's buttons: {@code last}/{@code next} page through, {@code export}/{@code import} use the crystal. */
    @Override
    public void onGuiEvent(PlayerEntity player, String event) {
        switch (event) {
            case "last" -> {
                if (!patterns.isEmpty()) {
                    index = index <= 0 ? patterns.size() - 1 : index - 1;
                    refreshInfo();
                }
            }
            case "next" -> {
                if (!patterns.isEmpty()) {
                    index = index >= patterns.size() - 1 ? 0 : index + 1;
                    refreshInfo();
                }
            }
            case "export" -> {
                ItemStack disk = diskSlot.get();
                if (index >= 0 && index < patterns.size() && disk.getItem() instanceof ItemCrystalMemory memory) {
                    memory.writecontentsTag(disk, patterns.get(index));
                    diskSlot.onChanged();
                }
            }
            case "import" -> {
                ItemStack disk = diskSlot.get();
                if (disk.getItem() instanceof ItemCrystalMemory memory) {
                    ItemStack record = memory.readItemStack(disk);
                    if (!record.isEmpty()) {
                        addPattern(record);
                    }
                }
            }
            default -> {
                return;
            }
        }
        markDirty();
    }

    public void refreshInfo() {
        if (index < 0 || index >= patterns.size()) {
            index = 0;
        }
        maxIndex = patterns.size();
        if (patterns.isEmpty()) {
            pattern = null;
        } else {
            pattern = patterns.get(index);
            patternUu = UuGraph.getInBuckets(pattern);
        }
    }

    @Override
    public boolean addPattern(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            throw new IllegalArgumentException("empty stack");
        }
        for (ItemStack stored : patterns) {
            if (stored.isOf(stack.getItem())) {
                return false;
            }
        }
        patterns.add(stack.copyWithCount(1));
        refreshInfo();
        markDirty();
        return true;
    }

    @Override
    public List<ItemStack> getPatterns() {
        return patterns;
    }

    @Override
    public double getGuiAmount(String name) {
        return switch (name) {
            case "index" -> Math.min(index + 1, maxIndex);
            case "count" -> maxIndex;
            case "patternUuMicro" -> pattern == null ? 0 : Math.round(patternUu * 1.0E6);
            default -> super.getGuiAmount(name);
        };
    }

    @Override
    public boolean getGuiCondition(String name) {
        return "hasPattern".equals(name) ? pattern != null : super.getGuiCondition(name);
    }

    @Override
    public ItemStack getGuiItem(String name) {
        return "pattern".equals(name) && pattern != null ? pattern : super.getGuiItem(name);
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.readNbt(nbt, lookup);
        patterns.clear();
        readContents(nbt);
    }

    @Override
    public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.writeNbt(nbt, lookup);
        writeContents(nbt);
    }
}
