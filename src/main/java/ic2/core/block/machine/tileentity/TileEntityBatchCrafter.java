package ic2.core.block.machine.tileentity;

import ic2.api.upgrade.IUpgradableBlock;
import ic2.api.upgrade.IUpgradeItem;
import ic2.api.upgrade.UpgradableProperty;
import ic2.core.block.invslot.InvSlot;
import ic2.core.block.invslot.InvSlotDischarge;
import ic2.core.block.invslot.InvSlotOutput;
import ic2.core.block.invslot.InvSlotUpgrade;
import ic2.core.init.IC2Blocks;
import ic2.core.util.StackBalance;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Fabric port of TileEntityBatchCrafter - crafts one recipe over and over.
 *
 * <p>The recipe is set in a 3x3 grid of ghost slots; under it nine ingredient slots, one per grid
 * cell, take only what fits that cell of the recipe (hoppers and pipes may fill them). Every craft
 * takes 40 ticks at 2 EU/t (80 EU), uses one item of each ingredient and puts the result into the
 * output slot; containers such as empty buckets go back into their ingredient slot or into the
 * nine container slots. Equal ingredients are kept balanced between the slots that need them. It
 * stores 20000 EU at tier 1 and takes overclocker, transformer, energy storage, ejector and
 * pulling upgrades.
 */
public class TileEntityBatchCrafter extends TileEntityElectricMachine implements IUpgradableBlock, InvSlotUpgrade.Listener,
        ic2.core.gui.IRecipeTransferTarget {
    private static final Set<UpgradableProperty> UPGRADES = EnumSet.of(
            UpgradableProperty.Processing, UpgradableProperty.Transformer, UpgradableProperty.EnergyStorage,
            UpgradableProperty.ItemConsuming, UpgradableProperty.ItemProducing);
    public static final int DEFAULT_TIER = 1;
    public static final int DEFAULT_ENERGY_CONSUME = 2;
    public static final int DEFAULT_OPERATION_LENGTH = 40;
    public static final int DEFAULT_ENERGY_STORAGE = 20000;

    /** The recipe, as ghost stacks of one item each. */
    public final ItemStack[] craftingGrid = new ItemStack[9];
    public final InvSlot[] ingredientsRow = new InvSlot[craftingGrid.length];
    public final InvSlotOutput craftingOutput;
    public final InvSlotOutput containerOutput;
    public final InvSlotUpgrade upgradeSlot;
    public final InvSlotDischarge dischargeSlot;

    protected Pattern recipe;
    protected boolean canCraft = false;
    protected boolean newChange = true;
    protected boolean attemptToBalance = false;
    /** Set while the machine moves its own items, which must not count as a change from outside. */
    private boolean ticking;
    public ItemStack recipeOutput = ItemStack.EMPTY;
    public int energyConsume = DEFAULT_ENERGY_CONSUME;
    public int operationLength = DEFAULT_OPERATION_LENGTH;
    public int operationsPerTick = 1;
    protected short progress = 0;
    protected float guiProgress = 0.0f;

    public TileEntityBatchCrafter(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type == null ? IC2Blocks.BATCH_CRAFTER_BE : type, pos, state, DEFAULT_ENERGY_STORAGE, DEFAULT_TIER);
        Arrays.fill(craftingGrid, ItemStack.EMPTY);
        this.dischargeSlot = new InvSlotDischarge(this, "discharge", DEFAULT_TIER);
        this.craftingOutput = new InvSlotOutput(this, "output", 1, InvSlot.InvSide.SIDE);
        this.containerOutput = new InvSlotOutput(this, "containersOut", craftingGrid.length, InvSlot.InvSide.NOTSIDE);
        this.upgradeSlot = new InvSlotUpgrade(this, "upgrade", 4);
        for (int i = 0; i < ingredientsRow.length; i++) {
            ingredientsRow[i] = new IngredientSlot(i);
        }
        onUpgradesChanged();
    }

    public TileEntityBatchCrafter(BlockPos pos, BlockState state) {
        this(null, pos, state);
    }

    /** An ingredient slot: takes only what keeps the recipe matching in its cell of the grid. */
    private final class IngredientSlot extends InvSlot {
        private final int slot;

        IngredientSlot(int slot) {
            super(TileEntityBatchCrafter.this, "ingredient[" + slot + "]", Access.I, 1);
            this.slot = slot;
        }

        @Override
        public boolean accepts(ItemStack ingredient) {
            Pattern current = world != null && world.isClient ? findRecipe() : recipe;
            if (current == null || ingredient.isEmpty()) {
                return false;
            }
            ItemStack recipeStack = craftingGrid[slot];
            try {
                craftingGrid[slot] = ingredient;
                return current.matches(craftingGrid, world);
            } finally {
                craftingGrid[slot] = recipeStack;
            }
        }

        @Override
        public void onChanged() {
            super.onChanged();
            newChange = true;
        }
    }

    // ---------------------------------------------------------------- recipes

    /** A recipe the grid can be set to; the assembly bench has its own kind. */
    protected interface Pattern {
        /** Whether the 3x3 grid (row by row) shows this recipe. */
        boolean matches(ItemStack[] grid, World world);

        ItemStack craft(ItemStack[] grid, World world);

        /** What stays behind in each of the nine cells, e.g. an empty bucket. */
        ItemStack[] getRemainders(ItemStack[] grid, World world);
    }

    /** A vanilla (or data pack) crafting recipe, which is what 1.12.2's CraftingManager found. */
    private record VanillaPattern(RecipeEntry<CraftingRecipe> entry) implements Pattern {
        @Override
        public boolean matches(ItemStack[] grid, World world) {
            return entry.value().matches(input(grid), world);
        }

        @Override
        public ItemStack craft(ItemStack[] grid, World world) {
            return entry.value().craft(input(grid), world.getRegistryManager());
        }

        @Override
        public ItemStack[] getRemainders(ItemStack[] grid, World world) {
            CraftingRecipeInput.Positioned positioned = positioned(grid);
            CraftingRecipeInput input = positioned.input();
            DefaultedList<ItemStack> remainders = entry.value().getRemainder(input);
            ItemStack[] ret = new ItemStack[grid.length];
            Arrays.fill(ret, ItemStack.EMPTY);
            for (int y = 0; y < input.getHeight(); y++) {
                for (int x = 0; x < input.getWidth(); x++) {
                    int index = x + y * input.getWidth();
                    if (index < remainders.size()) {
                        ret[x + positioned.left() + (y + positioned.top()) * 3] = remainders.get(index);
                    }
                }
            }
            return ret;
        }
    }

    protected static CraftingRecipeInput.Positioned positioned(ItemStack[] grid) {
        List<ItemStack> stacks = new ArrayList<>(grid.length);
        for (ItemStack stack : grid) {
            stacks.add(stack == null ? ItemStack.EMPTY : stack);
        }
        return CraftingRecipeInput.createPositioned(3, 3, stacks);
    }

    protected static CraftingRecipeInput input(ItemStack[] grid) {
        return positioned(grid).input();
    }

    /** Looks the grid up among the crafting recipes. */
    protected Pattern findRecipe() {
        if (world == null) {
            return null;
        }
        Optional<RecipeEntry<CraftingRecipe>> match = world.getRecipeManager()
                .getFirstMatch(RecipeType.CRAFTING, input(craftingGrid), world);
        return match.<Pattern>map(VanillaPattern::new).orElse(null);
    }

    /** The recipe grid changed: looks the recipe up again and shows its result. */
    public void matrixChange(int slot) {
        if (world == null) {
            return;
        }
        if (recipe == null || !recipe.matches(craftingGrid, world)) {
            recipe = findRecipe();
        }
        recipeOutput = recipe != null ? recipe.craft(craftingGrid, world) : ItemStack.EMPTY;
        newChange = true;
    }

    /**
     * A recipe viewer's "+", port of TransferHandlerBatchCrafter: the recipe becomes the ghost
     * grid, the first item each cell may take; no items move.
     */
    @Override
    public boolean transferRecipe(net.minecraft.server.network.ServerPlayerEntity player, List<List<ItemStack>> cells, boolean max) {
        for (int i = 0; i < craftingGrid.length; i++) {
            List<ItemStack> cell = i < cells.size() ? cells.get(i) : List.of();
            craftingGrid[i] = cell.isEmpty() ? ItemStack.EMPTY : cell.get(0).copyWithCount(1);
        }
        for (int i = 0; i < craftingGrid.length; i++) {
            matrixChange(i);
        }
        markDirty();
        return true;
    }

    @Override
    public boolean needsItems() {
        return false;
    }

    @Override
    public ItemStack[] getHologramSlots(String name) {
        return "grid".equals(name) ? craftingGrid : super.getHologramSlots(name);
    }

    @Override
    public void onHologramChanged(String name, int index) {
        if ("grid".equals(name)) {
            matrixChange(index);
        }
    }

    @Override
    public ItemStack getGuiItem(String name) {
        return "recipeOutput".equals(name) ? recipeOutput : super.getGuiItem(name);
    }

    // ---------------------------------------------------------------- operation

    @Override
    public void onLoad() {
        super.onLoad();
        if (world != null && !world.isClient) {
            onUpgradesChanged();
            matrixChange(-1);
        }
    }

    /** Items put in from outside are balanced over the ingredient slots on the next tick. */
    @Override
    public void onInventoryChanged() {
        super.onInventoryChanged();
        if (!ticking) {
            attemptToBalance = true;
        }
    }

    @Override
    public void tick() {
        if (world == null || world.isClient) {
            return;
        }
        ticking = true;
        try {
            update();
        } finally {
            ticking = false;
        }
    }

    private void update() {
        drainDischargeSlot(dischargeSlot);
        boolean needsInvUpdate = false;

        if (attemptToBalance) {
            if (!ingredientsEmpty()) {
                needsInvUpdate |= !balanceIngredients(List.of()).changed().isEmpty();
            }
            attemptToBalance = false;
        }

        if (newChange) {
            canCraft = canCraft();
            newChange = false;
        }

        if (canCraft && craftingOutput.canAdd(recipeOutput) && useEnergy(energyConsume)) {
            setActive(true);
            if (++progress >= operationLength) {
                doCrafting();
                needsInvUpdate = true;
                newChange = true;
                progress = 0;
            }
        } else {
            if (!hasRecipe()) {
                progress = 0;
            }
            setActive(false);
        }

        needsInvUpdate |= upgradeSlot.tickNoMark();
        guiProgress = (float) progress / operationLength;
        if (needsInvUpdate) {
            markDirty();
        }
    }

    private boolean ingredientsEmpty() {
        for (InvSlot slot : ingredientsRow) {
            if (!slot.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /** Spreads equal ingredients evenly over the slots that take them, adding the given stacks. */
    public StackBalance.Result balanceIngredients(List<ItemStack> additional) {
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < ingredientsRow.length; i++) {
            slots.add(i);
        }
        return StackBalance.balance(new StackBalance.Grid() {
            @Override
            public int size() {
                return ingredientsRow.length;
            }

            @Override
            public ItemStack get(int index) {
                return ingredientsRow[index].get();
            }

            @Override
            public void set(int index, ItemStack stack) {
                ingredientsRow[index].put(stack);
            }
        }, (stack, slot) -> ingredientsRow[slot].accepts(stack), slots, additional);
    }

    public boolean hasRecipe() {
        return recipe != null;
    }

    public boolean canCraft() {
        if (!hasRecipe()) {
            return false;
        }
        for (int slot = 0; slot < craftingGrid.length; slot++) {
            if (!craftingGrid[slot].isEmpty() && !ingredientsRow[slot].accepts(ingredientsRow[slot].get())) {
                return false;
            }
        }
        return true;
    }

    protected void doCrafting() {
        for (int operation = 0; operation < operationsPerTick; operation++) {
            List<ItemStack> outputs = Collections.singletonList(recipeOutput);
            for (ItemStack stack : upgradeSlot) {
                if (stack.getItem() instanceof IUpgradeItem upgrade) {
                    upgrade.onProcessEnd(stack, this, outputs);
                }
            }

            craft();
            // 1.12.2 went on without looking at the ingredients again, crafting from nothing
            // when overclocked; the next operation needs them in place
            if (!hasRecipe() || !craftingOutput.canAdd(recipeOutput) || !canCraft()) {
                break;
            }
        }
    }

    protected void craft() {
        craftingOutput.add(recipeOutput.copy());

        ItemStack[] ingredients = new ItemStack[ingredientsRow.length];
        for (int i = 0; i < ingredients.length; i++) {
            ingredients[i] = ingredientsRow[i].get();
        }
        ItemStack[] stacks = recipe.getRemainders(ingredients, world);

        for (int slot = 0; slot < ingredientsRow.length; slot++) {
            ItemStack oldStack = ingredientsRow[slot].get();
            if (!oldStack.isEmpty() && !craftingGrid[slot].isEmpty()) {
                oldStack = oldStack.copyWithCount(oldStack.getCount() - 1);
                ingredientsRow[slot].put(oldStack);
            }

            if (stacks.length > slot && !stacks[slot].isEmpty()) {
                ItemStack newStack = stacks[slot].copy();
                if (oldStack.isEmpty() && ingredientsRow[slot].accepts(newStack)) {
                    ingredientsRow[slot].put(newStack);
                } else if (ItemStack.areItemsAndComponentsEqual(oldStack, newStack)) {
                    ingredientsRow[slot].put(newStack.copyWithCount(newStack.getCount() + oldStack.getCount()));
                } else {
                    storeContainer(newStack);
                }
            }
        }

        for (int i = ingredientsRow.length; i < stacks.length; i++) {
            storeContainer(stacks[i]);
        }
    }

    private void storeContainer(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        if (containerOutput.canAdd(stack)) {
            containerOutput.add(stack);
        } else {
            ItemScatterer.spawn(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
        }
    }

    /** Re-reads speed, power, voltage and buffer from the upgrades, keeping the progress - setOverclockRates. */
    @Override
    public void onUpgradesChanged() {
        double previousProgress = (double) progress / operationLength;
        operationsPerTick = upgradeSlot.getOperationsPerTick(DEFAULT_OPERATION_LENGTH);
        operationLength = upgradeSlot.getOperationLength(DEFAULT_OPERATION_LENGTH);
        energyConsume = upgradeSlot.getEnergyDemand(DEFAULT_ENERGY_CONSUME);
        setTier(upgradeSlot.getTier(DEFAULT_TIER));
        setCapacity(upgradeSlot.getEnergyStorage(DEFAULT_ENERGY_STORAGE, DEFAULT_OPERATION_LENGTH, DEFAULT_ENERGY_CONSUME));
        progress = (short) Math.floor(previousProgress * operationLength + 0.1);
    }

    public short getProgress() {
        return progress;
    }

    @Override
    public int getComparatorOutput() {
        return progress * 15 / operationLength;
    }

    @Override
    public double getGuiValue(String name) {
        return "progress".equals(name) ? guiProgress : super.getGuiValue(name);
    }

    @Override
    public Set<UpgradableProperty> getUpgradableProperties() {
        return UPGRADES;
    }

    @Override
    public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.writeNbt(nbt, registries);
        nbt.putShort("progress", progress);
        NbtList grid = new NbtList();
        for (int i = 0; i < craftingGrid.length; i++) {
            if (!craftingGrid[i].isEmpty()) {
                NbtCompound contentTag = (NbtCompound) craftingGrid[i].encode(registries);
                contentTag.putByte("index", (byte) i);
                grid.add(contentTag);
            }
        }
        nbt.put("grid", grid);
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.readNbt(nbt, registries);
        progress = nbt.getShort("progress");
        Arrays.fill(craftingGrid, ItemStack.EMPTY);
        NbtList grid = nbt.getList("grid", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < grid.size(); i++) {
            NbtCompound contentTag = grid.getCompound(i);
            int index = contentTag.getByte("index");
            if (index >= 0 && index < craftingGrid.length) {
                craftingGrid[index] = ItemStack.fromNbt(registries, contentTag).orElse(ItemStack.EMPTY);
            }
        }
    }
}
