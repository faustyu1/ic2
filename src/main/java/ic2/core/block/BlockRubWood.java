package ic2.core.block;

import com.mojang.serialization.MapCodec;
import ic2.core.init.IC2ItemVariants;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

/**
 * Fabric port of BlockRubWood - the rubber tree's trunk.
 *
 * <p>Most of the trunk is plain wood, but a few logs carry a resin spot on one side. Tapping a
 * wet spot with a treetap gives resin and dries it out; a dry spot fills up again on its own over
 * time, which is what makes a rubber tree a renewable source of rubber rather than a one-off.
 *
 * <p>In 1.12.2 the eleven combinations were metadata values; here they are one block state
 * property, so the axis of a plain log and the side a spot faces live in the same place they did.
 */
public class BlockRubWood extends Block {
    public static final MapCodec<BlockRubWood> CODEC = createCodec(BlockRubWood::new);
    public static final EnumProperty<RubberWoodState> STATE = EnumProperty.of("state", RubberWoodState.class);

    public BlockRubWood(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState().with(STATE, RubberWoodState.plain_y));
    }

    public static Settings createSettings() {
        return Settings.create()
                .mapColor(net.minecraft.block.MapColor.BROWN)
                .sounds(net.minecraft.sound.BlockSoundGroup.WOOD)
                .strength(2.0F, 2.0F)
                .ticksRandomly();
    }

    @Override
    protected MapCodec<? extends Block> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(STATE);
    }

    /** A log placed by hand is plain, and lies along the axis it was placed against. */
    @Override
    public BlockState getPlacementState(ItemPlacementContext context) {
        return getDefaultState().with(STATE, RubberWoodState.plain(context.getSide().getAxis()));
    }

    /** A dry spot slowly fills up again; a plain log never grows one. */
    @Override
    protected void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (random.nextInt(7) != 0) {
            return;
        }

        RubberWoodState woodState = state.get(STATE);
        if (woodState.canRegenerate()) {
            world.setBlockState(pos, state.with(STATE, woodState.getWet()));
        }
    }

    /**
     * Tapping a wet spot with a treetap gives resin and leaves the spot dry. Anything else - a
     * plain log, a dry spot, an empty hand - is left to the normal right click.
     */
    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!isTreetap(player.getStackInHand(Hand.MAIN_HAND))) {
            return ActionResult.PASS;
        }
        return tap(world, pos, state);
    }

    /**
     * Take the resin out of a wet spot and leave it dry. Shared with the electric treetap, which
     * pays EU for the same thing.
     *
     * @return SUCCESS when a spot was actually tapped, PASS when there was nothing to tap
     */
    public static ActionResult tap(World world, BlockPos pos, BlockState state) {
        RubberWoodState woodState = state.get(STATE);
        if (!woodState.wet) {
            return ActionResult.PASS;
        }

        if (!world.isClient) {
            ic2.core.init.IC2Sounds.play(world, null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, ic2.core.init.IC2Sounds.TOOLS_TREETAP);
            world.setBlockState(pos, state.with(STATE, woodState.getDry()));
            ItemStack resin = resin();
            if (!resin.isEmpty()) {
                resin.setCount(1 + world.random.nextInt(3));
                ItemScatterer.spawn(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, resin);
            }
        }

        return ActionResult.SUCCESS;
    }

    /** Breaking a log that carries a spot sometimes drops the resin that was still in it. */
    @Override
    protected java.util.List<ItemStack> getDroppedStacks(BlockState state, LootContextParameterSet.Builder builder) {
        java.util.List<ItemStack> drops = super.getDroppedStacks(state, builder);
        if (state.get(STATE).isPlain()) {
            return drops;
        }

        ServerWorld world = builder.getWorld();
        if (world != null && world.random.nextInt(6) == 0) {
            ItemStack resin = resin();
            if (!resin.isEmpty()) {
                drops = new java.util.ArrayList<>(drops);
                drops.add(resin);
            }
        }
        return drops;
    }

    private static boolean isTreetap(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof ic2.core.item.ItemTreetap;
    }

    private static ItemStack resin() {
        var item = IC2ItemVariants.get("misc_resource", "resin");
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    /**
     * What one rubber log looks like: either plain along one of the three axes, or standing
     * upright with a dry or wet resin spot on one of its four sides.
     */
    public enum RubberWoodState implements StringIdentifiable {
        plain_y(Direction.Axis.Y, null, false),
        plain_x(Direction.Axis.X, null, false),
        plain_z(Direction.Axis.Z, null, false),
        dry_north(Direction.Axis.Y, Direction.NORTH, false),
        dry_south(Direction.Axis.Y, Direction.SOUTH, false),
        dry_west(Direction.Axis.Y, Direction.WEST, false),
        dry_east(Direction.Axis.Y, Direction.EAST, false),
        wet_north(Direction.Axis.Y, Direction.NORTH, true),
        wet_south(Direction.Axis.Y, Direction.SOUTH, true),
        wet_west(Direction.Axis.Y, Direction.WEST, true),
        wet_east(Direction.Axis.Y, Direction.EAST, true);

        private static final RubberWoodState[] VALUES = values();

        public final Direction.Axis axis;
        public final Direction facing;
        public final boolean wet;

        RubberWoodState(Direction.Axis axis, Direction facing, boolean wet) {
            this.axis = axis;
            this.facing = facing;
            this.wet = wet;
        }

        @Override
        public String asString() {
            return name();
        }

        public boolean isPlain() {
            return facing == null;
        }

        public boolean canRegenerate() {
            return !isPlain() && !wet;
        }

        /** The wet form of this spot; the dry and wet forms are four apart in the same order. */
        public RubberWoodState getWet() {
            if (isPlain()) {
                return this;
            }
            return wet ? this : VALUES[ordinal() + 4];
        }

        public RubberWoodState getDry() {
            return !isPlain() && wet ? VALUES[ordinal() - 4] : this;
        }

        public static RubberWoodState plain(Direction.Axis axis) {
            return switch (axis) {
                case X -> plain_x;
                case Y -> plain_y;
                case Z -> plain_z;
            };
        }

        public static RubberWoodState wet(Direction facing) {
            return switch (facing) {
                case NORTH -> wet_north;
                case SOUTH -> wet_south;
                case WEST -> wet_west;
                case EAST -> wet_east;
                default -> throw new IllegalArgumentException("incompatible facing: " + facing);
            };
        }

        public static RubberWoodState dry(Direction facing) {
            return wet(facing).getDry();
        }
    }
}
