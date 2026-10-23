package ic2.core.block.machine.tileentity;

import ic2.api.recipe.IRecipeInput;
import ic2.api.upgrade.UpgradableProperty;
import ic2.core.init.IC2Blocks;
import ic2.core.recipe.UuRecipe;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Fabric port of TileEntityAssemblyBench - the UU assembly bench: a batch crafter that knows only
 * the UU-matter patterns of {@code uu_recipes.ini} (one UU-matter in the middle of the grid makes
 * 16 stone, and so on). It works like the batch crafter - 40 ticks and 80 EU a craft - but takes
 * no overclockers, only transformer, ejector and pulling upgrades.
 */
public class TileEntityAssemblyBench extends TileEntityBatchCrafter {
    /** Pattern recipes from uu_recipes.ini, filled by {@link ic2.core.init.Rezepte#loadUuRecipes()}. */
    public static final List<UuRecipe> RECIPES = new ArrayList<>();

    public TileEntityAssemblyBench(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type == null ? IC2Blocks.UU_ASSEMBLY_BENCH_BE : type, pos, state);
    }

    public TileEntityAssemblyBench(BlockPos pos, BlockState state) {
        this(null, pos, state);
    }

    /** A UU pattern: every marked cell holds its ingredient, every other cell is empty, nothing is shifted. */
    private record UuPattern(UuRecipe recipe) implements Pattern {
        @Override
        public boolean matches(ItemStack[] grid, World world) {
            for (int y = 0; y < 3; y++) {
                String row = y < recipe.pattern.size() ? recipe.pattern.get(y) : "";
                for (int x = 0; x < 3; x++) {
                    ItemStack stack = grid[x + y * 3] == null ? ItemStack.EMPTY : grid[x + y * 3];
                    char symbol = x < row.length() ? row.charAt(x) : ' ';
                    if (symbol == ' ') {
                        if (!stack.isEmpty()) {
                            return false;
                        }
                    } else {
                        IRecipeInput input = recipe.inputs.get(symbol);
                        if (stack.isEmpty() || input == null || !input.matches(stack)) {
                            return false;
                        }
                    }
                }
            }
            return true;
        }

        @Override
        public ItemStack craft(ItemStack[] grid, World world) {
            return recipe.output.copy();
        }

        @Override
        public ItemStack[] getRemainders(ItemStack[] grid, World world) {
            ItemStack[] ret = new ItemStack[grid.length];
            Arrays.fill(ret, ItemStack.EMPTY);
            return ret;
        }
    }

    @Override
    protected Pattern findRecipe() {
        for (UuRecipe recipe : RECIPES) {
            UuPattern pattern = new UuPattern(recipe);
            if (pattern.matches(craftingGrid, world)) {
                return pattern;
            }
        }
        return null;
    }

    @Override
    public void addInformation(ItemStack stack, List<Text> tooltip) {
        tooltip.add(Text.literal("You probably want the ").append(Text.translatable("block.ic2.replicator")));
    }

    @Override
    public Set<UpgradableProperty> getUpgradableProperties() {
        return EnumSet.of(UpgradableProperty.Transformer, UpgradableProperty.ItemConsuming, UpgradableProperty.ItemProducing);
    }
}
