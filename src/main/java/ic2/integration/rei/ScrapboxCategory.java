package ic2.integration.rei;

import ic2.core.gui.GuiTextures;
import java.util.ArrayList;
import java.util.List;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/** What a scrap box can drop and how likely, port of the JEI ScrapboxRecipeCategory. */
public class ScrapboxCategory implements DisplayCategory<MachineDisplay> {
    private static final Identifier TEXTURE = GuiTextures.of("ic2:textures/gui/ScrapboxRecipes.png");
    private static final int PADDING = 4;
    private static final int WIDTH = 82 + 40;
    private static final int HEIGHT = 26;

    private final CategoryIdentifier<MachineDisplay> id;
    private final ItemStack icon;

    public ScrapboxCategory(CategoryIdentifier<MachineDisplay> id, ItemStack icon) {
        this.id = id;
        this.icon = icon;
    }

    /** The chance as JEI printed it: two decimals, or "< 0.01" below a tenth of a percent. */
    static Text chance(float chance) {
        String text = chance < 0.001F ? "< 0.01" : String.format("%.2f", chance * 100.0F);
        return Text.literal(text + "%");
    }

    @Override
    public CategoryIdentifier<? extends MachineDisplay> getCategoryIdentifier() {
        return id;
    }

    @Override
    public Text getTitle() {
        return icon.getName();
    }

    @Override
    public Renderer getIcon() {
        return EntryStacks.of(icon);
    }

    @Override
    public int getDisplayWidth(MachineDisplay display) {
        return WIDTH + 2 * PADDING;
    }

    @Override
    public int getDisplayHeight() {
        return HEIGHT + 2 * PADDING;
    }

    @Override
    public List<Widget> setupDisplay(MachineDisplay display, Rectangle bounds) {
        int ox = bounds.x + PADDING;
        int oy = bounds.y + PADDING;
        List<Widget> widgets = new ArrayList<>();
        widgets.add(Widgets.createRecipeBase(bounds));
        widgets.add(Widgets.createTexturedWidget(TEXTURE, ox, oy, 55, 30, 82, 26));
        widgets.add(Widgets.createSlot(new Point(ox + 1, oy + 5)).disableBackground()
                .entries(display.getInputEntries().get(0)).markInput());
        widgets.add(Widgets.createSlot(new Point(ox + 61, oy + 5)).disableBackground()
                .entries(display.getOutputEntries().get(0)).markOutput());
        if (display.getInfo() != null) {
            widgets.add(Widgets.createLabel(new Point(ox + 86, oy + 9), display.getInfo())
                    .leftAligned().noShadow().color(0xFF404040, 0xFFBBBBBB));
        }
        return widgets;
    }
}
