package ic2.core.gui;

import ic2.core.gui.dynamic.GuiDefinition;
import ic2.core.gui.dynamic.GuiElementNode;
import ic2.core.gui.dynamic.SlotGridLayout;
import ic2.core.gui.dynamic.SlotStyle;
import ic2.core.util.ConfigUtil;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.texture.MissingSprite;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;

/**
 * The screen for every guidef-driven machine GUI.
 *
 * <p>It draws what the machine's {@code assets/ic2/guidef/*.xml} declares: either the background
 * image the file names, or the nine-slice frame from {@code common.png} the original used as the
 * default, plus the slot backgrounds, labels, gauges and buttons.
 *
 * <p>Slot contents and the player inventory are drawn by the vanilla handled screen from the slots
 * {@link DynamicContainer} built out of the same definition.
 */
public class GuiIC2 extends HandledScreen<DynamicContainer> {
    /** Shared IC2 GUI atlas holding the frame, slots and gauges. */
    public static final Identifier COMMON_TEXTURE = GuiTextures.COMMON;
    private static final int DEFAULT_TEXTURE_SIZE = GuiTextures.DEFAULT_SIZE;

    private final GuiDefinition definition;
    /** The buttons of the definition, in the order the container assigns their ids. */
    private final List<GuiButton> buttons = new ArrayList<>();

    public GuiIC2(DynamicContainer handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);

        this.definition = handler.getDefinition();
        if (definition != null) {
            this.backgroundWidth = definition.width;
            this.backgroundHeight = definition.height;
        }
        // the guidef places its own labels, so the vanilla ones stay off screen
        this.titleX = Integer.MIN_VALUE;
        this.playerInventoryTitleX = Integer.MIN_VALUE;
    }

    /**
     * Creates the buttons once the window position is known.
     *
     * <p>The button id is its position among the {@code <button>} elements of the file, which is
     * what the container reads on the other side, so a click travels as the vanilla button packet
     * without any custom networking. Buttons of type {@code recipe} opened the recipe view of JEI
     * in 1.12.2; they get no widget here, the REI plugin finds them through
     * {@link #recipeEventAt} instead, and without REI they stay inert as they did without JEI.
     */
    @Override
    protected void init() {
        super.init();
        buttons.clear();

        if (definition == null) {
            return;
        }

        int id = 0;
        for (GuiElementNode element : definition.getElements()) {
            if (!element.name.equals("button")) {
                continue;
            }

            int buttonId = id++;
            if ("recipe".equalsIgnoreCase(element.get("type", "vanilla"))) {
                continue;
            }

            Text label = resolveText(element.getText());
            GuiButton button = new GuiButton(element, label == null ? Text.empty() : label, iconOf(element),
                    x + element.getX(), y + element.getY(),
                    element.getWidth(20), element.getHeight(20),
                    ignored -> press(buttonId));
            buttons.add(button);
            addDrawableChild(button);
        }
    }

    /**
     * The recipe list a {@code <button type="recipe">} under the mouse opens, e.g. {@code macerator}
     * or {@code minecraft.smelting}; null when there is no such button there.
     */
    public String recipeEventAt(double mouseX, double mouseY) {
        if (definition == null) {
            return null;
        }

        for (GuiElementNode element : definition.getElements("button")) {
            if (!"recipe".equalsIgnoreCase(element.get("type", "vanilla")) || !handler.isEnabled(element)) {
                continue;
            }

            int left = x + element.getX();
            int top = y + element.getY();
            if (mouseX >= left && mouseX < left + element.getWidth(20)
                    && mouseY >= top && mouseY < top + element.getHeight(20)) {
                return element.get("event", null);
            }
        }
        return null;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        for (GuiButton button : buttons) {
            button.visible = handler.isEnabled(button.element);
            button.active = button.visible;
        }

        super.render(context, mouseX, mouseY, delta);
        drawElementTooltip(context, mouseX, mouseY);
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int left = x;
        int top = y;

        if (definition == null) {
            drawFrame(context, left, top, backgroundWidth, backgroundHeight);
            return;
        }

        if (!drawBackgroundImage(context, left, top)) {
            drawFrame(context, left, top, definition.width, definition.height);
        }

        for (GuiElementNode element : definition.getElements()) {
            if (!handler.isEnabled(element)) {
                continue;
            }

            switch (element.name) {
                case "slot", "slothologram", "craftresult" -> drawSlotBackground(context, left, top, element);
                case "slotgrid" -> drawSlotGrid(context, left, top, element);
                case "playerInventory" -> drawPlayerInventory(context, left, top, element);
                case "image" -> drawImage(context, left, top, element);
                case "gauge" -> {
                    String name = element.get("name", "");
                    double ratio = handler.getGuiValue(name);
                    // the background turns lit while the gauge is active: by default whenever it
                    // shows anything, with active="true" when the machine says so
                    boolean active = element.has("active") ? handler.getGuiCondition("active:" + name) : ratio > 0.0;
                    String style = element.get("style", "");
                    drawGauge(context, left, top, element,
                            active ? GaugeStyle.ofActive(style) : GaugeStyle.of(style), ratio);
                }
                case "energygauge", "energyGauge" -> drawGauge(context, left, top, element,
                        GaugeStyle.ofEnergy(element.get("style", "")),
                        handler.getGuiValue("energy"));
                case "fluidslot" -> drawFluidSlot(context, left, top, element);
                case "fluidtank" -> drawFluidTank(context, left, top, element);
                default -> { /* text is drawn in the foreground, buttons are widgets */ }
            }
        }
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        if (definition == null) {
            return;
        }

        for (GuiElementNode element : definition.getElements("text")) {
            if (handler.isEnabled(element)) {
                drawText(context, element);
            }
        }
    }

    /**
     * Draws the tooltip of a {@code <tooltip>} element the mouse is over. Item tooltips win, which
     * is what the original did as well - a tooltip wrapping a slot only shows while it is empty.
     */
    private void drawElementTooltip(DrawContext context, int mouseX, int mouseY) {
        if (definition == null || focusedSlot != null || !handler.getCursorStack().isEmpty()) {
            return;
        }

        for (GuiElementNode element : definition.getElements()) {
            if (!handler.isEnabled(element)) {
                continue;
            }

            String key = element.getTooltipKey();
            boolean isTank = element.name.equals("fluidtank") || element.name.equals("fluidslot");
            boolean isGauge = element.name.equals("gauge")
                    || element.name.equals("energygauge") || element.name.equals("energyGauge");
            if (key == null && !isTank && !isGauge) {
                continue;
            }

            int[] bounds = boundsOf(element);
            if (bounds == null) {
                continue;
            }
            if (mouseX < x + bounds[0] || mouseX >= x + bounds[0] + bounds[2]
                    || mouseY < y + bounds[1] || mouseY >= y + bounds[1] + bounds[3]) {
                continue;
            }

            if (isTank) {
                context.drawTooltip(textRenderer, tankTooltip(element), mouseX, mouseY);
            } else if (isGauge && key == null) {
                java.util.List<Text> tooltip = gaugeTooltip(element);
                if (tooltip.isEmpty()) {
                    continue;
                }
                context.drawTooltip(textRenderer, tooltip, mouseX, mouseY);
            } else {
                // a tooltip may carry numbers and arguments like a text: "{key|%amount:heat%}"
                Text text = key.contains("{") || key.contains("%") ? resolveText(key) : translation(key);
                if (text != null) {
                    context.drawTooltip(textRenderer, text, mouseX, mouseY);
                }
            }
            return;
        }
    }

    /**
     * What a gauge stands for, as "1234 / 40000 EU" - the energy buffer of the block, which the
     * original wrote next to its energy bar.
     *
     * @return the lines, or an empty list when the machine gives no numbers for that gauge
     */
    private java.util.List<Text> gaugeTooltip(GuiElementNode element) {
        boolean energy = !element.name.equals("gauge");
        String name = energy ? "energy" : element.get("name", "");

        int[] numbers = handler.getGuiNumbers(name);
        if (numbers == null || numbers[1] <= 0) {
            return java.util.List.of();
        }

        String unit = energy ? " EU" : "";
        return java.util.List.of(Text.literal(numbers[0] + " / " + numbers[1] + unit));
    }

    /** What a tank holds, in the "fluid: amount / capacity mB" form the original showed. */
    private java.util.List<net.minecraft.text.Text> tankTooltip(GuiElementNode element) {
        int[] tank = handler.getTank(element.get("name", ""));
        if (tank == null || tank[1] <= 0) {
            return java.util.List.of(translation("ic2.generic.text.empty"));
        }

        Fluid fluid = FluidRegistry.byId(tank[0]);
        String name = fluid == null ? "?" : fluid.getName();
        return java.util.List.of(
                Text.literal(name),
                Text.literal(tank[1] + " / " + tank[2] + " mB"));
    }

    /** Window-relative x, y, width and height of an element, or null when it has no area. */
    private int[] boundsOf(GuiElementNode element) {
        switch (element.name) {
            case "slot", "slothologram" -> {
                SlotStyle style = SlotStyle.of(element);
                return new int[] {element.getX(), element.getY(), style.width(), style.height()};
            }
            case "slotgrid" -> {
                SlotGridLayout layout = SlotGridLayout.of(element, handler.getGroupSize(element));
                if (layout.rows() == 0 || layout.cols() == 0) {
                    return null;
                }
                return new int[] {element.getX(), element.getY(),
                        layout.cols() * layout.stepX(), layout.rows() * layout.stepY()};
            }
            case "image", "button", "text" -> {
                int width = element.getWidth(0);
                int height = element.getHeight(0);
                return width <= 0 || height <= 0 ? null
                        : new int[] {element.getX(), element.getY(), width, height};
            }
            case "gauge", "energygauge", "energyGauge" -> {
                GaugeStyle style = element.name.equals("gauge")
                        ? GaugeStyle.of(element.get("style", ""))
                        : GaugeStyle.ofEnergy(element.get("style", ""));
                return style == null ? null
                        : new int[] {element.getX(), element.getY(), style.width(), style.height()};
            }
            case "fluidslot" -> {
                return new int[] {element.getX(), element.getY(), 18, 18};
            }
            case "fluidtank" -> {
                boolean withBorder = "normal".equalsIgnoreCase(element.get("type", "normal"));
                return new int[] {element.getX(), element.getY(),
                        element.getWidth(withBorder ? 20 : 12), element.getHeight(withBorder ? 55 : 47)};
            }
            default -> {
                return null;
            }
        }
    }

    /**
     * Draws the full-window image the guidef names, if it has one.
     *
     * @return true when an image covering the whole window was drawn
     */
    private boolean drawBackgroundImage(DrawContext context, int left, int top) {
        for (GuiElementNode element : definition.getElements("image")) {
            if (element.getX() != 0 || element.getY() != 0 || element.has("u") || element.has("v")) {
                continue;
            }
            int width = element.getInt("width", definition.width);
            int height = element.getInt("height", definition.height);
            if (width < definition.width || height < definition.height) {
                continue;
            }

            context.drawTexture(GuiTextures.of(element.get("src", "")), left, top, 0, 0, width, height,
                    element.getInt("basesize", DEFAULT_TEXTURE_SIZE),
                    element.getInt("basesize", DEFAULT_TEXTURE_SIZE));
            return true;
        }
        return false;
    }

    /**
     * The default IC2 window: a nine-slice of 32x32 tiles from the common atlas, drawn 16 pixels
     * outside the content area on every side, exactly as GuiDefaultBackground did.
     */
    private void drawFrame(DrawContext context, int left, int top, int width, int height) {
        // corners
        drawCommon(context, left - 16, top - 16, 0, 0, 32, 32);
        drawCommon(context, left + width - 16, top - 16, 64, 0, 32, 32);
        drawCommon(context, left - 16, top + height - 16, 0, 64, 32, 32);
        drawCommon(context, left + width - 16, top + height - 16, 64, 64, 32, 32);

        // top and bottom edges
        for (int side = 0; side < 2; side++) {
            int edgeY = top + height * side - 16;
            int v = 64 * side;
            for (int offset = 16; offset < width - 16; offset += 32) {
                int segment = Math.min(32, width - 16 - offset);
                drawCommon(context, left + offset, edgeY, 32, v, segment, 32);
            }
        }

        // left and right edges
        for (int side = 0; side < 2; side++) {
            int edgeX = left + width * side - 16;
            int u = 64 * side;
            for (int offset = 16; offset < height - 16; offset += 32) {
                int segment = Math.min(32, height - 16 - offset);
                drawCommon(context, edgeX, top + offset, u, 32, 32, segment);
            }
        }

        // interior
        for (int offsetY = 16; offsetY < height - 16; offsetY += 32) {
            int segmentHeight = Math.min(32, height - 16 - offsetY);
            for (int offsetX = 16; offsetX < width - 16; offsetX += 32) {
                int segmentWidth = Math.min(32, width - 16 - offsetX);
                drawCommon(context, left + offsetX, top + offsetY, 32, 32, segmentWidth, segmentHeight);
            }
        }
    }

    /**
     * Draws a {@code <fluidslot>}: an 18x18 box from the common atlas with the fluid inside it.
     * The geometry is the one FluidSlot used in 1.12.2.
     */
    private void drawFluidSlot(DrawContext context, int left, int top, GuiElementNode element) {
        int x = left + element.getX();
        int y = top + element.getY();

        drawCommon(context, x, y, 8, 160, 18, 18);

        int[] tank = handler.getTank(element.get("name", ""));
        if (tank == null || tank[1] <= 0) {
            return;
        }
        fillFluid(context, x + 1, y + 1, 16, 16, tank[0], 1.0);
    }

    /**
     * Draws a {@code <fluidtank>}. The styles are TankGauge's: {@code normal} is the bordered 20x55
     * tank, {@code plain} is the bare surface the guidef sizes itself, {@code borderless} draws the
     * gauge frame around a 12x47 surface.
     */
    private void drawFluidTank(DrawContext context, int left, int top, GuiElementNode element) {
        String type = element.get("type", "normal").toLowerCase(java.util.Locale.ROOT);
        boolean withBorder = type.equals("normal");
        boolean withGauge = !type.equals("plain");

        int x = left + element.getX();
        int y = top + element.getY();
        int width = element.getWidth(withBorder ? 20 : 12);
        int height = element.getHeight(withBorder ? 55 : 47);

        int[] tank = handler.getTank(element.get("name", ""));
        int amount = tank == null ? 0 : tank[1];
        int capacity = tank == null ? 0 : tank[2];

        if (amount <= 0 || capacity <= 0) {
            if (withBorder) {
                drawCommon(context, x, y, 70, 100, width, height);
            } else if (withGauge) {
                drawCommon(context, x, y, 74, 104, width, height);
            }
            return;
        }

        int fluidX = x;
        int fluidY = y;
        int fluidWidth = width;
        int fluidHeight = height;

        if (withBorder) {
            drawCommon(context, x, y, 6, 100, width, height);
            fluidX += 4;
            fluidY += 4;
            fluidWidth = 12;
            fluidHeight = 47;
        }

        fillFluid(context, fluidX, fluidY, fluidWidth, fluidHeight, tank[0],
                Math.min(1.0, (double) amount / capacity));

        if (withGauge) {
            // the gauge frame is drawn on top of the fluid, around the 12x47 surface; the
            // mirrored variant of the original is drawn unmirrored, the frame is symmetric enough
            drawCommon(context, withBorder ? x : x - 4, withBorder ? y : y - 4, 38, 100, 20, 55);
        }
    }

    /**
     * Fills the lower part of a rectangle with a fluid, drawing its own texture tinted with its
     * colour, exactly as the original did. A fluid whose texture is not in the block atlas is
     * drawn as a flat surface of its colour instead.
     */
    private void fillFluid(DrawContext context, int x, int y, int width, int height, int fluidId, double ratio) {
        drawFluid(context, x, y, width, height, FluidRegistry.byId(fluidId), ratio);
    }

    /**
     * Fills the lower part of a rectangle with a fluid's still texture, tinted in its colour, the
     * way the tanks and fluid slots show their content; the recipe viewer draws fluids with it too.
     */
    public static void drawFluid(DrawContext context, int x, int y, int width, int height, Fluid fluid, double ratio) {
        int filled = (int) Math.round(height * Math.max(0.0, Math.min(1.0, ratio)));
        if (filled <= 0) {
            return;
        }

        int color = fluid == null ? 0x808080 : fluid.getColor();
        Sprite sprite = fluid == null ? null : spriteOf(fluid);

        if (sprite == null) {
            context.fill(x, y + height - filled, x + width, y + height, 0xFF000000 | color);
            return;
        }

        // the texture is 16x16, so it is tiled over the surface; the scissor cuts the tiles that
        // stick out past the fluid level or the tank's edge
        int top = y + height - filled;
        float red = ((color >> 16) & 0xFF) / 255.0f;
        float green = ((color >> 8) & 0xFF) / 255.0f;
        float blue = (color & 0xFF) / 255.0f;

        context.enableScissor(x, top, x + width, y + height);
        for (int tileY = y + height - 16; tileY + 16 > top; tileY -= 16) {
            for (int tileX = x; tileX < x + width; tileX += 16) {
                context.drawSprite(tileX, tileY, 0, 16, 16, sprite, red, green, blue, 1.0f);
            }
        }
        context.disableScissor();
    }

    /** The fluid's still texture as stitched into the block atlas, or null when it is not there. */
    private static Sprite spriteOf(Fluid fluid) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return null;
        }

        Sprite sprite = client.getSpriteAtlas(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE).apply(fluid.getStillTexture());
        return sprite == null || sprite.getContents().getId().equals(MissingSprite.getMissingSpriteId()) ? null : sprite;
    }

    private void drawSlotBackground(DrawContext context, int left, int top, GuiElementNode element) {
        SlotStyle style = SlotStyle.of(element);
        if (!style.hasBackground() || !element.getBool("background", true)) {
            // the plain style belongs to a slot the machine's own background image already shows
            return;
        }
        drawCommon(context, left + element.getX(), top + element.getY(),
                style.u(), style.v(), style.width(), style.height());
    }

    private void drawSlotGrid(DrawContext context, int left, int top, GuiElementNode element) {
        SlotGridLayout layout = SlotGridLayout.of(element, handler.getGroupSize(element));
        SlotStyle style = layout.style();

        // a grid whose backgrounds are already part of the machine's own image says so
        if (style.hasBackground() && element.getBool("background", true)) {
            for (int row = 0; row < layout.rows(); row++) {
                for (int col = 0; col < layout.cols(); col++) {
                    drawCommon(context,
                            left + element.getX() + col * layout.stepX(),
                            top + element.getY() + row * layout.stepY(),
                            style.u(), style.v(), style.width(), style.height());
                }
            }
        }

        drawLockedCells(context, left, top, element, layout);
    }

    /**
     * Covers the cells a grid does not have yet.
     *
     * <p>The reactor is the case this exists for: its background image shows all nine columns, but
     * only the ones its chambers unlock hold a slot, so the rest is covered with the same patch
     * the 1.12.2 screen drew over them. A grid without {@code maxcols} has nothing to cover.
     */
    private void drawLockedCells(DrawContext context, int left, int top,
                                 GuiElementNode element, SlotGridLayout layout) {
        int maxCols = element.getInt("maxcols", 0);
        if (maxCols <= layout.cols()) {
            return;
        }

        Identifier texture = element.has("lockedsrc")
                ? GuiTextures.of(element.get("lockedsrc", ""))
                : GuiTextures.COMMON;
        int u = element.getInt("lockedu", 0);
        int v = element.getInt("lockedv", 0);
        int inset = layout.style().insetX();

        for (int row = 0; row < layout.rows(); row++) {
            for (int col = layout.cols(); col < maxCols; col++) {
                context.drawTexture(texture,
                        left + element.getX() + inset + col * layout.stepX(),
                        top + element.getY() + layout.style().insetY() + row * layout.stepY(),
                        u, v, 16, 16, DEFAULT_TEXTURE_SIZE, DEFAULT_TEXTURE_SIZE);
            }
        }
    }

    private void drawPlayerInventory(DrawContext context, int left, int top, GuiElementNode element) {
        if (!element.getBool("background", true)) {
            // the machine's own background image already shows the player inventory
            return;
        }

        SlotStyle style = SlotStyle.NORMAL;
        int originX = left + element.getX();
        int originY = top + element.getY();

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawCommon(context,
                        originX + col * DynamicContainer.SLOT_SIZE,
                        originY + row * DynamicContainer.SLOT_SIZE,
                        style.u(), style.v(), style.width(), style.height());
            }
        }
        for (int col = 0; col < 9; col++) {
            drawCommon(context,
                    originX + col * DynamicContainer.SLOT_SIZE,
                    originY + 3 * DynamicContainer.SLOT_SIZE + 4,
                    style.u(), style.v(), style.width(), style.height());
        }
    }

    private void drawImage(DrawContext context, int left, int top, GuiElementNode element) {
        int width = element.getInt("width", 16);
        int height = element.getInt("height", 16);
        int base = element.getInt("basesize", DEFAULT_TEXTURE_SIZE);

        context.drawTexture(GuiTextures.of(element.get("src", "")),
                left + element.getX(), top + element.getY(),
                element.getInt("u", 0), element.getInt("v", 0),
                width, height, base, base);
    }

    /**
     * Draws a label. The guidef uses {@code %name%} for the block name and {@code {some.key}} for
     * a translation key, which is what the original TextProvider resolved.
     */
    private void drawText(DrawContext context, GuiElementNode element) {
        Text text = resolveText(element.getText().isEmpty() ? element.get("text", "") : element.getText());
        if (text == null) {
            return;
        }

        int textX = element.getX();
        int textY = element.getY();
        String center = element.get("center", "");
        if (center.contains("x") && element.has("width")) {
            // centred in the box the element spans, as the original Text element did
            textX += (element.getWidth(0) - textRenderer.getWidth(text)) / 2;
        } else if ("center".equals(element.get("align", "left"))) {
            textX = (backgroundWidth - textRenderer.getWidth(text)) / 2;
        } else if ("right".equals(element.get("align", "left"))) {
            textX -= textRenderer.getWidth(text);
        }
        if (center.contains("y") && element.has("height")) {
            textY += (element.getHeight(0) - textRenderer.fontHeight) / 2 + 1;
        }

        int color = 0x404040;
        String colorAttr = element.get("color", "");
        if (!colorAttr.isEmpty()) {
            try {
                color = Integer.decode(colorAttr);
            } catch (NumberFormatException ignored) {
                // keep the default colour
            }
        }
        context.drawText(textRenderer, text, textX, textY, color, false);
    }

    /** {@code %amount:name%} and {@code %capacity:name%}: the synced numbers behind a gui value. */
    private static final java.util.regex.Pattern NUMBER = java.util.regex.Pattern.compile("%(amount|capacity):([A-Za-z0-9_]+)%");

    private String substituteNumbers(String raw) {
        java.util.regex.Matcher matcher = NUMBER.matcher(raw);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            int[] numbers = handler.getGuiNumbers(matcher.group(2));
            int value = numbers == null ? 0 : numbers["amount".equals(matcher.group(1)) ? 0 : 1];
            matcher.appendReplacement(out, Integer.toString(value));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    /**
     * Resolves the text of a {@code <text>} or {@code <button>}.
     *
     * @return the text, or null for the expression form ({@code %base.getHeat()%}) the ported
     *         machines do not provide yet
     */
    private Text resolveText(String raw) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        if (raw.equals("%name%")) {
            return title;
        }
        raw = substituteNumbers(raw);
        if (raw.startsWith("%")) {
            return null;
        }
        if (raw.startsWith("{") && raw.endsWith("}") && raw.indexOf('}') == raw.length() - 1) {
            // {key|arg|arg} fills the translation's placeholders
            String[] parts = raw.substring(1, raw.length() - 1).split("\\|");
            if (parts.length > 1) {
                return translation(parts[0], (Object[]) java.util.Arrays.copyOfRange(parts, 1, parts.length));
            }
            return translation(parts[0]);
        }
        if (raw.contains("{")) {
            // text around translations, e.g. "{ic2.FluidHeatGenerator.gui.info.Emit} 32"
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\{([^}|]+)}").matcher(raw);
            StringBuilder out = new StringBuilder();
            while (matcher.find()) {
                matcher.appendReplacement(out, java.util.regex.Matcher.quoteReplacement(translation(matcher.group(1)).getString()));
            }
            matcher.appendTail(out);
            return Text.literal(out.toString());
        }
        return Text.literal(raw);
    }

    /**
     * Looks a translation key up in both spellings.
     *
     * <p>The guidef files write the 1.12.2 keys ({@code ic2.BlockCutter.gui.bladeslot}) while the
     * extracted language files dropped the leading namespace, so a missing key is retried without
     * it rather than being shown raw.
     */
    private static Text translation(String key, Object... args) {
        if (!I18n.hasTranslation(key) && key.startsWith("ic2.") && I18n.hasTranslation(key.substring(4))) {
            return Text.translatable(key.substring(4), args);
        }
        return Text.translatable(key, args);
    }

    /**
     * Draws a gauge: its static background first, then the filled part revealed from the side the
     * style grows from, which is how the original animated progress and energy.
     */
    private void drawGauge(DrawContext context, int left, int top, GuiElementNode element,
                           GaugeStyle style, double ratio) {
        if (style == null) {
            return;
        }

        int x = left + element.getX();
        int y = top + element.getY();

        if (style.hasBackground()) {
            context.drawTexture(style.texture(),
                    x + style.backgroundOffsetX(), y + style.backgroundOffsetY(),
                    style.backgroundU(), style.backgroundV(),
                    style.backgroundWidth(), style.backgroundHeight(),
                    DEFAULT_TEXTURE_SIZE, DEFAULT_TEXTURE_SIZE);
        }

        ratio = Math.max(0.0, Math.min(1.0, ratio));
        if (ratio <= 0.0) {
            return;
        }

        int filledWidth = (int) Math.round(style.width() * ratio);
        int filledHeight = (int) Math.round(style.height() * ratio);

        switch (style.orientation()) {
            case RIGHT -> drawGaugePart(context, style, x, y, style.u(), style.v(), filledWidth, style.height());
            case LEFT -> drawGaugePart(context, style, x + style.width() - filledWidth, y,
                    style.u() + style.width() - filledWidth, style.v(), filledWidth, style.height());
            case UP -> drawGaugePart(context, style, x, y + style.height() - filledHeight,
                    style.u(), style.v() + style.height() - filledHeight, style.width(), filledHeight);
            case DOWN -> drawGaugePart(context, style, x, y, style.u(), style.v(), style.width(), filledHeight);
        }
    }

    private void drawGaugePart(DrawContext context, GaugeStyle style,
                               int x, int y, int u, int v, int width, int height) {
        context.drawTexture(style.texture(), x, y, u, v, width, height,
                DEFAULT_TEXTURE_SIZE, DEFAULT_TEXTURE_SIZE);
    }

    private void drawCommon(DrawContext context, int x, int y, int u, int v, int width, int height) {
        context.drawTexture(COMMON_TEXTURE, x, y, u, v, width, height, DEFAULT_TEXTURE_SIZE, DEFAULT_TEXTURE_SIZE);
    }

    /**
     * A guidef {@code <button>}: a vanilla button, optionally showing an item as its icon, or a
     * region of an image when it names a {@code src}; it sends its id to the server when pressed.
     */
    private static final class GuiButton extends ButtonWidget {
        private final GuiElementNode element;
        private final ItemStack icon;

        private GuiButton(GuiElementNode element, Text label, ItemStack icon,
                          int x, int y, int width, int height, PressAction onPress) {
            super(x, y, width, height, label, onPress, DEFAULT_NARRATION_SUPPLIER);
            this.element = element;
            this.icon = icon;
        }

        @Override
        protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
            // a button drawn from the machine's own image, as the canner's mode switch is
            String src = element.get("src", "");
            if (!src.isEmpty()) {
                int base = element.getInt("basesize", DEFAULT_TEXTURE_SIZE);
                context.drawTexture(GuiTextures.of(src), getX(), getY(), element.getInt("u", 0), element.getInt("v", 0),
                        getWidth(), getHeight(), base, base);
                if (isHovered()) {
                    context.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), 0x30FFFFFF);
                }
                return;
            }
            super.renderWidget(context, mouseX, mouseY, delta);
            if (!icon.isEmpty()) {
                context.drawItem(icon, getX() + (getWidth() - 16) / 2, getY() + (getHeight() - 16) / 2);
            }
        }
    }

    private void press(int id) {
        if (client != null && client.interactionManager != null) {
            client.interactionManager.clickButton(handler.syncId, id);
        }
    }

    /** The item a button shows instead of a label, e.g. the iron furnace's experience bottle. */
    private static ItemStack iconOf(GuiElementNode element) {
        String icon = element.get("icon", "none");
        if ("none".equals(icon)) {
            return ItemStack.EMPTY;
        }
        try {
            return ConfigUtil.asStack(icon);
        } catch (ParseException e) {
            return ItemStack.EMPTY;
        }
    }
}
