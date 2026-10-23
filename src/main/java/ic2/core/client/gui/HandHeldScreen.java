package ic2.core.client.gui;

import ic2.core.item.handheld.HandHeldScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

/**
 * The screen for every hand-held IC2 container. The background and the slot positions come from
 * the handler's {@link ic2.core.item.handheld.HandHeldKind}, so one screen covers them all.
 */
public class HandHeldScreen extends HandledScreen<HandHeldScreenHandler> {

    public HandHeldScreen(HandHeldScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundHeight = 166;
        this.playerInventoryTitleY = this.backgroundHeight - 94;
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        context.drawTexture(handler.getKind().getTexture(), x, y, 0, 0,
                backgroundWidth, backgroundHeight, 256, 256);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        drawMouseoverTooltip(context, mouseX, mouseY);
    }
}
