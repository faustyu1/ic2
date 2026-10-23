package ic2.core.block.machine.tileentity;

import ic2.api.item.ICustomDamageItem;
import ic2.core.block.TileEntityInventory;
import ic2.core.block.invslot.InvSlot;
import ic2.core.gui.IRecipeTransferTarget;
import ic2.core.init.IC2Blocks;
import ic2.core.init.IC2Items;
import ic2.core.util.StackBalance;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * Fabric port of TileEntityIndustrialWorkbench - a crafting table that keeps its grid, with 18
 * slots of storage underneath and two small crafting spots for the forge hammer and the cutter.
 *
 * <p>After every craft from the 3x3 grid the grid is refilled from the storage, equal items
 * spread evenly, so the same recipe can be taken again and again. The clear button moves the grid
 * into the storage (or the player's inventory). The left spot crafts what the forge hammer makes
 * of the item next to it - an ingot into a plate - the right one what the cutter makes - a plate
 * into cable; a placed workbench starts with a hammer and a cutter in them. The buttons along the
 * right edge open the machines next to the workbench.
 */
public class TileEntityIndustrialWorkbench extends TileEntityInventory implements IRecipeTransferTarget {
    public static final TagKey<net.minecraft.item.Item> FORGE_HAMMERS = TagKey.of(RegistryKeys.ITEM, Identifier.of("ic2", "tools/forge_hammer"));
    public static final TagKey<net.minecraft.item.Item> WIRE_CUTTERS = TagKey.of(RegistryKeys.ITEM, Identifier.of("ic2", "tools/wire_cutter"));

    public final InvSlot craftingGrid;
    public final InvSlot craftingStorage;
    public final InvSlotCraftingCombo leftCrafting;
    public final InvSlotCraftingCombo rightCrafting;

    public TileEntityIndustrialWorkbench(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type == null ? IC2Blocks.INDUSTRIAL_WORKBENCH_BE : type, pos, state);
        this.craftingGrid = new InvSlot(this, "crafting", InvSlot.Access.NONE, 9);
        this.craftingStorage = new InvSlot(this, "craftingStorage", InvSlot.Access.I, 18);
        this.leftCrafting = new InvSlotCraftingCombo("left", FORGE_HAMMERS);
        this.rightCrafting = new InvSlotCraftingCombo("right", WIRE_CUTTERS);
    }

    public TileEntityIndustrialWorkbench(BlockPos pos, BlockState state) {
        this(null, pos, state);
    }

    /** A freshly crafted workbench comes with its tools; one that was placed before does not. */
    @Override
    public void onPlaced(ItemStack stack, LivingEntity placer) {
        super.onPlaced(stack, placer);
        if (!dropData(stack).contains("PLACED")) {
            leftCrafting.tool.put(new ItemStack(Registries.ITEM.get(Identifier.of("ic2", "forge_hammer"))));
            rightCrafting.tool.put(new ItemStack(IC2Items.CUTTER));
        }
    }

    @Override
    protected ItemStack adjustDrop(ItemStack drop, boolean wrench) {
        drop = super.adjustDrop(drop, wrench);
        if (drop != null && !drop.isEmpty()) {
            editDropData(drop, nbt -> nbt.putBoolean("PLACED", true));
        }
        return drop;
    }

    // ---------------------------------------------------------------- the 3x3 grid

    /** Refills the grid from the storage: each storage stack is spread over the grid slots holding its item. */
    public void rebalance() {
        if (craftingGrid.isEmpty()) {
            return;
        }

        StackBalance.Grid grid = new StackBalance.Grid() {
            @Override
            public int size() {
                return craftingGrid.size();
            }

            @Override
            public ItemStack get(int index) {
                return craftingGrid.get(index);
            }

            @Override
            public void set(int index, ItemStack stack) {
                craftingGrid.put(index, stack);
            }
        };

        boolean changed = false;
        for (int index = 0; index < craftingStorage.size(); index++) {
            if (craftingStorage.isEmpty(index)) {
                continue;
            }
            StackBalance.Result result = StackBalance.balance(grid, List.of(craftingStorage.get(index)));
            if (!result.changed().isEmpty()) {
                changed = true;
                craftingStorage.put(index, result.leftOvers().isEmpty() ? ItemStack.EMPTY : result.leftOvers().get(0));
            }
        }
        if (changed) {
            markDirty();
        }
    }

    private static int getPossible(int max, ItemStack existing, ItemStack in) {
        int amount = Math.min(max, in.isStackable() ? in.getMaxCount() : 1);
        if (!existing.isEmpty()) {
            if (!ItemStack.areItemsAndComponentsEqual(existing, in)) {
                return 0;
            }
            amount -= existing.getCount();
        }
        return Math.min(amount, in.getCount());
    }

    private static ItemStack transfer(InvSlot slot, ItemStack gridItem, boolean allowEmpty) {
        for (int index = 0; index < slot.size(); index++) {
            ItemStack stack = slot.get(index);
            int amount = getPossible(slot.getStackSizeLimit(), stack, gridItem);
            if (amount < 1) {
                continue;
            }
            if (stack.isEmpty()) {
                if (!allowEmpty) {
                    continue;
                }
                slot.put(index, gridItem.copyWithCount(amount));
            } else {
                slot.put(index, stack.copyWithCount(stack.getCount() + amount));
            }
            gridItem = gridItem.copyWithCount(gridItem.getCount() - amount);
            if (gridItem.isEmpty()) {
                break;
            }
        }
        return gridItem;
    }

    /** The clear button: the grid goes into the storage, what does not fit to the player. */
    public void clear(PlayerEntity player) {
        for (int index = 0; index < craftingGrid.size(); index++) {
            if (craftingGrid.isEmpty(index)) {
                continue;
            }
            ItemStack stack = craftingGrid.get(index);
            for (int pass = 0; pass < 2 && !stack.isEmpty(); pass++) {
                stack = transfer(craftingStorage, stack, pass == 1);
            }
            if (!stack.isEmpty() && player != null) {
                stack = stack.copy();
                player.getInventory().insertStack(stack);
            }
            // 1.12.2 put what was left into the first grid slot, overwriting it
            craftingGrid.put(index, stack.isEmpty() ? ItemStack.EMPTY : stack);
        }
    }

    private List<ItemStack> gridStacks() {
        List<ItemStack> ret = new ArrayList<>(craftingGrid.size());
        for (ItemStack stack : craftingGrid) {
            ret.add(stack);
        }
        return ret;
    }

    private Optional<RecipeEntry<CraftingRecipe>> findRecipe(int width, int height, List<ItemStack> stacks) {
        if (world == null) {
            return Optional.empty();
        }
        CraftingRecipeInput input = CraftingRecipeInput.create(width, height, stacks);
        if (input.isEmpty()) {
            return Optional.empty();
        }
        return world.getRecipeManager().getFirstMatch(RecipeType.CRAFTING, input, world);
    }

    private ItemStack craft(int width, int height, List<ItemStack> stacks) {
        CraftingRecipeInput input = CraftingRecipeInput.create(width, height, stacks);
        return findRecipe(width, height, stacks)
                .map(entry -> entry.value().craft(input, world.getRegistryManager()))
                .orElse(ItemStack.EMPTY);
    }

    /**
     * What each cell leaves behind. A crafting tool such as the forge hammer wears down by one use
     * instead of being used up, which is what its container item was in 1.12.2.
     */
    private ItemStack[] remainders(RecipeEntry<CraftingRecipe> entry, int width, int height, List<ItemStack> stacks) {
        CraftingRecipeInput.Positioned positioned = CraftingRecipeInput.createPositioned(width, height, stacks);
        CraftingRecipeInput input = positioned.input();
        DefaultedList<ItemStack> remainders = entry.value().getRemainder(input);
        ItemStack[] ret = new ItemStack[stacks.size()];
        Arrays.fill(ret, ItemStack.EMPTY);
        for (int y = 0; y < input.getHeight(); y++) {
            for (int x = 0; x < input.getWidth(); x++) {
                int index = x + y * input.getWidth();
                int cell = x + positioned.left() + (y + positioned.top()) * width;
                if (index < remainders.size()) {
                    ret[cell] = remainders.get(index);
                }
            }
        }
        for (int i = 0; i < ret.length; i++) {
            ItemStack used = stacks.get(i);
            if (ret[i].isEmpty() && !used.isEmpty() && used.getItem() instanceof ICustomDamageItem tool) {
                ItemStack worn = used.copyWithCount(1);
                ret[i] = tool.applyCustomDamage(worn, 1, null) ? worn : ItemStack.EMPTY;
            }
        }
        return ret;
    }

    /** Takes one item from each used cell and puts back what the recipe leaves. */
    private void consume(List<InvSlot> cells, int width, int height, PlayerEntity player) {
        List<ItemStack> stacks = new ArrayList<>();
        for (InvSlot cell : cells) {
            stacks.add(cell.get());
        }
        Optional<RecipeEntry<CraftingRecipe>> entry = findRecipe(width, height, stacks);
        if (entry.isEmpty()) {
            return;
        }
        ItemStack[] left = remainders(entry.get(), width, height, stacks);

        for (int i = 0; i < cells.size(); i++) {
            InvSlot cell = cells.get(i);
            ItemStack stack = cell.get();
            if (!stack.isEmpty()) {
                stack = stack.copyWithCount(stack.getCount() - 1);
                cell.put(stack);
            }
            ItemStack remainder = left[i];
            if (remainder.isEmpty()) {
                continue;
            }
            if (stack.isEmpty()) {
                cell.put(remainder);
            } else if (ItemStack.areItemsAndComponentsEqual(stack, remainder)) {
                cell.put(remainder.copyWithCount(remainder.getCount() + stack.getCount()));
            } else if (player != null && !player.getInventory().insertStack(remainder)) {
                player.dropItem(remainder, false);
            }
        }
    }

    // ---------------------------------------------------------------- result slots

    @Override
    public ItemStack getCraftingResult(String name) {
        return switch (name) {
            case "main" -> craft(3, 3, gridStacks());
            case "left" -> leftCrafting.getOutputStack();
            case "right" -> rightCrafting.getOutputStack();
            default -> ItemStack.EMPTY;
        };
    }

    @Override
    public void onCraftingResultTaken(String name, PlayerEntity player, ItemStack stack) {
        if (world != null) {
            stack.onCraftByPlayer(world, player, stack.getCount());
        }
        switch (name) {
            case "main" -> {
                List<InvSlot> cells = new ArrayList<>();
                for (int i = 0; i < craftingGrid.size(); i++) {
                    cells.add(new CellView(craftingGrid, i));
                }
                consume(cells, 3, 3, player);
                rebalance();
            }
            case "left" -> leftCrafting.consume(player);
            case "right" -> rightCrafting.consume(player);
            default -> { }
        }
    }

    /**
     * Crafts the result of a slot for a player without a window, as taking it from the result
     * slot does.
     *
     * @return what was crafted, or nothing when the slot offers nothing
     */
    public ItemStack craftResult(String name, PlayerEntity player) {
        ItemStack result = getCraftingResult(name);
        if (!result.isEmpty()) {
            onCraftingResultTaken(name, player, result);
        }
        return result;
    }

    /** One cell of a slot group, seen as a group of its own so the grid and the combos share {@link #consume}. */
    private static final class CellView extends InvSlot {
        private final InvSlot slot;
        private final int index;

        CellView(InvSlot slot, int index) {
            super(1);
            this.slot = slot;
            this.index = index;
        }

        @Override
        public ItemStack get(int i) {
            return slot.get(index);
        }

        @Override
        public void put(int i, ItemStack content) {
            slot.put(index, content);
        }
    }

    // ---------------------------------------------------------------- neighbours and buttons

    /** The block entity on a side if it has a window to open. */
    private TileEntityInventory neighbourWithGui(Direction side) {
        if (world == null) {
            return null;
        }
        BlockEntity neighbour = world.getBlockEntity(pos.offset(side));
        return neighbour instanceof TileEntityInventory inventory && inventory.hasGui() ? inventory : null;
    }

    @Override
    public ItemStack getGuiItem(String name) {
        if (name.startsWith("side_")) {
            TileEntityInventory neighbour = neighbourWithGui(Direction.byId(Integer.parseInt(name.substring(5))));
            return neighbour == null ? ItemStack.EMPTY : neighbour.getPickStack();
        }
        return super.getGuiItem(name);
    }

    /** {@code gui_<id>}: a machine with a window is on that side. */
    @Override
    public boolean getGuiCondition(String name) {
        if (name.startsWith("gui_")) {
            return neighbourWithGui(Direction.byId(Integer.parseInt(name.substring(4)))) != null;
        }
        return super.getGuiCondition(name);
    }

    /** {@code clear} empties the grid, {@code open:<id>} opens the machine on that side. */
    @Override
    public void onGuiEvent(PlayerEntity player, String event) {
        if ("clear".equals(event)) {
            clear(player);
        } else if (event.startsWith("open:")) {
            try {
                TileEntityInventory neighbour = neighbourWithGui(Direction.byId(Integer.parseInt(event.substring(5))));
                if (neighbour == null || player == null) {
                    return;
                }
                if (!neighbour.permitsAccess(player)) {
                    player.sendMessage(neighbour.getAccessDeniedMessage(), false);
                    return;
                }
                player.openHandledScreen(neighbour);
            } catch (NumberFormatException e) {
                // a malformed event changes nothing
            }
        }
    }

    // ---------------------------------------------------------------- recipe transfer

    /**
     * A recipe viewer's "+", port of TransferHandlerIndustrialWorkbench: a two-item recipe with a
     * forge hammer or a cutter goes into that tool's spot, anything else into the grid. The items
     * come from the storage first, then from the player's inventory; what was in the way goes back
     * the same way the clear button sends it.
     */
    @Override
    public boolean transferRecipe(ServerPlayerEntity player, List<List<ItemStack>> cells, boolean max) {
        List<List<ItemStack>> used = cells.stream().filter(cell -> !cell.isEmpty()).toList();
        if (used.size() == 2 && (fillCombo(leftCrafting, FORGE_HAMMERS, used, player, max)
                || fillCombo(rightCrafting, WIRE_CUTTERS, used, player, max))) {
            return true;
        }

        // nothing changes unless one craft's worth is there, counting what the grid already holds
        List<Source> all = sources(player);
        for (int i = 0; i < craftingGrid.size(); i++) {
            int index = i;
            all.add(new Source(() -> craftingGrid.get(index), stack -> craftingGrid.put(index, stack)));
        }
        if (!available(all, used)) {
            return false;
        }

        clear(player);
        return fill(sources(player), cells, i -> craftingGrid.get(i), (i, stack) -> craftingGrid.put(i, stack), max ? 64 : 1) > 0;
    }

    private boolean fillCombo(InvSlotCraftingCombo combo, TagKey<net.minecraft.item.Item> toolTag,
                              List<List<ItemStack>> used, PlayerEntity player, boolean max) {
        int toolIndex = used.get(0).stream().anyMatch(stack -> stack.isIn(toolTag)) ? 0
                : used.get(1).stream().anyMatch(stack -> stack.isIn(toolTag)) ? 1 : -1;
        if (toolIndex < 0) {
            return false;
        }

        List<Source> sources = sources(player);
        sources.add(new Source(() -> combo.tool.get(), stack -> combo.tool.put(stack)));
        sources.add(new Source(() -> combo.input.get(), stack -> combo.input.put(stack)));
        List<List<ItemStack>> wanted = List.of(used.get(toolIndex), used.get(1 - toolIndex));
        if (!available(sources, wanted) || craft(2, 1, List.of(first(sources, wanted.get(0)), first(sources, wanted.get(1)))).isEmpty()) {
            // not there, or not a recipe of this spot: the grid takes it
            return false;
        }

        // the tool and the item that were there make room, then the recipe is fetched
        stash(combo.tool, player);
        stash(combo.input, player);
        // the tool only wears, so one does for every craft; the item fills up
        List<Source> from = sources(player);
        return fill(from, wanted.subList(0, 1), i -> combo.tool.get(), (i, stack) -> combo.tool.put(stack), 1) > 0
                & fill(from, wanted.subList(1, 2), i -> combo.input.get(), (i, stack) -> combo.input.put(stack), max ? 64 : 1) > 0;
    }

    /** Moves a slot's content into the storage, or to the player when it does not fit. */
    private void stash(InvSlot slot, PlayerEntity player) {
        ItemStack stack = slot.get();
        for (int pass = 0; pass < 2 && !stack.isEmpty(); pass++) {
            stack = transfer(craftingStorage, stack, pass == 1);
        }
        if (!stack.isEmpty() && player != null) {
            stack = stack.copy();
            player.getInventory().insertStack(stack);
        }
        slot.put(stack.isEmpty() ? ItemStack.EMPTY : stack);
    }

    private static ItemStack first(List<Source> sources, List<ItemStack> cell) {
        int found = find(sources, new int[sources.size()], cell, ItemStack.EMPTY);
        return found < 0 ? ItemStack.EMPTY : sources.get(found).get().copyWithCount(1);
    }

    /** Where the items for a recipe come from: the storage, then the player's main inventory. */
    private List<Source> sources(PlayerEntity player) {
        List<Source> sources = new ArrayList<>();
        for (int i = 0; i < craftingStorage.size(); i++) {
            int index = i;
            sources.add(new Source(() -> craftingStorage.get(index), stack -> craftingStorage.put(index, stack)));
        }
        if (player != null) {
            PlayerInventory inventory = player.getInventory();
            for (int i = 0; i < inventory.main.size(); i++) {
                int index = i;
                sources.add(new Source(() -> inventory.getStack(index), stack -> inventory.setStack(index, stack)));
            }
        }
        return sources;
    }

    /** Whether the sources hold one of each wanted item at once. */
    private static boolean available(List<Source> sources, List<List<ItemStack>> wanted) {
        int[] taken = new int[sources.size()];
        for (List<ItemStack> cell : wanted) {
            int found = find(sources, taken, cell, ItemStack.EMPTY);
            if (found < 0) {
                return false;
            }
            taken[found]++;
        }
        return true;
    }

    /**
     * Fills the cells one item per round, a round only when every cell can take its item.
     *
     * @return the number of rounds done
     */
    static int fill(List<Source> sources, List<List<ItemStack>> cells, java.util.function.IntFunction<ItemStack> target,
                    java.util.function.BiConsumer<Integer, ItemStack> setTarget, int rounds) {
        int done = 0;
        for (; done < rounds; done++) {
            int[] taken = new int[sources.size()];
            int[] picks = new int[cells.size()];
            boolean complete = true;
            for (int i = 0; i < cells.size() && complete; i++) {
                picks[i] = -1;
                if (cells.get(i).isEmpty()) {
                    continue;
                }
                ItemStack there = target.apply(i);
                if (!there.isEmpty() && there.getCount() >= there.getMaxCount()) {
                    complete = false;
                    break;
                }
                picks[i] = find(sources, taken, cells.get(i), there);
                if (picks[i] < 0) {
                    complete = false;
                } else {
                    taken[picks[i]]++;
                }
            }
            if (!complete) {
                break;
            }

            for (int i = 0; i < cells.size(); i++) {
                if (picks[i] < 0) {
                    continue;
                }
                Source source = sources.get(picks[i]);
                ItemStack from = source.get();
                ItemStack there = target.apply(i);
                setTarget.accept(i, there.isEmpty() ? from.copyWithCount(1) : there.copyWithCount(there.getCount() + 1));
                source.set(from.getCount() > 1 ? from.copyWithCount(from.getCount() - 1) : ItemStack.EMPTY);
            }
        }
        return done;
    }

    /** A source still holding an item the cell takes and that stacks with what the cell has; -1 for none. */
    private static int find(List<Source> sources, int[] taken, List<ItemStack> cell, ItemStack there) {
        for (int s = 0; s < sources.size(); s++) {
            ItemStack stack = sources.get(s).get();
            if (stack.getCount() - taken[s] <= 0 || !IRecipeTransferTarget.matches(stack, cell)
                    || (!there.isEmpty() && !ItemStack.areItemsAndComponentsEqual(there, stack))) {
                continue;
            }
            return s;
        }
        return -1;
    }

    /** A stack somewhere the recipe can take items from. */
    record Source(java.util.function.Supplier<ItemStack> getter, java.util.function.Consumer<ItemStack> setter) {
        ItemStack get() {
            return getter.get();
        }

        void set(ItemStack stack) {
            setter.accept(stack);
        }
    }

    // ---------------------------------------------------------------- the tool spots

    /**
     * A crafting tool next to an item, crafted as a two-slot shapeless recipe - the forge hammer
     * or the cutter combination of the original.
     */
    public final class InvSlotCraftingCombo {
        public final InvSlot input;
        public final InvSlot tool;

        InvSlotCraftingCombo(String name, TagKey<net.minecraft.item.Item> toolTag) {
            this.input = new InvSlot(TileEntityIndustrialWorkbench.this, name + "Input", InvSlot.Access.I, 1) {
                @Override
                public boolean accepts(ItemStack stack) {
                    return !stack.isEmpty() && !craft(2, 1, List.of(tool.get(), stack)).isEmpty();
                }
            };
            this.tool = new InvSlot(TileEntityIndustrialWorkbench.this, name + "Tool", InvSlot.Access.I, 1) {
                @Override
                public boolean accepts(ItemStack stack) {
                    return stack.isIn(toolTag);
                }
            };
        }

        public ItemStack getOutputStack() {
            if (input.isEmpty() && tool.isEmpty()) {
                return ItemStack.EMPTY;
            }
            return craft(2, 1, List.of(tool.get(), input.get()));
        }

        void consume(PlayerEntity player) {
            TileEntityIndustrialWorkbench.this.consume(List.of(tool, input), 2, 1, player);
        }
    }
}
