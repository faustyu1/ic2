package ic2.core.block.machine.tileentity;

import ic2.api.recipe.IBasicMachineRecipeManager;
import ic2.api.recipe.IRecipeInput;
import ic2.api.recipe.MachineRecipe;
import ic2.api.recipe.MachineRecipeResult;
import ic2.api.recipe.RecipeOutput;
import ic2.api.recipe.Recipes;
import ic2.api.upgrade.UpgradableProperty;
import ic2.core.IC2;
import ic2.core.init.IC2Blocks;
import ic2.core.init.IC2ItemVariants;
import ic2.core.recipe.BasicListRecipeManager;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;

/**
 * Fabric port of TileEntityRecycler.
 *
 * <p>It eats anything, one item every 45 ticks at 1 EU/t, and one time in eight gives a piece of
 * scrap for it. Items on {@code balance/recyclerBlacklist} (glass panes, sticks, snowballs, snow
 * and scaffolding by default) are eaten for nothing; when {@code balance/recyclerWhitelist} is
 * not empty, only what it lists makes scrap.
 */
public class TileEntityRecycler extends TileEntityStandardMachine<IRecipeInput, Collection<ItemStack>, ItemStack> {

    public TileEntityRecycler(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type == null ? IC2Blocks.RECYCLER_BE : type, pos, state, 1, 45, 1, 1);
    }

    public TileEntityRecycler(BlockPos pos, BlockState state) {
        this(IC2Blocks.RECYCLER_BE, pos, state);
    }

    /** Sets up the recycler's recipe manager and empty lists; the lists fill once the config is read. */
    public static void init() {
        if (!(Recipes.recycler instanceof RecyclerRecipeManager)) {
            Recipes.recycler = new RecyclerRecipeManager();
        }
        if (Recipes.recyclerWhitelist == null) {
            Recipes.recyclerWhitelist = new BasicListRecipeManager();
        }
        if (Recipes.recyclerBlacklist == null) {
            Recipes.recyclerBlacklist = new BasicListRecipeManager();
        }
    }

    /** One item in this many turns into scrap. */
    public static int recycleChance() {
        return 8;
    }

    @Override
    protected IBasicMachineRecipeManager getRecipeManager() {
        if (Recipes.recycler == null) {
            init();
        }
        return Recipes.recycler;
    }

    /** Whether the item gives no scrap: on the blacklist, or missing from a non-empty whitelist. */
    public static boolean getIsItemBlacklisted(ItemStack stack) {
        if (Recipes.recyclerWhitelist != null && !Recipes.recyclerWhitelist.isEmpty()) {
            return !Recipes.recyclerWhitelist.contains(stack);
        }
        return Recipes.recyclerBlacklist != null && Recipes.recyclerBlacklist.contains(stack);
    }

    /** The input is always used up; the scrap only comes one time in {@link #recycleChance()}. */
    @Override
    @SuppressWarnings("unchecked")
    protected void operate(MachineRecipeResult<IRecipeInput, Collection<ItemStack>, ItemStack> result) {
        if (result == null) {
            return;
        }

        inputSlot.consume(1);
        if (IC2.random.nextInt(recycleChance()) == 0) {
            for (ItemStack out : (Collection<ItemStack>) result.getRecipe().getOutput()) {
                outputSlot.add(out.copy());
            }
        }
        markDirty();
    }

    public String getStartSoundFile() {
        return "Machines/RecyclerOp.ogg";
    }

    public String getInterruptSoundFile() {
        return "Machines/InterruptOne.ogg";
    }

    @Override
    public Set<UpgradableProperty> getUpgradableProperties() {
        return EnumSet.of(UpgradableProperty.Processing, UpgradableProperty.Transformer,
                UpgradableProperty.EnergyStorage, UpgradableProperty.ItemConsuming,
                UpgradableProperty.ItemProducing);
    }

    /** Every item is a recipe: one of it makes a piece of scrap, or nothing when it is blacklisted. */
    private static class RecyclerRecipeManager implements IBasicMachineRecipeManager {

        private static Collection<ItemStack> getOutput(ItemStack input) {
            if (getIsItemBlacklisted(input)) {
                return Collections.emptyList();
            }
            Item scrap = IC2ItemVariants.get("crafting", "scrap");
            return scrap == null ? Collections.emptyList() : Collections.singletonList(new ItemStack(scrap));
        }

        @Override
        public boolean addRecipe(IRecipeInput input, Collection<ItemStack> output, NbtCompound metadata, boolean replace) {
            return false;
        }

        @Override
        public boolean addRecipe(IRecipeInput input, NbtCompound metadata, boolean replace, ItemStack... outputs) {
            return false;
        }

        @Override
        public RecipeOutput getOutputFor(ItemStack input, boolean adjustInput) {
            if (input == null || input.isEmpty()) {
                return null;
            }
            RecipeOutput ret = new RecipeOutput(null, new ArrayList<>(getOutput(input)));
            if (adjustInput) {
                input.decrement(1);
            }
            return ret;
        }

        @Override
        public MachineRecipeResult<IRecipeInput, Collection<ItemStack>, ItemStack> apply(ItemStack input, boolean acceptTest) {
            if (input == null || input.isEmpty()) {
                return null;
            }
            return new MachineRecipe<IRecipeInput, Collection<ItemStack>>(Recipes.inputFactory.forStack(input, 1), getOutput(input))
                    .getResult(input.copyWithCount(input.getCount() - 1));
        }

        @Override
        public Iterable<? extends MachineRecipe<IRecipeInput, Collection<ItemStack>>> getRecipes() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isIterable() {
            return false;
        }
    }
}
