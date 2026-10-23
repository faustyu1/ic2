package ic2.integration.rei;

import ic2.core.gui.GaugeStyle;
import java.util.ArrayList;
import java.util.List;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

/**
 * The electrolyzer: one fluid in, up to five out, port of the JEI ElectrolyzerCategory.
 *
 * <p>The outputs take the tanks the machine fills for that many fluids: the middle one for an odd
 * count, the inner pair from two on and the outer pair from four on. The window's own gauge for
 * that count shows the split.
 */
public class ElectrolyzerCategory implements DisplayCategory<MachineDisplay> {
    private static final int PADDING = 4;
    private static final int[] OUTPUT_X = {36, 57, 78, 99, 120};
    private static final int OUTPUT_Y = 45;
    private static final int[] GAUGE_X = {84, 60, 60, 39, 39};

    private final CategoryIdentifier<MachineDisplay> id;
    private final ItemStack icon;

    public ElectrolyzerCategory(CategoryIdentifier<MachineDisplay> id, ItemStack icon) {
        this.id = id;
        this.icon = icon;
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
        return GuidefCategory.WIDTH + 2 * PADDING;
    }

    @Override
    public int getDisplayHeight() {
        return 64 + 2 * PADDING;
    }

    /** Tank index of each output, the way ElectrolyzerTankController assigns them. */
    static int[] tanks(int count) {
        return switch (count) {
            case 1 -> new int[] {2};
            case 2 -> new int[] {1, 3};
            case 3 -> new int[] {1, 2, 3};
            case 4 -> new int[] {0, 1, 3, 4};
            default -> new int[] {0, 1, 2, 3, 4};
        };
    }

    @Override
    public List<Widget> setupDisplay(MachineDisplay display, Rectangle bounds) {
        int ox = bounds.x + PADDING;
        int oy = bounds.y + PADDING;
        List<EntryIngredient> outputs = display.getOutputEntries();
        int count = Math.max(1, Math.min(5, outputs.size()));
        GaugeStyle progress = GaugeStyle.of("electrolyzer" + count);
        GaugeStyle energy = GaugeStyle.ofEnergy("");

        List<Widget> widgets = new ArrayList<>();
        widgets.add(Widgets.createRecipeBase(bounds));
        widgets.add(Widgets.createDrawableWidget((ctx, mouseX, mouseY, delta) -> {
            long ticks = GuidefCategory.ticks();
            GuidefCategory.drawCommon(ctx, ox + 78, oy, 8, 160, 18, 18);
            for (int x : OUTPUT_X) {
                GuidefCategory.drawCommon(ctx, ox + x, oy + OUTPUT_Y, 8, 160, 18, 18);
            }
            if (energy != null) {
                GuidefCategory.drawGauge(ctx, ox + 12, oy + 28, energy, 1.0 - (ticks % 300) / 300.0);
            }
            if (progress != null) {
                GuidefCategory.drawGauge(ctx, ox + GAUGE_X[count - 1], oy + 20, progress, (ticks % 100) / 100.0);
            }
        }));

        if (!display.getInputEntries().isEmpty()) {
            widgets.add(Widgets.createSlot(new Point(ox + 79, oy + 1)).disableBackground()
                    .entries(display.getInputEntries().get(0)).markInput());
        }
        int[] tanks = tanks(outputs.size());
        for (int i = 0; i < tanks.length && i < outputs.size(); i++) {
            widgets.add(Widgets.createSlot(new Point(ox + OUTPUT_X[tanks[i]] + 1, oy + OUTPUT_Y + 1)).disableBackground()
                    .entries(outputs.get(i)).markOutput());
        }
        if (display.getInfo() != null) {
            widgets.add(Widgets.createLabel(new Point(ox + 130, oy + 6), display.getInfo())
                    .noShadow().color(0xFF404040, 0xFFBBBBBB));
        }
        return widgets;
    }
}
