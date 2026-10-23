package ic2.core.block.machine.tileentity;

import ic2.api.item.ITerraformingBP;
import ic2.core.block.invslot.InvSlot;
import ic2.core.block.invslot.InvSlotConsumable;
import ic2.core.init.IC2Blocks;
import ic2.core.ref.TeBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Fabric port of TileEntityTerra - the terraformer.
 *
 * <p>A terraforming blueprint is put in by right clicking with it and taken out by right clicking
 * again; there is no window. Each tick the terraformer picks a random column around itself and
 * lets the blueprint work on it. A successful change costs the blueprint's EU (2000 for chilling
 * up to 8000 for mushroom), and the next column is then picked close to the last one, within a
 * tenth of the range. A failed attempt costs a tenth of that and picks the next column
 * anywhere in a square that grows with each failure, from a fifth of the range up to the full
 * range after four. The blueprint's work happens in {@link ic2.core.item.tfbp.ItemTfbp}.
 * 100 000 EU, tier 4.
 */
public class TileEntityTerraformer extends TileEntityElectricMachine {
    public int failedAttempts = 0;
    private BlockPos lastPos;
    public int inactiveTicks = 0;
    public final InvSlotConsumable tfbpSlot;

    public TileEntityTerraformer(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type == null ? IC2Blocks.TERRAFORMER_BE : type, pos, state, TeBlock.terraformer, 100000, 4);
        this.tfbpSlot = new InvSlotConsumable(this, "tfbp", InvSlot.Access.I, 1, InvSlot.InvSide.ANY) {
            @Override
            public boolean accepts(ItemStack stack) {
                return stack.getItem() instanceof ITerraformingBP;
            }
        };
    }

    public TileEntityTerraformer(BlockPos pos, BlockState state) {
        this(null, pos, state);
    }

    @Override
    public void tick() {
        if (world == null || world.isClient) {
            return;
        }

        boolean newActive = false;
        ItemStack stack = tfbpSlot.get();
        if (!stack.isEmpty() && stack.getItem() instanceof ITerraformingBP tfbp) {
            if (getEnergy() >= tfbp.getConsume(stack)) {
                newActive = true;
                BlockPos nextPos;
                if (lastPos != null) {
                    int range = tfbp.getRange(stack) / 10;
                    nextPos = new BlockPos(
                            lastPos.getX() - world.random.nextInt(range + 1) + world.random.nextInt(range + 1),
                            pos.getY(),
                            lastPos.getZ() - world.random.nextInt(range + 1) + world.random.nextInt(range + 1));
                } else {
                    if (failedAttempts > 4) {
                        failedAttempts = 4;
                    }

                    int range = tfbp.getRange(stack) * (failedAttempts + 1) / 5;
                    nextPos = new BlockPos(
                            pos.getX() - world.random.nextInt(range + 1) + world.random.nextInt(range + 1),
                            pos.getY(),
                            pos.getZ() - world.random.nextInt(range + 1) + world.random.nextInt(range + 1));
                }

                if (tfbp.terraform(stack, world, nextPos)) {
                    useEnergy(tfbp.getConsume(stack));
                    failedAttempts = 0;
                    lastPos = nextPos;
                } else {
                    useEnergy(tfbp.getConsume(stack) / 10.0);
                    failedAttempts++;
                    lastPos = null;
                }
            }
        }

        if (newActive) {
            inactiveTicks = 0;
            setActive(true);
        } else if (getActive() && inactiveTicks++ > 30) {
            setActive(false);
        }
    }

    /** A right click takes the blueprint out, or puts the one in hand in. */
    @Override
    public boolean onActivated(PlayerEntity player, Hand hand) {
        if (player.isSneaking() || world.isClient) {
            return true;
        }

        if (ejectBlueprint()) {
            return true;
        }

        ItemStack held = player.getStackInHand(hand);
        if (!held.isEmpty() && held.getItem() instanceof ITerraformingBP tfbp && tfbp.canInsert(held, player, world, pos)) {
            ItemStack inserted = held.copyWithCount(1);
            if (!player.getAbilities().creativeMode) {
                held.decrement(1);
            }
            insertBlueprint(inserted);
        }
        return true;
    }

    private boolean ejectBlueprint() {
        ItemStack stack = tfbpSlot.get();
        if (stack.isEmpty()) {
            return false;
        }

        ItemEntity item = new ItemEntity(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack.copy());
        item.setToDefaultPickupDelay();
        world.spawnEntity(item);
        tfbpSlot.clear();
        markDirty();
        return true;
    }

    /** Puts a blueprint into the empty slot. */
    public void insertBlueprint(ItemStack tfbp) {
        if (!tfbpSlot.isEmpty()) {
            throw new IllegalStateException("not empty");
        }
        tfbpSlot.put(tfbp);
        markDirty();
    }

    /** The highest full solid block in the column, starting yOffset above pos; null if there is none. */
    public static BlockPos getFirstSolidBlockFrom(World world, BlockPos pos, int yOffset) {
        BlockPos.Mutable ret = new BlockPos.Mutable(pos.getX(), pos.getY() + yOffset, pos.getZ());
        while (ret.getY() >= world.getBottomY()) {
            if (world.getBlockState(ret).isSolidBlock(world, ret)) {
                return ret.toImmutable();
            }
            ret.move(0, -1, 0);
        }
        return null;
    }

    /** The highest block that is not air in the column, starting yOffset above pos; null if there is none. */
    public static BlockPos getFirstBlockFrom(World world, BlockPos pos, int yOffset) {
        BlockPos.Mutable ret = new BlockPos.Mutable(pos.getX(), pos.getY() + yOffset, pos.getZ());
        while (ret.getY() >= world.getBottomY()) {
            if (!world.isAir(ret)) {
                return ret.toImmutable();
            }
            ret.move(0, -1, 0);
        }
        return null;
    }

    /**
     * Replaces ground in a column. Downwards it replaces the first {@code from} block at or below
     * pos; upwards it goes down through the {@code from} blocks starting at pos and replaces the
     * lowest of them, so a layer is eaten from below.
     *
     * @return whether a block was replaced
     */
    public static boolean switchGround(World world, BlockPos pos, Block from, BlockState to, boolean upwards) {
        BlockPos.Mutable cPos = new BlockPos.Mutable(pos.getX(), pos.getY(), pos.getZ());
        while (cPos.getY() >= world.getBottomY()) {
            Block block = world.getBlockState(cPos).getBlock();
            if (upwards && block != from || !upwards && block == from) {
                break;
            }
            cPos.move(0, -1, 0);
        }

        if ((!upwards || cPos.getY() != pos.getY()) && (upwards || cPos.getY() >= world.getBottomY())) {
            world.setBlockState(upwards ? cPos.up() : cPos.toImmutable(), to);
            return true;
        }
        return false;
    }
}
