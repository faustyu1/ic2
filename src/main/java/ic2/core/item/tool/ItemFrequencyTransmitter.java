package ic2.core.item.tool;

import ic2.core.block.machine.tileentity.TileEntityTeleporter;
import ic2.core.item.ItemIC2;
import java.util.List;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Fabric port of ItemFrequencyTransmitter - links two teleporters.
 *
 * <p>Right clicking a teleporter remembers it; right clicking a second one links the two both
 * ways. Right clicking into the air forgets the remembered teleporter.
 */
public class ItemFrequencyTransmitter extends ItemIC2 {
    public ItemFrequencyTransmitter(String name) {
        super(name, new Settings().maxCount(1));
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (!world.isClient) {
            NbtCompound nbt = getData(stack);
            boolean hadJustSet = nbt.getBoolean("targetJustSet");
            if (nbt.getBoolean("targetSet") && !hadJustSet) {
                nbt.putBoolean("targetSet", false);
                player.sendMessage(Text.literal("Frequency Transmitter unlinked"), false);
            }

            // 1.12.2 fired a use after a click on a teleporter as well, which this flag swallowed;
            // 1.21 does not, so it is only cleared here to keep stacks from older saves working
            if (hadJustSet) {
                nbt.putBoolean("targetJustSet", false);
            }
            setData(stack, nbt);
        }
        return TypedActionResult.success(stack, world.isClient);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        if (!(world.getBlockEntity(context.getBlockPos()) instanceof TileEntityTeleporter tp)) {
            return ActionResult.PASS;
        }
        if (world.isClient) {
            return ActionResult.SUCCESS;
        }

        link(context.getStack(), context.getPlayer(), tp);
        return ActionResult.SUCCESS;
    }

    /** What a click on a teleporter does: remember it, or link it to the one remembered. */
    public static void link(ItemStack stack, PlayerEntity player, TileEntityTeleporter tp) {
        World world = tp.getWorld();
        BlockPos pos = tp.getPos();
        NbtCompound nbt = getData(stack);
        boolean targetSet = nbt.getBoolean("targetSet");
        BlockPos target = new BlockPos(nbt.getInt("targetX"), nbt.getInt("targetY"), nbt.getInt("targetZ"));
        if (!targetSet) {
            targetSet = true;
            target = pos;
            message(player, "Frequency Transmitter linked to Teleporter.");
        } else if (pos.equals(target)) {
            message(player, "Can't link Teleporter to itself.");
        } else if (tp.hasTarget() && tp.getTarget().equals(target)) {
            message(player, "Teleportation link unchanged.");
        } else if (world.getBlockEntity(target) instanceof TileEntityTeleporter targetTe) {
            tp.setTarget(target);
            targetTe.setTarget(pos);
            message(player, "Teleportation link established.");
        } else {
            // the remembered teleporter is gone
            targetSet = false;
        }

        nbt.putBoolean("targetSet", targetSet);
        nbt.putBoolean("targetJustSet", false);
        nbt.putInt("targetX", target.getX());
        nbt.putInt("targetY", target.getY());
        nbt.putInt("targetZ", target.getZ());
        setData(stack, nbt);
    }

    private static void message(PlayerEntity player, String text) {
        if (player != null) {
            player.sendMessage(Text.literal(text), false);
        }
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        NbtCompound nbt = getData(stack);
        if (nbt.getBoolean("targetSet")) {
            tooltip.add(Text.translatable("frequency_transmitter.tooltip.target",
                    nbt.getInt("targetX"), nbt.getInt("targetY"), nbt.getInt("targetZ")));
        } else {
            tooltip.add(Text.translatable("frequency_transmitter.tooltip.blank"));
        }
    }

    private static NbtCompound getData(ItemStack stack) {
        return stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
    }

    private static void setData(ItemStack stack, NbtCompound nbt) {
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
    }
}
