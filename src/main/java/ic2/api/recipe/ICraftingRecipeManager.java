package ic2.api.recipe;

import net.minecraft.item.ItemStack;

public interface ICraftingRecipeManager {
    void addRecipe(ItemStack output, Object... inputs);

    void addShapelessRecipe(ItemStack output, Object... inputs);

    class AttributeContainer {
        public final boolean hidden;
        public final boolean consuming;
        public final boolean fixedSize;

        public AttributeContainer(boolean hidden, boolean consuming) {
            this(hidden, consuming, false);
        }

        public AttributeContainer(boolean hidden, boolean consuming, boolean fixedSize) {
            this.hidden = hidden;
            this.consuming = consuming;
            this.fixedSize = fixedSize;
        }
    }
}
