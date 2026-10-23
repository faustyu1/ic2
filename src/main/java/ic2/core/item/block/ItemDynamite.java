package ic2.core.item.block;

import ic2.api.item.IBoxable;
import ic2.core.block.BlockDynamite;
import ic2.core.block.EntityDynamite;
import ic2.core.block.EntityStickyDynamite;
import ic2.core.init.IC2ClassicBlocks;
import ic2.core.item.ItemIC2;
import net.minecraft.block.BlockState;
import net.minecraft.block.DispenserBlock;
import net.minecraft.block.dispenser.ItemDispenserBehavior;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPointer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Position;
import net.minecraft.world.World;
import net.minecraft.world.WorldEvents;

/**
 * Fabric port of {@code ic2.core.item.block.ItemDynamite}: dynamite and sticky dynamite.
 *
 * <p>Right click in the air throws a lit stick; plain dynamite used on a block is placed there
 * instead, to be set off by redstone or a Dynamite-O-Mote. A dispenser throws it like an arrow.
 */
public class ItemDynamite extends ItemIC2 implements IBoxable {
    public final boolean sticky;

    public ItemDynamite(String name, boolean sticky) {
        super(name, new Item.Settings().maxCount(16));
        this.sticky = sticky;
        DispenserBlock.registerBehavior(this, new DispenseBehavior(sticky));
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (sticky) {
            return ActionResult.PASS;
        }

        World world = context.getWorld();
        BlockPos pos = context.getBlockPos().offset(context.getSide());
        BlockDynamite dynamite = (BlockDynamite) IC2ClassicBlocks.DYNAMITE;
        BlockState placement = world.getBlockState(pos).isAir() ? dynamite.getPlacementState(world, pos, context.getSide()) : null;
        if (placement == null) {
            return ActionResult.FAIL;
        }

        if (!world.isClient) {
            world.setBlockState(pos, placement);
            world.playSound(null, pos, placement.getSoundGroup().getPlaceSound(), SoundCategory.BLOCKS, 1.0F, 0.8F);
        }
        PlayerEntity player = context.getPlayer();
        if (player == null || !player.getAbilities().creativeMode) {
            context.getStack().decrement(1);
        }
        return ActionResult.success(world.isClient);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENTITY_ARROW_SHOOT, SoundCategory.PLAYERS,
                0.5F, 0.4F / (world.random.nextFloat() * 0.4F + 0.8F));
        if (!world.isClient) {
            world.spawnEntity(sticky ? new EntityStickyDynamite(world, player) : new EntityDynamite(world, player));
        }
        if (!player.getAbilities().creativeMode) {
            stack.decrement(1);
        }
        return TypedActionResult.success(stack, world.isClient);
    }

    @Override
    public boolean canBeStoredInToolbox(ItemStack stack) {
        return true;
    }

    /** {@code BehaviorDynamiteDispense}: the projectile behaviour, velocity 1.1 and spread 6. */
    private static final class DispenseBehavior extends ItemDispenserBehavior {
        private final boolean sticky;

        DispenseBehavior(boolean sticky) {
            this.sticky = sticky;
        }

        @Override
        protected ItemStack dispenseSilently(BlockPointer pointer, ItemStack stack) {
            World world = pointer.world();
            Position pos = DispenserBlock.getOutputLocation(pointer);
            Direction facing = pointer.state().get(DispenserBlock.FACING);
            EntityDynamite entity = sticky
                    ? new EntityStickyDynamite(world, pos.getX(), pos.getY(), pos.getZ())
                    : new EntityDynamite(world, pos.getX(), pos.getY(), pos.getZ());
            entity.setVelocity(facing.getOffsetX(), facing.getOffsetY() + 0.1F, facing.getOffsetZ(), 1.1F, 6.0F);
            world.spawnEntity(entity);
            stack.decrement(1);
            return stack;
        }

        @Override
        protected void playSound(BlockPointer pointer) {
            pointer.world().syncWorldEvent(WorldEvents.DISPENSER_LAUNCHES_PROJECTILE, pointer.pos(), 0);
        }
    }
}
