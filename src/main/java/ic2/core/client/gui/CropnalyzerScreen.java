package ic2.core.client.gui;

import ic2.api.crops.CropCard;
import ic2.core.item.handheld.CropnalyzerScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Fabric port of {@code ic2.core.item.tool.GuiCropnalyzer}: what is known about the seed in the
 * output slot, revealed a little more at each scan level.
 *
 * <p>Level 0 says nothing but {@code UNKNOWN}, level 1 adds the name, level 2 the tier and who
 * discovered it, level 3 the description lines and level 4 the three stats. All of it is read off
 * the stack in the slot, so the client needs nothing synced beyond the slot itself.
 */
public class CropnalyzerScreen extends HandledScreen<CropnalyzerScreenHandler> {
    private static final Identifier TEXTURE = Identifier.of("ic2", "textures/gui/guicropnalyzer.png");

    private static final int WHITE = 0xFFFFFF;
    private static final int GROWTH_COLOR = 0xAE05AF;
    private static final int GAIN_COLOR = 0xEEC400;
    private static final int RESISTANCE_COLOR = 0x00CED1;

    /** The roman numerals a crop tier is shown as. */
    private static final String[] TIERS = {
            "", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X",
    };

    public CropnalyzerScreen(CropnalyzerScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundHeight = 223;
        this.playerInventoryTitleY = this.backgroundHeight - 94;
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        context.drawTexture(TEXTURE, x, y, 0, 0, backgroundWidth, backgroundHeight, 256, 256);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        context.drawText(textRenderer, title, 74, 11, 0, false);

        int level = handler.getScannedLevel();
        if (level < 0) {
            return;
        }
        if (level == 0) {
            context.drawText(textRenderer, "UNKNOWN", 8, 37, WHITE, false);
            return;
        }

        CropCard crop = handler.getCrop();
        if (crop == null) {
            return;
        }

        context.drawText(textRenderer, Text.translatable(ic2.core.init.Localization.key(crop.getUnlocalizedName())), 8, 37, WHITE, false);

        if (level >= 2) {
            context.drawText(textRenderer, "Tier: " + tierOf(crop), 8, 50, WHITE, false);
            context.drawText(textRenderer, "Discovered by:", 8, 73, WHITE, false);
            context.drawText(textRenderer, crop.getDiscoveredBy(), 8, 86, WHITE, false);
        }

        if (level >= 3) {
            context.drawText(textRenderer, crop.desc(0), 8, 109, WHITE, false);
            context.drawText(textRenderer, crop.desc(1), 8, 122, WHITE, false);
        }

        if (level >= 4) {
            context.drawText(textRenderer, "Growth:", 118, 37, GROWTH_COLOR, false);
            context.drawText(textRenderer, Integer.toString(handler.getSeedGrowth()), 118, 50, GROWTH_COLOR, false);
            context.drawText(textRenderer, "Gain:", 118, 73, GAIN_COLOR, false);
            context.drawText(textRenderer, Integer.toString(handler.getSeedGain()), 118, 86, GAIN_COLOR, false);
            context.drawText(textRenderer, "Resis.:", 118, 109, RESISTANCE_COLOR, false);
            context.drawText(textRenderer, Integer.toString(handler.getSeedResistance()), 118, 122,
                    RESISTANCE_COLOR, false);
        }
    }

    private static String tierOf(CropCard crop) {
        int tier = crop.getProperties() == null ? 0 : crop.getProperties().getTier();
        return tier >= 0 && tier < TIERS.length ? TIERS[tier] : Integer.toString(tier);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        drawMouseoverTooltip(context, mouseX, mouseY);
    }
}
