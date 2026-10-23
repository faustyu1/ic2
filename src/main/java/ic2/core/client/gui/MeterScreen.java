package ic2.core.client.gui;

import ic2.core.item.handheld.MeterScreenHandler;
import ic2.core.util.Util;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Fabric port of {@code ic2.core.item.tool.GuiToolMeter}: the EU reader's screen.
 *
 * <p>The four buttons on the right pick what is measured and the bar under the numbers resets the
 * measurement. 1.12.2 built those out of IC2's own widget classes; here they are hit tests in
 * {@code mouseClicked}, which is all those widgets did, and the click is reported through the
 * vanilla button channel.
 */
public class MeterScreen extends HandledScreen<MeterScreenHandler> {
    private static final Identifier TEXTURE = Identifier.of("ic2", "textures/gui/guitooleumeter.png");

    private static final int TEXT_COLOR = 0x20EFFE;

    /** The 20x20 mode buttons, in the order of {@link MeterScreenHandler.Mode}. */
    private static final int[][] MODE_BUTTONS = {
            {112, 55}, {132, 55}, {112, 75}, {132, 75},
    };

    /** Where the mode picture is drawn, and the four 40x40 pictures in the texture. */
    private static final int MODE_ICON_X = 112;
    private static final int MODE_ICON_Y = 55;
    private static final int[] MODE_ICON_V = {0, 40, 120, 80};

    public MeterScreen(MeterScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundHeight = 218;
        this.playerInventoryTitleY = this.backgroundHeight - 94;
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        context.drawTexture(TEXTURE, x, y, 0, 0, backgroundWidth, backgroundHeight, 256, 256);

        int icon = MODE_ICON_V[handler.getMode().ordinal()];
        context.drawTexture(TEXTURE, x + MODE_ICON_X, y + MODE_ICON_Y, 176, icon, 40, 40, 256, 256);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        String unit = Text.translatable(handler.getMode() == MeterScreenHandler.Mode.VOLTAGE
                ? "generic.text.v"
                : "generic.text.EUt").getString();

        context.drawText(textRenderer, Text.translatable("itemToolMEter.mode"), 115, 43, TEXT_COLOR, false);
        context.drawText(textRenderer, Text.translatable("itemToolMEter.avg"), 15, 41, TEXT_COLOR, false);
        context.drawText(textRenderer, Util.toSiString(handler.getResultAvg(), 6) + unit, 15, 51, TEXT_COLOR, false);
        context.drawText(textRenderer, Text.translatable("itemToolMEter.max/min"), 15, 64, TEXT_COLOR, false);
        context.drawText(textRenderer, Util.toSiString(handler.getResultMax(), 6) + unit, 15, 74, TEXT_COLOR, false);
        context.drawText(textRenderer, Util.toSiString(handler.getResultMin(), 6) + unit, 15, 84, TEXT_COLOR, false);
        context.drawText(textRenderer,
                Text.translatable("itemToolMEter.cycle", handler.getResultCount() / 20), 15, 100, TEXT_COLOR, false);
        context.drawText(textRenderer, Text.translatable("itemToolMEter.mode.reset"), 39, 114, TEXT_COLOR, false);
        context.drawText(textRenderer, Text.translatable(handler.getMode().getTranslationKey()),
                105, 100, TEXT_COLOR, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int localX = (int) mouseX - x;
        int localY = (int) mouseY - y;

        for (int mode = 0; mode < MODE_BUTTONS.length; mode++) {
            int buttonX = MODE_BUTTONS[mode][0];
            int buttonY = MODE_BUTTONS[mode][1];
            if (localX >= buttonX && localX < buttonX + 20 && localY >= buttonY && localY < buttonY + 20) {
                click(mode);
                return true;
            }
        }

        // the reset bar under the readings
        if (localX >= 26 && localX <= 83 && localY >= 111 && localY <= 123) {
            click(MeterScreenHandler.BUTTON_RESET);
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void click(int id) {
        if (client == null || client.interactionManager == null) return;
        client.interactionManager.clickButton(handler.syncId, id);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        drawMouseoverTooltip(context, mouseX, mouseY);
    }
}
