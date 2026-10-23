package ic2.core.block;

import ic2.api.item.ItemWrapper;
import ic2.core.IC2;
import ic2.core.block.machine.tileentity.TileEntityMagnetizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.block.FenceBlock;
import net.minecraft.block.MapColor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Fabric port of BlockIC2Fence - the iron fence, which a magnetizer turns into a lift.
 *
 * <p>A pole of iron fences (no fence beside it) with a magnetizer facing it pushes a player
 * standing in it upwards, up to 0.5 blocks a tick, 1.5 in metal boots, 0.1 holding the mode key;
 * sneaking lowers them slowly. Without power metal boots still brake a fall. The magnetizers may
 * stand at any height of the pole within 20 blocks; each lift tick costs them 2 EU together.
 * Standing in the pole never hurts on landing.
 */
public class BlockIC2Fence extends FenceBlock {
    private static final int MAX_RANGE = 20;

    public BlockIC2Fence() {
        super(Settings.create().mapColor(MapColor.GRAY).sounds(BlockSoundGroup.METAL).strength(5.0f, 10.0f).requiresTool());
    }

    @Override
    protected void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if (!(entity instanceof PlayerEntity player)) {
            return;
        }

        boolean powered = isPowered(world, pos);
        boolean metalShoes = hasMetalShoes(player);
        boolean descending = player.isSneaking();
        Vec3d velocity = player.getVelocity();
        double vy = velocity.y;
        // as in 1.12.2, where this test is always true
        boolean slow = vy >= -0.25 || vy < 1.6;
        if (slow) {
            player.fallDistance = 0.0F;
        }

        if (!powered) {
            if (descending && !slow && metalShoes) {
                vy *= 0.9;
            }
        } else if (descending) {
            if (!slow) {
                vy *= 0.8;
            }
        } else {
            vy += 0.075;
            if (vy > 0.0) {
                vy *= 1.03;
            }
            double maxSpeed = IC2.keyboard.isAltKeyDown(player) ? 0.1 : metalShoes ? 1.5 : 0.5;
            vy = Math.min(vy, maxSpeed);
        }
        if (vy != velocity.y) {
            player.setVelocity(velocity.x, vy, velocity.z);
        }
    }

    public static boolean hasMetalShoes(PlayerEntity player) {
        ItemStack shoes = player.getEquippedStack(EquipmentSlot.FEET);
        Item item = shoes.getItem();
        return item == Items.IRON_BOOTS || item == Items.GOLDEN_BOOTS || item == Items.CHAINMAIL_BOOTS
                || !shoes.isEmpty() && ItemWrapper.isMetalArmor(shoes, player);
    }

    private static boolean isFence(BlockState state) {
        return state.getBlock() instanceof FenceBlock;
    }

    /** The magnetizer at pos, if it faces back towards the fence on the side; with checkPower only one able to lift. */
    private static TileEntityMagnetizer getMagnetizer(World world, BlockPos pos, Direction side, boolean checkPower) {
        if (world.getBlockEntity(pos) instanceof TileEntityMagnetizer magnetizer) {
            if (side != null && side.getOpposite() != magnetizer.getFacing()) {
                return null;
            }
            if (!checkPower || magnetizer.canBoost()) {
                return magnetizer;
            }
        }
        return null;
    }

    private boolean isPowered(World world, BlockPos start) {
        List<TileEntityMagnetizer> magnetizers = getMagnetizers(world, start, true);
        if (magnetizers.isEmpty()) {
            return false;
        }
        double multiplier = 1.0 / magnetizers.size();
        for (TileEntityMagnetizer magnetizer : magnetizers) {
            magnetizer.boost(multiplier);
        }
        return true;
    }

    /**
     * The magnetizers of the pole the fence belongs to: beside the fence itself, or else at the
     * nearest height up or down the pole (up to 20 blocks) that has any. A fence beside the pole
     * ends it in that direction.
     */
    private List<TileEntityMagnetizer> getMagnetizers(World world, BlockPos start, boolean checkPower) {
        List<TileEntityMagnetizer> ret = new ArrayList<>();
        for (Direction facing : Direction.Type.HORIZONTAL) {
            BlockPos nPos = start.offset(facing);
            if (isFence(world.getBlockState(nPos))) {
                return Collections.emptyList();
            }
            TileEntityMagnetizer te = getMagnetizer(world, nPos, facing, checkPower);
            if (te != null) {
                ret.add(te);
            }
        }
        if (!ret.isEmpty()) {
            return ret;
        }

        boolean[] open = {true, true}; // down, up
        for (int dy = 1; dy <= MAX_RANGE; dy++) {
            boolean found = false;
            for (int dir = 0; dir < 2; dir++) {
                if (!open[dir]) {
                    continue;
                }
                BlockPos center = start.up((dir * 2 - 1) * dy);
                if (!(world.getBlockState(center).getBlock() instanceof BlockIC2Fence)) {
                    open[dir] = false;
                    continue;
                }
                int oldSize = ret.size();
                for (Direction facing : Direction.Type.HORIZONTAL) {
                    BlockPos nPos = center.offset(facing);
                    if (isFence(world.getBlockState(nPos))) {
                        // a fence beside the pole ends it this way, and what was found here does not count
                        open[dir] = false;
                        while (ret.size() > oldSize) {
                            ret.remove(ret.size() - 1);
                        }
                        break;
                    }
                    TileEntityMagnetizer te = getMagnetizer(world, nPos, facing, checkPower);
                    if (te != null) {
                        ret.add(te);
                        found = true;
                    }
                }
            }
            if (found || !open[0] && !open[1]) {
                break;
            }
        }
        return ret;
    }
}
