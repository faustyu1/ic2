package ic2.core.crop;

import ic2.core.init.IC2Blocks;
import ic2.core.item.ItemIC2;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

/**
 * Fabric port of ItemCrop - the crop stick, which is planted on farmland as an empty crop.
 *
 * <p>Clicking the side of a block plants in front of it, clicking the farmland itself plants on
 * top, as for any placed block. Sticks used on an existing crop make a cross-crop; that click is
 * handled by the crop.
 */
public class ItemCrop extends ItemIC2 {
    public ItemCrop() {
        super("crop_stick");
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        PlayerEntity player = context.getPlayer();
        ItemStack stack = context.getStack();
        BlockPos pos = context.getBlockPos();

        if (!world.getBlockState(pos).canReplace(new ItemPlacementContext(context))) {
            pos = pos.offset(context.getSide());
        }

        if (stack.isEmpty() || !world.getBlockState(pos.down()).isOf(Blocks.FARMLAND)) {
            return ActionResult.PASS;
        }
        if (player != null && !player.canPlaceOn(pos, context.getSide(), stack)) {
            return ActionResult.PASS;
        }
        if (!world.getBlockState(pos).isReplaceable()) {
            return ActionResult.PASS;
        }

        if (!world.isClient) {
            BlockState state = IC2Blocks.CROP_BLOCK.getDefaultState();
            if (!world.setBlockState(pos, state)) {
                return ActionResult.PASS;
            }

            BlockSoundGroup sound = BlockSoundGroup.WOOD;
            world.playSound(null, pos, sound.getPlaceSound(), SoundCategory.BLOCKS,
                    (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
            world.emitGameEvent(player, GameEvent.BLOCK_PLACE, pos);

            if (player == null || !player.getAbilities().creativeMode) {
                stack.decrement(1);
            }
        }

        return ActionResult.success(world.isClient);
    }
}
