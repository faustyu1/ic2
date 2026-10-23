package ic2.core.item.tool;

import java.util.List;

import ic2.api.item.ElectricItem;
import ic2.core.WorldData;
import ic2.core.block.generator.tileentity.TileEntityWindGenerator;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

/**
 * Fabric port of {@code ic2.core.item.tool.ItemWindmeter}: reads the wind, for 50 EU a reading.
 *
 * <p>In the air it reports the wind at the player's height; pointed at a wind generator it
 * reports what that generator actually gets, obstructions included.
 */
public class ItemWindmeter extends ItemElectricTool {

    public ItemWindmeter() {
        super("wind_meter", 50, 10000, 100, 1, 1.0f, BlockTags.INCORRECT_FOR_WOODEN_TOOL, List.of());
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("wind_meter.tooltipA"));
        tooltip.add(Text.translatable("wind_meter.tooltipB"));
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (world.isClient || !ElectricItem.manager.canUse(stack, operationEnergyCost)) {
            return TypedActionResult.pass(stack);
        }

        ElectricItem.manager.use(stack, operationEnergyCost, player);

        double windStrength = Math.max(0.0, windAt(world, player.getY()));
        player.sendMessage(Text.translatable("wind_meter.info", roundWind(windStrength)), false);
        return TypedActionResult.success(stack);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        PlayerEntity player = context.getPlayer();
        if (world.isClient || player == null || player.isSneaking()) {
            return ActionResult.PASS;
        }

        ItemStack stack = context.getStack();
        if (!ElectricItem.manager.canUse(stack, operationEnergyCost)) {
            return ActionResult.PASS;
        }

        BlockEntity blockEntity = world.getBlockEntity(context.getBlockPos());
        if (!(blockEntity instanceof TileEntityWindGenerator generator)) {
            return ActionResult.PASS;
        }

        ElectricItem.manager.use(stack, operationEnergyCost, player);

        // the 1.12.2 formula: 567 obstructed blocks and the generator gets nothing at all
        double obstructiveFactor = generator.getObstructions() / 567.0;
        double wind = obstructiveFactor >= 1.0
                ? 0.0
                : windAt(world, context.getBlockPos().getY()) * (1.0 - obstructiveFactor);

        float displayWind = roundWind(wind);
        if (displayWind <= 0.0f) {
            player.sendMessage(Text.translatable("wind_meter.info.obstructed", generator.getObstructions()), false);
        } else {
            player.sendMessage(Text.translatable("wind_meter.info.effective", displayWind), false);
        }

        return ActionResult.SUCCESS;
    }

    private static double windAt(World world, double height) {
        WorldData data = WorldData.get(world);
        return data == null || data.windSim == null ? 0.0 : data.windSim.getWindAt(height);
    }

    private static float roundWind(double windStrength) {
        return (float) Math.round(windStrength * 100.0) / 100.0f;
    }
}
