package ic2.integration.rei;

import java.util.List;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.basic.BasicDisplay;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import net.minecraft.text.Text;

/**
 * One IC2 recipe as REI shows it: the ingredients in the order the category places them, and an
 * optional line of text such as the scrap box chance.
 *
 * <p>The displays are made on the client from IC2's own recipe managers, which both sides fill at
 * start-up, so they are never sent over the network and need no serializer.
 */
public class MachineDisplay extends BasicDisplay {
    private final CategoryIdentifier<MachineDisplay> category;
    private final Text info;

    public MachineDisplay(CategoryIdentifier<MachineDisplay> category, List<EntryIngredient> inputs, List<EntryIngredient> outputs) {
        this(category, inputs, outputs, null);
    }

    public MachineDisplay(CategoryIdentifier<MachineDisplay> category, List<EntryIngredient> inputs, List<EntryIngredient> outputs, Text info) {
        super(inputs, outputs);
        this.category = category;
        this.info = info;
    }

    @Override
    public CategoryIdentifier<?> getCategoryIdentifier() {
        return category;
    }

    public Text getInfo() {
        return info;
    }
}
