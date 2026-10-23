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

/**
 * The fluid/solid canner in one of its modes, port of the JEI CannerCategory: the middle of the
 * canner's window with the mode label underneath.
 *
 * <p>Canning puts the filling (input 0) and the container (input 1) together into the output;
 * enriching turns the input tank's fluid (input 0) with the additive (input 1) into the output
 * tank's fluid.
 */
public class CannerCategory implements DisplayCategory<MachineDisplay> {
    private static final Identifier TEXTURE = GuiTextures.of("textures/gui/GUICanner");
    private static final int PADDING = 4;
    private static final int WIDTH = 96;
    private static final int HEIGHT = 81;
    private static final int INPUT_TANK_X = -1;
    private static final int OUTPUT_TANK_X = 77;
    private static final int TANK_Y = 26;

    public enum Activity {
        CANNING("BottleSolid", 18),
        ENRICHING("EnrichLiquid", 60);

        final String mode;
        final int labelV;

        Activity(String mode, int labelV) {
            this.mode = mode;
            this.labelV = labelV;
        }
    }

    private final CategoryIdentifier<MachineDisplay> id;
    private final ItemStack icon;
    private final Activity activity;

    public CannerCategory(CategoryIdentifier<MachineDisplay> id, ItemStack icon, Activity activity) {
        this.id = id;
        this.icon = icon;
        this.activity = activity;
    }

    @Override
    public CategoryIdentifier<? extends MachineDisplay> getCategoryIdentifier() {
        return id;
    }

    @Override
    public Text getTitle() {
        return Text.translatable("Canner.gui.switch." + activity.mode);
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
        int ox = bounds.x + (bounds.width - WIDTH) / 2;
        int oy = bounds.y + PADDING;
        boolean enriching = activity == Activity.ENRICHING;

        List<Widget> widgets = new ArrayList<>();
        widgets.add(Widgets.createRecipeBase(bounds));
        widgets.add(Widgets.createDrawableWidget((ctx, mouseX, mouseY, delta) -> {
            ctx.drawTexture(TEXTURE, ox, oy, 40, 16, WIDTH, HEIGHT, 256, 256);
            if (!enriching) {
                // cover the pipes between the tanks and the middle slot, which canning does not use
                ctx.drawTexture(TEXTURE, ox + 19, oy + 37, 3, 4, 9, 18, 256, 256);
                ctx.drawTexture(TEXTURE, ox + 59, oy + 37, 3, 4, 18, 23, 256, 256);
            }
            ctx.drawTexture(TEXTURE, ox + 23, oy + 65, 176, activity.labelV, 50, 14, 256, 256);
            for (int tankX : new int[] {INPUT_TANK_X, OUTPUT_TANK_X}) {
                // a tank in use, or the greyed-out one of a mode without tanks
                GuidefCategory.drawCommon(ctx, ox + tankX, oy + TANK_Y, enriching ? 6 : 70, 100, 20, 55);
            }
            int width = (int) (GuidefCategory.ticks() % 66 * 23 / 66);
            ctx.drawTexture(TEXTURE, ox + 34, oy + 6, 233, 0, width, 14, 256, 256);
        }));

        var inputs = display.getInputEntries();
        var outputs = display.getOutputEntries();
        if (enriching) {
            if (inputs.size() > 0) {
                widgets.add(tank(ox + INPUT_TANK_X, oy + TANK_Y).entries(inputs.get(0)).markInput());
            }
            if (inputs.size() > 1) {
                widgets.add(Widgets.createSlot(new Point(ox + 40, oy + 28)).disableBackground().entries(inputs.get(1)).markInput());
            }
            if (!outputs.isEmpty()) {
                widgets.add(tank(ox + OUTPUT_TANK_X, oy + TANK_Y).entries(outputs.get(0)).markOutput());
            }
        } else {
            if (inputs.size() > 0) {
                widgets.add(Widgets.createSlot(new Point(ox + 40, oy + 28)).disableBackground().entries(inputs.get(0)).markInput());
            }
            if (inputs.size() > 1) {
                widgets.add(Widgets.createSlot(new Point(ox + 1, oy + 1)).disableBackground().entries(inputs.get(1)).markInput());
            }
            if (!outputs.isEmpty()) {
                widgets.add(Widgets.createSlot(new Point(ox + 79, oy + 1)).disableBackground().entries(outputs.get(0)).markOutput());
            }
        }
        return widgets;
    }

    /** The inside of a tank, where the fluid is drawn. */
    private static me.shedaniel.rei.api.client.gui.widgets.Slot tank(int x, int y) {
        return Widgets.createSlot(new Rectangle(x + 4, y + 4, 12, 47)).disableBackground();
    }
}
