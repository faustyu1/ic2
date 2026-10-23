package ic2.core.client;

import ic2.api.item.ElectricItem;
import ic2.api.item.HudMode;
import ic2.api.item.IElectricItem;
import ic2.api.item.IItemHudInfo;
import ic2.api.item.IItemHudProvider;
import ic2.core.item.armor.jetpack.JetpackHandler;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

/**
 * Port of {@code ic2.core.GuiOverlayer}: the armor HUD in the top left corner of the screen.
 *
 * <p>It shows while a nano or quantum helmet with charge is worn and its HUD mode (cycled with
 * ALT + the HUD key) is not off. Basic mode lists the four armor pieces with their charge (or
 * durability) in percent, plus night vision goggles beside the helmet while night vision is on.
 * Extended and advanced mode add the items in both hands with their tooltip, or the item's own HUD
 * info for IC2 tools; advanced shows the advanced tooltip.
 *
 * <p>Also adds the "Electric Jetpack attached" line to a chestplate carrying a jetpack, which
 * 1.12.2 did from the jetpack handler.
 */
public final class GuiOverlayer {
    private static final Identifier BACKGROUND = Identifier.of("ic2", "textures/gui/guioverlay.png");
    private static final int WHITE = 0xFFFFFFFF;

    private GuiOverlayer() {}

    public static void register() {
        HudRenderCallback.EVENT.register((context, tickCounter) -> render(context));
        ItemTooltipCallback.EVENT.register((stack, tooltipContext, type, lines) -> {
            // an electric item shows its charge right under its name
            if (stack.getItem() instanceof IElectricItem && ElectricItem.manager != null && !lines.isEmpty()) {
                String charge = ElectricItem.manager.getToolTip(stack);
                if (charge != null) {
                    lines.add(1, Text.literal(charge).formatted(Formatting.AQUA));
                }
            }
            if (JetpackHandler.hasJetpackAttached(stack)) {
                lines.add(Text.translatable("jetpackAttached").formatted(Formatting.YELLOW));
                if (!(stack.getItem() instanceof IElectricItem)) {
                    lines.add(Text.literal(ElectricItem.manager.getToolTip(stack)).formatted(Formatting.AQUA));
                }
            }
        });
    }

    private static void render(DrawContext context) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null || mc.options.hudHidden) {
            return;
        }

        ItemStack helm = player.getEquippedStack(EquipmentSlot.HEAD);
        if (helm.isEmpty() || !(helm.getItem() instanceof IItemHudProvider provider) || !provider.doesProvideHUD(helm)) {
            return;
        }

        HudMode hudMode = provider.getHudMode(helm);
        if (!hudMode.shouldDisplay()) {
            return;
        }

        TextRenderer font = mc.textRenderer;
        ItemStack boots = player.getEquippedStack(EquipmentSlot.FEET);
        ItemStack legs = player.getEquippedStack(EquipmentSlot.LEGS);
        ItemStack chestplate = player.getEquippedStack(EquipmentSlot.CHEST);
        if (chestplate.isEmpty()) {
            // a pack on the back takes the chest row
            chestplate = ic2.core.item.armor.jetpack.BackSlot.get(player);
        }

        context.drawTexture(BACKGROUND, 0, 0, 0, 0, 71, 69);
        context.drawItem(helm, 5, 4);
        context.drawText(font, mapCharge(helm) + "%", 25, 9, WHITE, false);
        if (data(helm).getBoolean("Nightvision")) {
            context.drawItem(item("nightvision_goggles"), 50, 4);
        }

        if (!chestplate.isEmpty()) {
            int charge = getCharge(chestplate);
            if (charge >= 0) {
                context.drawText(font, charge + "%", 25, 25, WHITE, false);
                context.drawItem(chestplate, 5, 20);
                if (data(chestplate).getBoolean("jetpack")) {
                    context.drawItem(item(data(chestplate).getBoolean("hoverMode") ? "jetpack_electric" : "jetpack"), 50, 20);
                }
            }
        }

        if (!legs.isEmpty()) {
            int charge = getCharge(legs);
            if (charge >= 0) {
                context.drawText(font, charge + "%", 25, 41, WHITE, false);
                context.drawItem(legs, 5, 36);
            }
        }

        if (!boots.isEmpty()) {
            int charge = getCharge(boots);
            if (charge >= 0) {
                context.drawText(font, charge + "%", 25, 56, WHITE, false);
                context.drawItem(boots, 5, 52);
            }
        }

        if (!hudMode.hasTooltip()) {
            return;
        }

        boolean advanced = hudMode == HudMode.ADVANCED;
        ItemStack rightItem = player.getMainHandStack();
        ItemStack leftItem = player.getOffHandStack();
        int nextLine = 83;

        if (!rightItem.isEmpty()) {
            context.drawItem(rightItem, 5, 74);
            context.drawText(font, rightItem.getName(), 30, 78, WHITE, false);
            List<Text> info = info(mc, rightItem, advanced);
            for (int l = 0; l < info.size(); l++) {
                context.drawText(font, info.get(l), 8, 83 + (l + 1) * 14, WHITE, false);
            }
            nextLine += (info.size() + 1) * 14 + 8;
        }

        if (!leftItem.isEmpty()) {
            context.drawItem(leftItem, 5, nextLine - 9);
            context.drawText(font, leftItem.getName(), 30, nextLine - 5, WHITE, false);
            List<Text> info = info(mc, leftItem, advanced);
            for (int l = 0; l < info.size(); l++) {
                context.drawText(font, info.get(l), 8, nextLine + (l + 1) * 14, WHITE, false);
            }
        }
    }

    /**
     * The lines under a held item: an IC2 item's HUD info, otherwise its tooltip without the name.
     */
    private static List<Text> info(MinecraftClient mc, ItemStack stack, boolean advanced) {
        List<Text> ret = new ArrayList<>();
        if (stack.getItem() instanceof IItemHudInfo hudInfo) {
            for (String line : hudInfo.getHudInfo(stack, advanced)) {
                ret.add(Text.literal(line));
            }
        } else {
            List<Text> tooltip = stack.getTooltip(Item.TooltipContext.create(mc.world), mc.player,
                    advanced ? TooltipType.ADVANCED : TooltipType.BASIC);
            ret.addAll(tooltip.subList(Math.min(1, tooltip.size()), tooltip.size()));
        }
        return ret;
    }

    /** Percent charge, percent durability left, or -1 for a piece with neither. */
    private static int getCharge(ItemStack stack) {
        Item item = stack.getItem();
        if (item instanceof IItemHudProvider.IItemHudBarProvider bar) {
            return bar.getBarPercent(stack);
        } else if (item instanceof IElectricItem) {
            return mapCharge(stack);
        } else if (stack.isDamageable()) {
            return (int) map(1.0 - (double) stack.getDamage() / stack.getMaxDamage(), 1.0, 100.0);
        }
        return -1;
    }

    private static int mapCharge(ItemStack stack) {
        double charge = ElectricItem.manager.getCharge(stack);
        double maxCharge = charge + ElectricItem.manager.charge(stack, Double.POSITIVE_INFINITY, Integer.MAX_VALUE, true, true);
        return (int) map(charge, maxCharge, 100.0);
    }

    /** {@code ic2.core.util.Util.map}: value clamped to 0..srcMax, scaled to 0..dstMax. */
    private static double map(double value, double srcMax, double dstMax) {
        if (value < 0.0 || Double.isNaN(value)) {
            value = 0.0;
        }
        if (value > srcMax) {
            value = srcMax;
        }
        return value / srcMax * dstMax;
    }

    private static net.minecraft.nbt.NbtCompound data(ItemStack stack) {
        return stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
    }

    private static ItemStack item(String name) {
        return new ItemStack(Registries.ITEM.get(Identifier.of("ic2", name)));
    }
}
