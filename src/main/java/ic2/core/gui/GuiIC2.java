package ic2.core.gui;

import ic2.core.gui.dynamic.GuiDefinition;
import ic2.core.gui.dynamic.GuiElementNode;
import ic2.core.gui.dynamic.SlotGridLayout;
import ic2.core.gui.dynamic.SlotStyle;
import ic2.core.util.ConfigUtil;
import ic2.core.util.LiquidUtil;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.transfer.v1.client.fluid.FluidVariantRendering;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.texture.MissingSprite;
import net.minecraft.client.texture.Sprite;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

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
        // HandledScreen leaves the hovered item's tooltip to the screen
        drawMouseoverTooltip(context, mouseX, mouseY);
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
                case "slot", "armorslot", "slothologram", "craftresult" -> drawSlotBackground(context, left, top, element);
                case "slotgrid" -> drawSlotGrid(context, left, top, element);
                case "playerInventory" -> drawPlayerInventory(context, left, top, element);
                case "image" -> {
                    if (element != backgroundImage) {
                        drawImage(context, left, top, element);
                    }
                }
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
                case "bar" -> drawBar(context, left, top, element, handler.getGuiValue(element.get("name", "")));
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

        Fluid fluid = LiquidUtil.bySyncId(tank[0]);
        return java.util.List.of(
                fluid == null ? Text.literal("?") : ic2.core.util.FluidContainers.displayName(fluid),
                Text.literal(tank[1] + " / " + tank[2] + " mB"));
    }

    /** Window-relative x, y, width and height of an element, or null when it has no area. */
    private int[] boundsOf(GuiElementNode element) {
        switch (element.name) {
            case "slot", "armorslot", "slothologram" -> {
                SlotStyle style = SlotStyle.of(element);
                return new int[] {element.getX(), element.getY(), style.width(), style.height()};
            }
            case "slotgrid" -> {
                SlotGridLayout layout = SlotGridLayout.of(element, handler.getGroupSize(element));
                if (layout.rows() == 0 || layout.cols() == 0) {
                    return null;
                }
                return new int[] {element.getX() + layout.alignShiftX(element), element.getY(),
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
            backgroundImage = element;
            return true;
        }
        return false;
    }

    /** The image drawn as the window's background, which the element pass leaves alone. */
    private GuiElementNode backgroundImage;

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
     * colour, exactly as the original did. A fluid without a texture is drawn as a flat surface of
     * its colour instead.
     */
    private void fillFluid(DrawContext context, int x, int y, int width, int height, int fluidId, double ratio) {
        drawFluid(context, x, y, width, height, LiquidUtil.bySyncId(fluidId), ratio);
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

        FluidVariant variant = fluid == null ? FluidVariant.blank() : FluidVariant.of(fluid);
        int color = fluid == null ? 0x808080 : FluidVariantRendering.getColor(variant) & 0xFFFFFF;
        Sprite sprite = fluid == null ? null : FluidVariantRendering.getSprite(variant);
        if (sprite != null && sprite.getContents().getId().equals(MissingSprite.getMissingSpriteId())) {
            sprite = null;
        }

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
            int x = element.getX() + layout.alignShiftX(element);
            for (int row = 0; row < layout.rows(); row++) {
                for (int col = 0; col < layout.cols(); col++) {
                    drawCommon(context,
                            left + x + col * layout.stepX(),
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
        if (maxCols <= layout.cols() || "center".equals(element.get("align", ""))) {
            // a centred grid shrinks to its slots instead of covering the missing ones
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

    /**
     * A sunken bar the width of the window, filled from the left - IC2 Classic's reactor heat bar.
     * The fill runs from green to red as it grows; a {@code <text>} over it gives the label.
     */
    private void drawBar(DrawContext context, int left, int top, GuiElementNode element, double ratio) {
        int x = left + element.getX();
        int y = top + element.getY();
        int width = element.getWidth(100);
        // grid="<name>": as wide as that slot grid is now, under it - the reactor's grows with its chambers
        if (element.has("grid")) {
            for (GuiElementNode grid : definition.getElements("slotgrid")) {
                if (grid.get("name", "").equals(element.get("grid", ""))) {
                    SlotGridLayout layout = SlotGridLayout.of(grid, handler.getGroupSize(grid));
                    x = left + grid.getX() + layout.alignShiftX(grid);
                    width = layout.cols() * layout.stepX();
                    break;
                }
            }
        }
        int height = element.getHeight(14);

        // the same bevel as a slot: dark top and left, white bottom and right
        context.fill(x, y, x + width, y + height, 0xFF8B8B8B);
        context.fill(x, y, x + width - 1, y + 1, 0xFF373737);
        context.fill(x, y, x + 1, y + height - 1, 0xFF373737);
        context.fill(x + 1, y + height - 1, x + width, y + height, 0xFFFFFFFF);
        context.fill(x + width - 1, y + 1, x + width, y + height, 0xFFFFFFFF);

        double clamped = Math.max(0.0, Math.min(1.0, ratio));
        int fill = (int) Math.round((width - 2) * clamped);
        if (fill > 0) {
            int red = (int) Math.round(0x30 + (0xE0 - 0x30) * clamped);
            int green = (int) Math.round(0xC0 + (0x30 - 0xC0) * clamped);
            context.fill(x + 1, y + 1, x + 1 + fill, y + height - 1, 0xFF000000 | red << 16 | green << 8 | 0x30);
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

    /**
     * Draws an image. As the 1.12.2 Image did, a missing width or height is the texture's own,
     * and the texture coordinates are in pixels of the texture unless {@code basesize} says
     * otherwise - the small overlay textures (the solar panel's sun) are not 256 wide.
     */
    private void drawImage(DrawContext context, int left, int top, GuiElementNode element) {
        Identifier texture = GuiTextures.of(element.get("src", ""));
        int[] size = textureSize(texture);
        int width = element.getInt("width", size[0]);
        int height = element.getInt("height", size[1]);
        int baseWidth = element.getInt("basewidth", element.getInt("basesize", size[0]));
        int baseHeight = element.getInt("baseheight", element.getInt("basesize", size[1]));

        context.drawTexture(texture,
                left + element.getX(), top + element.getY(),
                element.getInt("u", 0), element.getInt("v", 0),
                width, height, baseWidth, baseHeight);
    }

    private static final java.util.Map<Identifier, int[]> TEXTURE_SIZES = new java.util.HashMap<>();

    /** The width and height of a texture, read from its PNG header; 256 when it cannot be read. */
    private static int[] textureSize(Identifier texture) {
        return TEXTURE_SIZES.computeIfAbsent(texture, id -> {
            var resource = net.minecraft.client.MinecraftClient.getInstance().getResourceManager().getResource(id);
            if (resource.isPresent()) {
                try (java.io.InputStream in = resource.get().getInputStream()) {
                    byte[] header = in.readNBytes(24);
                    if (header.length == 24) {
                        java.nio.ByteBuffer buffer = java.nio.ByteBuffer.wrap(header);
                        return new int[] {buffer.getInt(16), buffer.getInt(20)};
                    }
                } catch (java.io.IOException ignored) {
                    // fall through to the default
                }
            }
            return new int[] {DEFAULT_TEXTURE_SIZE, DEFAULT_TEXTURE_SIZE};
        });
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
            drawCentredTitle(context, text, textY, textColor(element));
            return;
        } else if ("right".equals(element.get("align", "left"))) {
            textX -= textRenderer.getWidth(text);
        }
        if (center.contains("y") && element.has("height")) {
            textY += (element.getHeight(0) - textRenderer.fontHeight) / 2 + 1;
        }

        // the positions were set for the English text: a longer translation is kept on the dark
        // display it is written on, moved to its middle or drawn smaller
        int width = textRenderer.getWidth(text);
        int[] panel = darkPanelAt(textX + 1, textY + textRenderer.fontHeight / 2);
        if (panel != null && (textX < panel[0] || textX + width > panel[1])) {
            int left = panel[0] + 2;
            int span = panel[1] - panel[0] - 4;
            if (width <= span) {
                textX = left + (span - width) / 2;
            } else {
                float scale = (float) span / width;
                context.getMatrices().push();
                context.getMatrices().translate(left, textY + textRenderer.fontHeight * (1 - scale) / 2, 0);
                context.getMatrices().scale(scale, scale, 1);
                context.drawText(textRenderer, text, 0, 0, textColor(element), false);
                context.getMatrices().pop();
                return;
            }
        }

        context.drawText(textRenderer, text, textX, textY, textColor(element), false);
    }

    private static final java.util.Map<Identifier, java.util.Optional<net.minecraft.client.texture.NativeImage>> TEXTURE_PIXELS = new java.util.HashMap<>();

    /**
     * The dark display panel of the window background under a point, as its left and right edge in
     * window coordinates, or null when the point is not on one.
     */
    private int @org.jetbrains.annotations.Nullable [] darkPanelAt(int x, int y) {
        for (GuiElementNode image : definition.getElements("image")) {
            Identifier texture = GuiTextures.of(image.get("src", ""));
            int[] size = textureSize(texture);
            int width = image.getInt("width", size[0]);
            int height = image.getInt("height", size[1]);
            if (x < image.getX() || y < image.getY() || x >= image.getX() + width || y >= image.getY() + height) {
                continue;
            }
            net.minecraft.client.texture.NativeImage pixels = texturePixels(texture);
            if (pixels == null) {
                continue;
            }
            int baseWidth = image.getInt("basewidth", image.getInt("basesize", size[0]));
            int baseHeight = image.getInt("baseheight", image.getInt("basesize", size[1]));
            int u = image.getInt("u", 0) - image.getX();
            int ty = (image.getInt("v", 0) + y - image.getY()) * pixels.getHeight() / baseHeight;
            java.util.function.IntPredicate dark = px -> {
                int tx = (u + px) * pixels.getWidth() / baseWidth;
                if (tx < 0 || tx >= pixels.getWidth() || ty < 0 || ty >= pixels.getHeight()) {
                    return false;
                }
                int abgr = pixels.getColor(tx, ty);
                return (abgr >>> 24) > 200 && (abgr & 0xFF) < 48 && (abgr >> 8 & 0xFF) < 48 && (abgr >> 16 & 0xFF) < 48;
            };
            if (!dark.test(x)) {
                return null;
            }
            int left = x;
            while (left > image.getX() && dark.test(left - 1)) {
                left--;
            }
            int right = x;
            while (right < image.getX() + width && dark.test(right)) {
                right++;
            }
            return new int[] {left, right};
        }
        return null;
    }

    private static net.minecraft.client.texture.@org.jetbrains.annotations.Nullable NativeImage texturePixels(Identifier texture) {
        return TEXTURE_PIXELS.computeIfAbsent(texture, id -> {
            var resource = net.minecraft.client.MinecraftClient.getInstance().getResourceManager().getResource(id);
            if (resource.isPresent()) {
                try (java.io.InputStream in = resource.get().getInputStream()) {
                    return java.util.Optional.of(net.minecraft.client.texture.NativeImage.read(in));
                } catch (java.io.IOException ignored) {
                    // no panel detection for this window
                }
            }
            return java.util.Optional.empty();
        }).orElse(null);
    }

    private static int textColor(GuiElementNode element) {
        String colorAttr = element.get("color", "");
        if (!colorAttr.isEmpty()) {
            try {
                return Integer.decode(colorAttr);
            } catch (NumberFormatException ignored) {
                // keep the default colour
            }
        }
        return 0x404040;
    }

    /**
     * A line centred across the window, kept clear of the slots beside it on its row - the upgrade
     * column in the top right corner, say - and drawn smaller when a long name does not fit between them.
     */
    private void drawCentredTitle(DrawContext context, Text text, int y, int color) {
        int left = 6;
        int right = backgroundWidth - 6;
        for (Slot slot : handler.slots) {
            if (!slot.isEnabled() || slot.y + 17 <= y || slot.y - 1 >= y + textRenderer.fontHeight) {
                continue;
            }
            if (slot.x + 8 < backgroundWidth / 2) {
                left = Math.max(left, slot.x + 19);
            } else {
                right = Math.min(right, slot.x - 3);
            }
        }

        int width = textRenderer.getWidth(text);
        int span = right - left;
        if (width <= span) {
            context.drawText(textRenderer, text, left + (span - width) / 2, y, color, false);
            return;
        }
        float scale = (float) span / width;
        context.getMatrices().push();
        context.getMatrices().translate(left, y + textRenderer.fontHeight * (1 - scale) / 2, 0);
        context.getMatrices().scale(scale, scale, 1);
        context.drawText(textRenderer, text, 0, 0, color, false);
        context.getMatrices().pop();
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
            // text around translations, e.g. "{ic2.FluidHeatGenerator.gui.info.Emit} 32" or "{key|arg} %"
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\{([^}]+)}").matcher(raw);
            StringBuilder out = new StringBuilder();
            while (matcher.find()) {
                String[] parts = matcher.group(1).split("\\|");
                Text translated = translation(parts[0], (Object[]) java.util.Arrays.copyOfRange(parts, 1, parts.length));
                matcher.appendReplacement(out, java.util.regex.Matcher.quoteReplacement(translated.getString()));
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
