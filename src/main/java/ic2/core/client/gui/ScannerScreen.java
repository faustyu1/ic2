package ic2.core.client.gui;

import ic2.core.item.handheld.ScannerScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Fabric port of {@code ic2.core.item.tool.GuiToolScanner}: the list of what the scan found, with
 * the icon of each ore beside its count.
 */
public class ScannerScreen extends HandledScreen<ScannerScreenHandler> {
    private static final Identifier TEXTURE = Identifier.of("ic2", "textures/gui/guitoolscanner.png");

    private static final int HEADING_COLOR = 0x20EFFE;
    private static final int LINE_COLOR = 0x57C8DA;

    public ScannerScreen(ScannerScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundHeight = 231;
        this.playerInventoryTitleY = this.backgroundHeight - 94;
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        context.drawTexture(TEXTURE, x, y, 0, 0, backgroundWidth, backgroundHeight, 256, 256);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        context.drawText(textRenderer, Text.translatable("itemScanner.found"), 10, 20, HEADING_COLOR, false);

        int line = 0;
        for (ScannerScreenHandler.ScanEntry entry : handler.getResults()) {
            String text = entry.count() + "x " + entry.stack().getName().getString();
            context.drawText(textRenderer, text, 10, 34 + line * 11, LINE_COLOR, false);
            // the icons alternate between two columns, as they did in 1.12.2
            context.drawItem(entry.stack(), 135 + (line & 1) * 15, 11 * line + 28);
            line++;
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        drawMouseoverTooltip(context, mouseX, mouseY);
    }
}
