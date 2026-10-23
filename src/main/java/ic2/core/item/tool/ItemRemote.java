package ic2.core.item.tool;

import ic2.core.block.BlockDynamite;
import ic2.core.init.IC2ClassicBlocks;
import ic2.core.init.IC2Sounds;
import ic2.core.item.ItemIC2;
import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Fabric port of {@code ic2.core.item.tool.ItemRemote}: the Dynamite-O-Mote.
 *
 * <p>Used on a placed stick of dynamite it links it (the stick shows its remote cap) or, if this
 * remote linked it, unlinks it; right click in the air sets off every linked stick in a loaded
 * chunk. Sticks in unloaded chunks stay on the list for the next try. 1.12.2 kept the count of
 * links in the damage value; here it is the length of the list.
 */
public class ItemRemote extends ItemIC2 {
    public ItemRemote() {
        super("remote", new Item.Settings().maxCount(1));
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        if (world.isClient) {
            return ActionResult.SUCCESS;
        }

        BlockPos pos = context.getBlockPos();
        BlockState state = world.getBlockState(pos);
        if (!state.isOf(IC2ClassicBlocks.DYNAMITE)) {
            return ActionResult.SUCCESS;
        }

        ItemStack stack = context.getStack();
        if (!state.get(BlockDynamite.LINKED)) {
            addRemote(pos, stack);
            world.setBlockState(pos, state.with(BlockDynamite.LINKED, true));
        } else {
            int index = hasRemote(pos, stack);
            if (index > -1) {
                world.setBlockState(pos, state.with(BlockDynamite.LINKED, false));
                removeRemote(index, stack);
            } else if (context.getPlayer() != null) {
                context.getPlayer().sendMessage(Text.literal("This dynamite stick is not linked to this remote, cannot unlink."), false);
            }
        }

        return ActionResult.SUCCESS;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (!world.isClient) {
            world.playSound(null, player.getX(), player.getY(), player.getZ(), IC2Sounds.TOOLS_DYNAMITEOMOTE, SoundCategory.PLAYERS, 1.0F, 1.0F);
            launchRemotes(world, stack, player);
        }
        return TypedActionResult.success(stack, world.isClient);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        int linked = getCoords(stack).size();
        if (linked > 0) {
            tooltip.add(Text.literal("Linked to " + linked + " dynamite"));
        }
    }

    private static NbtList getCoords(ItemStack stack) {
        return stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt().getList("coords", NbtElement.COMPOUND_TYPE);
    }

    private static void setCoords(ItemStack stack, NbtList coords) {
        NbtCompound data = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
        data.put("coords", coords);
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(data));
    }

    public static void addRemote(BlockPos pos, ItemStack remote) {
        NbtList coords = getCoords(remote);
        NbtCompound coord = new NbtCompound();
        coord.putInt("x", pos.getX());
        coord.putInt("y", pos.getY());
        coord.putInt("z", pos.getZ());
        coords.add(coord);
        setCoords(remote, coords);
    }

    /** Sets off the linked sticks that are loaded and forgets them, and forgets those that are gone. */
    public static void launchRemotes(World world, ItemStack remote, PlayerEntity player) {
        NbtList coords = getCoords(remote);
        int i = 0;
        while (i < coords.size()) {
            NbtCompound coord = coords.getCompound(i);
            BlockPos pos = new BlockPos(coord.getInt("x"), coord.getInt("y"), coord.getInt("z"));
            if (world.isChunkLoaded(pos)) {
                BlockState state = world.getBlockState(pos);
                if (state.isOf(IC2ClassicBlocks.DYNAMITE) && state.get(BlockDynamite.LINKED)) {
                    BlockDynamite.explode(world, pos, player, false);
                }
                coords.remove(i);
            } else {
                i++;
            }
        }
        setCoords(remote, coords);
    }

    /** @return the index of the position in the remote's list, or -1 */
    public static int hasRemote(BlockPos pos, ItemStack remote) {
        NbtList coords = getCoords(remote);
        for (int i = 0; i < coords.size(); i++) {
            NbtCompound coord = coords.getCompound(i);
            if (coord.getInt("x") == pos.getX() && coord.getInt("y") == pos.getY() && coord.getInt("z") == pos.getZ()) {
                return i;
            }
        }
        return -1;
    }

    public static void removeRemote(int index, ItemStack remote) {
        NbtList coords = getCoords(remote);
        if (index >= 0 && index < coords.size()) {
            coords.remove(index);
            setCoords(remote, coords);
        }
    }
}
