package ic2.core.uu;

import java.util.List;

/** Port of {@code ic2.core.uu.IRecipeResolver}: a source of recipes for the UU graph. */
interface IRecipeResolver {
    List<RecipeTransformation> getTransformations();
}
