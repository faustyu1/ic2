package ic2.core.block;

import ic2.core.block.invslot.InvSlot;
import ic2.core.gui.DynamicContainer;
import ic2.core.gui.GuiParser;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * Fabric port of ic2.core.block.TileEntityInventory - a block entity whose inventory is made of
 * named {@link InvSlot} groups.
 *
 * <p>The groups are the machine's own model; the flat, index-addressed {@link SidedInventory} view
 * that hoppers and screen handlers need is derived from them by concatenating the groups in
 * registration order. Automation access follows each group's {@link InvSlot.Access} and
 * {@link InvSlot.InvSide}, which is how the original decided what a hopper may insert or pull.
 */
public abstract class TileEntityInventory extends TileEntityBlock
        implements IInventorySlotHolder<TileEntityInventory>, SidedInventory,
        ExtendedScreenHandlerFactory<BlockPos> {

    private final List<InvSlot> invSlots = new ArrayList<>();

    public TileEntityInventory(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public TileEntityInventory(BlockEntityType<?> type, BlockPos pos, BlockState state, ic2.core.ref.TeBlock teBlock) {
        super(type, pos, state, teBlock);
    }

    @Override
    public void addInventorySlot(InvSlot slot) {
        invSlots.add(slot);
    }

    @Override
    public TileEntityInventory getParent() {
        return this;
    }

    @Override
    public void onInventoryChanged() {
        markDirty();
    }

    /** The slot groups in registration order. */
    public List<InvSlot> getInvSlots() {
        return Collections.unmodifiableList(invSlots);
    }

    /**
     * Name of the guidef file describing this machine's GUI, {@code assets/ic2/guidef/<name>.xml}.
     * Defaults to the block's own name, which is how the original mapped them.
     */
    public String getGuiName() {
        // the block entities are built from a BlockEntityType and do not carry their TeBlock, so
        // the block they belong to is the reliable source of the name
        if (world != null && getCachedState().getBlock() instanceof BlockTileEntity block) {
            return block.teBlock.getGuiName();
        }
        return getTeBlock().getGuiName();
    }

    /**
     * The guidef a particular player sees. The trade-o-mat and the energy-o-mat show their owner a
     * different window than their customers, as the 1.12.2 blocks picked an open or a closed
     * container per player; both sides call this, so what the client knows of the block has to be
     * enough to decide.
     */
    public String getGuiName(PlayerEntity player) {
        return getGuiName();
    }

    /** @return the slot group with that name, or null when the machine has none */
    public InvSlot getInvSlot(String name) {
        for (InvSlot slot : invSlots) {
            if (name.equals(slot.name)) {
                return slot;
            }
        }
        return null;
    }

    /** Whether this machine has a GUI to open, i.e. a guidef file exists for it. */
    public boolean hasGui() {
        return GuiParser.parse(getGuiName()) != null;
    }

    // ---------------------------------------------------------------- screen handler

    @Override
    public BlockPos getScreenOpeningData(net.minecraft.server.network.ServerPlayerEntity player) {
        return pos;
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable(getCachedState().getBlock().getTranslationKey());
    }

    @Override
    public ScreenHandler createMenu(int syncId, net.minecraft.entity.player.PlayerInventory playerInventory,
                                    PlayerEntity player) {
        return new DynamicContainer(syncId, playerInventory, this);
    }

    // ---------------------------------------------------------------- persistence

    @Override
    public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.writeNbt(nbt, registries);

        NbtList slotsTag = new NbtList();
        for (InvSlot slot : invSlots) {
            NbtCompound slotTag = new NbtCompound();
            slotTag.putString("Name", slot.name);
            slot.writeToNbt(slotTag, registries);
            slotsTag.add(slotTag);
        }
        nbt.put("InvSlots", slotsTag);
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.readNbt(nbt, registries);

        NbtList slotsTag = nbt.getList("InvSlots", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < slotsTag.size(); i++) {
            NbtCompound slotTag = slotsTag.getCompound(i);
            InvSlot slot = getInvSlot(slotTag.getString("Name"));
            if (slot != null) {
                slot.readFromNbt(slotTag, registries);
            }
        }
    }

    /** An item the window shows with {@code <itemimage name="...">}; nothing by default. */
    public ItemStack getGuiItem(String name) {
        return ItemStack.EMPTY;
    }

    /**
     * The stacks behind a {@code <slothologram>} or a {@code <slotgrid hologram="true">}: ghost
     * slots showing a copy of what the player clicked into them, as SlotHologramSlot did for the
     * sorting machine's filters and the batch crafter's recipe. They are no part of the inventory,
     * so they neither drop nor take part in automation.
     *
     * @return the array the slots edit in place, or null when the block has no such slots
     */
    public ItemStack[] getHologramSlots(String name) {
        return null;
    }

    /** A hologram slot was edited through the window; called on the server only. */
    public void onHologramChanged(String name, int index) {
    }

    /**
     * What a {@code <craftresult name="...">} slot offers now, e.g. what the industrial workbench's
     * grid crafts. The window refreshes it on the server before every sync.
     */
    public ItemStack getCraftingResult(String name) {
        return ItemStack.EMPTY;
    }

    /**
     * The player took the stack {@link #getCraftingResult} offered; the block uses up the
     * ingredients. Called on the server only.
     */
    public void onCraftingResultTaken(String name, PlayerEntity player, ItemStack stack) {
    }

    /**
     * The comparator signal for how full the slot groups are, upgrade slots left out - what 1.12.2
     * gave for every inventory block: 0 when empty, otherwise 1 to 15.
     */
    protected static int calcRedstoneFromInvSlots(Iterable<InvSlot> invSlots) {
        int space = 0;
        int used = 0;

        for (InvSlot slot : invSlots) {
            if (slot instanceof ic2.core.block.invslot.InvSlotUpgrade) {
                continue;
            }
            int size = slot.size();
            int limit = slot.getStackSizeLimit();
            space += size * limit;

            for (int i = 0; i < size; i++) {
                ItemStack stack = slot.get(i);
                if (!stack.isEmpty()) {
                    used += Math.min(limit, stack.getCount() * limit / stack.getMaxCount());
                }
            }
        }

        return used != 0 && space != 0 ? 1 + used * 14 / space : 0;
    }

    // ---------------------------------------------------------------- flat inventory view

    /** Resolves a flat inventory index to the owning group and the index inside it. */
    private record Position(InvSlot slot, int index) {}

    private Position resolve(int index) {
        for (InvSlot slot : invSlots) {
            if (index < slot.size()) {
                return new Position(slot, index);
            }
            index -= slot.size();
        }
        return null;
    }

    @Override
    public int size() {
        int ret = 0;
        for (InvSlot slot : invSlots) {
            ret += slot.size();
        }
        return ret;
    }

    @Override
    public boolean isEmpty() {
        for (InvSlot slot : invSlots) {
            if (!slot.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getStack(int index) {
        Position position = resolve(index);
        return position == null ? ItemStack.EMPTY : position.slot().get(position.index());
    }

    @Override
    public ItemStack removeStack(int index, int amount) {
        Position position = resolve(index);
        if (position == null) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = position.slot().get(position.index());
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack ret = stack.split(Math.min(amount, stack.getCount()));
        position.slot().put(position.index(), stack.isEmpty() ? ItemStack.EMPTY : stack);
        return ret;
    }

    @Override
    public ItemStack removeStack(int index) {
        Position position = resolve(index);
        if (position == null) {
            return ItemStack.EMPTY;
        }
        ItemStack ret = position.slot().get(position.index());
        position.slot().clear(position.index());
        return ret;
    }

    @Override
    public void setStack(int index, ItemStack stack) {
        Position position = resolve(index);
        if (position != null) {
            position.slot().put(position.index(), stack);
        }
    }

    @Override
    public int getMaxCountPerStack() {
        return 64;
    }

    /** How many items the slot at a flat index holds, as its group allows - one for a reactor slot. */
    public int getMaxCountAt(int index) {
        Position position = resolve(index);
        return position == null ? getMaxCountPerStack() : Math.min(getMaxCountPerStack(), position.slot().getStackSizeLimit());
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return permitsAccess(player)
                && world != null
                && world.getBlockEntity(pos) == this
                && player.squaredDistanceTo(pos.toCenterPos()) <= 64.0;
    }

    /**
     * Whether a player may open this block, which is how the personal chest and the trade-o-mat
     * keep others out. The check runs on the server, both when the block is used and while the
     * screen is open.
     */
    public boolean permitsAccess(PlayerEntity player) {
        return true;
    }

    /** What the player is told when {@link #permitsAccess} refused. */
    public Text getAccessDeniedMessage() {
        return Text.literal("This block is owned by somebody else");
    }

    @Override
    public boolean isValid(int index, ItemStack stack) {
        Position position = resolve(index);
        return position != null && position.slot().accepts(stack);
    }

    @Override
    public void clear() {
        for (InvSlot slot : invSlots) {
            slot.clear();
        }
        markDirty();
    }

    // ---------------------------------------------------------------- automation access

    @Override
    public int[] getAvailableSlots(Direction side) {
        // every slot is offered on every side; canInsert/canExtract decide, as in 1.12.2
        int size = size();
        int[] ret = new int[size];
        for (int i = 0; i < size; i++) {
            ret[i] = i;
        }
        return ret;
    }

    /**
     * A slot group takes the stack from its own side, and from any other side unless a group
     * that prefers that side would take it too - the side is a preference, not a wall, which is
     * how a hopper on the side of a macerator still fills it.
     */
    @Override
    public boolean canInsert(int index, ItemStack stack, Direction side) {
        Position position = resolve(index);
        if (stack.isEmpty() || position == null || !position.slot().canInput() || !position.slot().accepts(stack)) {
            return false;
        }

        InvSlot target = position.slot();
        if (target.preferredSide != InvSlot.InvSide.ANY && target.preferredSide.matches(side)) {
            return true;
        }
        for (InvSlot other : invSlots) {
            if (other != target && other.preferredSide != InvSlot.InvSide.ANY && other.preferredSide.matches(side)
                    && other.canInput() && other.accepts(stack)) {
                return false;
            }
        }
        return true;
    }

    /** Output works the same way: its own side first, any side no other filled output prefers. */
    @Override
    public boolean canExtract(int index, ItemStack stack, Direction side) {
        Position position = resolve(index);
        if (position == null || !position.slot().canOutput()) {
            return false;
        }

        InvSlot target = position.slot();
        boolean correctSide = target.preferredSide.matches(side);
        if (target.preferredSide != InvSlot.InvSide.ANY && correctSide) {
            return true;
        }
        for (InvSlot other : invSlots) {
            if (other != target && (other.preferredSide != InvSlot.InvSide.ANY || !correctSide)
                    && other.preferredSide.matches(side) && other.canOutput() && !other.isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
