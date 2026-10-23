package ic2.core.uu;

import java.util.List;

/**
 * Port of {@code ic2.core.uu.ILateRecipeResolver}: a recipe source that needs to know every item
 * the other recipes can make, such as the recycler, which takes anything.
 */
interface ILateRecipeResolver {
    List<RecipeTransformation> getTransformations(Iterable<LeanItemStack> obtainableStacks);
}
