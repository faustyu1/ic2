package ic2.integration.rei;

import ic2.core.gui.GaugeStyle;
import ic2.core.gui.GuiParser;
import ic2.core.gui.GuiTextures;
import ic2.core.gui.dynamic.GuiDefinition;
import ic2.core.gui.dynamic.GuiElementNode;
import ic2.core.gui.dynamic.SlotGridLayout;
import ic2.core.gui.dynamic.SlotStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

/**
 * A machine's recipes laid out like its window, port of the JEI DynamicCategory of 1.12.2.
 *
 * <p>The category reads the machine's guidef without its {@code game} environment, which holds the
 * upgrade slots and the recipe buttons, and draws what is left 16 pixels higher: slot backgrounds,
 * images, the energy gauge draining and the progress gauge filling. Slots named {@code input} take
 * the display's inputs in file order, slots named {@code output} its outputs; inputs left over after
 * the input slots go into the {@code <fluidtank>}s, such as the ore washing plant's water.
 */
public class GuidefCategory implements DisplayCategory<MachineDisplay> {
    static final int WIDTH = 160;
    static final int HEIGHT = 60;
    private static final int Y_OFFSET = -16;
    private static final int PADDING = 4;
    private static final int ENERGY_TICKS = 300;
    private static final int DEFAULT_PROCESS_TICKS = 200;

    private final CategoryIdentifier<MachineDisplay> id;
    private final ItemStack icon;
    private final Text title;
    private final List<Element> elements = new ArrayList<>();
    private final List<Point> inputs = new ArrayList<>();
    private final List<Point> outputs = new ArrayList<>();
    private final List<Point> tanks = new ArrayList<>();
    private ItemStack overlay = ItemStack.EMPTY;
    private Point overlayPos;

    /**
     * @param guidef the guidef file, e.g. {@code macerator}
     * @param operationLength the machine's default operation length; the progress gauge fills in a
     *                        third of it, as JEI showed it
     * @param gridSizes the number of slots of each slot grid, which the guidef leaves to the machine
     */
    public GuidefCategory(CategoryIdentifier<MachineDisplay> id, ItemStack icon, Text title, String guidef,
                          int operationLength, Map<String, Integer> gridSizes) {
        this.id = id;
        this.icon = icon;
        this.title = title;

        GuiDefinition definition = GuiParser.parse(guidef, "rei");
        if (definition != null) {
            // the definition is flat already: a slot wrapped in a <tooltip> is listed like any other
            for (GuiElementNode element : definition.getElements()) {
                add(element, operationLength, gridSizes);
            }
        }
    }

    /** An item drawn over the layout, the metal former's mode icon. */
    public GuidefCategory withOverlay(ItemStack stack, int x, int y) {
        this.overlay = stack;
        this.overlayPos = new Point(x, y);
        return this;
    }

    private void add(GuiElementNode element, int operationLength, Map<String, Integer> gridSizes) {
        int x = element.getX();
        int y = element.getY() + Y_OFFSET;

        switch (element.name) {
            case "slot" -> {
                SlotStyle style = SlotStyle.of(element);
                if (style.hasBackground() && element.getBool("background", true)) {
                    elements.add((ctx, ox, oy, ticks) -> drawCommon(ctx, ox + x, oy + y, style.u(), style.v(), style.width(), style.height()));
                }
                addSlot(element.get("name", ""), new Point(x + style.insetX(), y + style.insetY()));
            }
            case "slotgrid" -> {
                SlotGridLayout layout = SlotGridLayout.of(element, gridSizes.getOrDefault(element.get("name", ""), 1));
                SlotStyle style = layout.style();
                for (int row = 0; row < layout.rows(); row++) {
                    for (int col = 0; col < layout.cols(); col++) {
                        int cellX = x + col * layout.stepX();
                        int cellY = y + row * layout.stepY();
                        if (style.hasBackground() && element.getBool("background", true)) {
                            elements.add((ctx, ox, oy, ticks) -> drawCommon(ctx, ox + cellX, oy + cellY, style.u(), style.v(), style.width(), style.height()));
                        }
                        addSlot(element.get("name", ""), new Point(cellX + style.insetX(), cellY + style.insetY()));
                    }
                }
            }
            case "image" -> {
                int width = element.getInt("width", 16);
                int height = element.getInt("height", 16);
                int base = element.getInt("basesize", GuiTextures.DEFAULT_SIZE);
                int baseWidth = element.getInt("basewidth", base);
                int baseHeight = element.getInt("baseheight", base);
                int u = element.getInt("u", 0);
                int v = element.getInt("v", 0);
                var texture = GuiTextures.of(element.get("src", ""));
                elements.add((ctx, ox, oy, ticks) -> ctx.drawTexture(texture, ox + x, oy + y, u, v, width, height, baseWidth, baseHeight));
            }
            case "energygauge", "energyGauge" -> {
                GaugeStyle style = GaugeStyle.ofEnergy(element.get("style", ""));
                if (style != null) {
                    // the stored energy runs down while the machine works
                    elements.add((ctx, ox, oy, ticks) -> drawGauge(ctx, ox + x, oy + y, style,
                            1.0 - (ticks % ENERGY_TICKS) / (double) ENERGY_TICKS));
                }
            }
            case "gauge" -> {
                String styleName = element.get("style", "");
                GaugeStyle style = GaugeStyle.ofActive(styleName);
                if (style == null) {
                    break;
                }
                if (styleName.equalsIgnoreCase("heatcentrifuge")) {
                    // the heat is shown full, as JEI drew it
                    elements.add((ctx, ox, oy, ticks) -> drawGauge(ctx, ox + x, oy + y, style, 1.0));
                } else {
                    int period = "progress".equals(element.get("name", "")) && operationLength > 0
                            ? Math.max(1, operationLength / 3) : DEFAULT_PROCESS_TICKS;
                    elements.add((ctx, ox, oy, ticks) -> drawGauge(ctx, ox + x, oy + y, style,
                            (ticks % period) / (double) period));
                }
            }
            case "fluidtank" -> {
                if (element.get("type", "normal").equalsIgnoreCase("normal")) {
                    // the frame of a tank in use; the gauge lines go over the fluid in setupDisplay
                    elements.add((ctx, ox, oy, ticks) -> drawCommon(ctx, ox + x, oy + y, 6, 100, 20, 55));
                    tanks.add(new Point(x, y));
                }
            }
            case "fluidslot" -> elements.add((ctx, ox, oy, ticks) -> drawCommon(ctx, ox + x, oy + y, 8, 160, 18, 18));
            default -> {
            }
        }
    }

    private void addSlot(String name, Point pos) {
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.contains("input")) {
            inputs.add(pos);
        } else if (lower.contains("output")) {
            outputs.add(pos);
        }
    }

    @Override
    public CategoryIdentifier<? extends MachineDisplay> getCategoryIdentifier() {
        return id;
    }

    @Override
    public Text getTitle() {
        return title;
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
        widgets.add(Widgets.createDrawableWidget((ctx, mouseX, mouseY, delta) -> {
            long ticks = ticks();
            for (Element element : elements) {
                element.draw(ctx, ox, oy, ticks);
            }
            if (!overlay.isEmpty()) {
                ctx.drawItem(overlay, ox + overlayPos.x, oy + overlayPos.y);
            }
        }));

        List<EntryIngredient> inputEntries = display.getInputEntries();
        addSlots(widgets, inputs, inputEntries, ox, oy, true);
        addSlots(widgets, outputs, display.getOutputEntries(), ox, oy, false);
        for (int i = 0; i < tanks.size() && inputs.size() + i < inputEntries.size(); i++) {
            Point tank = tanks.get(i);
            widgets.add(Widgets.createSlot(new Rectangle(ox + tank.x + 4, oy + tank.y + 4, 12, 47))
                    .disableBackground().entries(inputEntries.get(inputs.size() + i)).markInput());
            widgets.add(Widgets.createDrawableWidget((ctx, mouseX, mouseY, delta) ->
                    drawCommon(ctx, ox + tank.x, oy + tank.y, 38, 100, 20, 55)));
        }

        if (display.getInfo() != null) {
            widgets.add(Widgets.createLabel(new Point(ox + WIDTH / 2, oy + HEIGHT - 9), display.getInfo())
                    .noShadow().color(0xFF404040, 0xFFBBBBBB));
        }
        return widgets;
    }

    private static void addSlots(List<Widget> widgets, List<Point> positions, List<EntryIngredient> entries,
                                 int ox, int oy, boolean input) {
        for (int i = 0; i < positions.size() && i < entries.size(); i++) {
            Point pos = positions.get(i);
            var slot = Widgets.createSlot(new Point(ox + pos.x, oy + pos.y)).disableBackground().entries(entries.get(i));
            widgets.add(input ? slot.markInput() : slot.markOutput());
        }
    }

    static long ticks() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client.world != null ? client.world.getTime() : System.currentTimeMillis() / 50;
    }

    static void drawCommon(DrawContext ctx, int x, int y, int u, int v, int width, int height) {
        ctx.drawTexture(GuiTextures.COMMON, x, y, u, v, width, height, GuiTextures.DEFAULT_SIZE, GuiTextures.DEFAULT_SIZE);
    }

    /** A gauge's background and its filled part, the same geometry the machine window draws. */
    static void drawGauge(DrawContext ctx, int x, int y, GaugeStyle style, double ratio) {
        int size = GuiTextures.DEFAULT_SIZE;
        if (style.hasBackground()) {
            ctx.drawTexture(style.texture(), x + style.backgroundOffsetX(), y + style.backgroundOffsetY(),
                    style.backgroundU(), style.backgroundV(), style.backgroundWidth(), style.backgroundHeight(), size, size);
        }

        ratio = Math.max(0.0, Math.min(1.0, ratio));
        int w = (int) Math.round(style.width() * ratio);
        int h = (int) Math.round(style.height() * ratio);
        switch (style.orientation()) {
            case RIGHT -> ctx.drawTexture(style.texture(), x, y, style.u(), style.v(), w, style.height(), size, size);
            case LEFT -> ctx.drawTexture(style.texture(), x + style.width() - w, y, style.u() + style.width() - w, style.v(), w, style.height(), size, size);
            case UP -> ctx.drawTexture(style.texture(), x, y + style.height() - h, style.u(), style.v() + style.height() - h, style.width(), h, size, size);
            case DOWN -> ctx.drawTexture(style.texture(), x, y, style.u(), style.v(), style.width(), h, size, size);
        }
    }

    @FunctionalInterface
    interface Element {
        void draw(DrawContext ctx, int ox, int oy, long ticks);
    }
}
