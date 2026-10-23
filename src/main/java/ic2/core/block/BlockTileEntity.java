package ic2.core.block;

import ic2.core.ref.TeBlock;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import java.util.List;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Minimal Fabric port of BlockTileEntity.
 * Original was a Block with Forge extended states, material properties, facing.
 * This version delegates to TeBlock and creates BlockEntities via BlockEntityType.
 */
public class BlockTileEntity extends BlockWithEntity {
    /**
     * Where the machine faces and whether it is working, as block state so the client draws the
     * front on the right side and the working texture - 1.12.2 rendered both from synced
     * tile entity fields. Only plain machine blocks have them; crops and cables are subclasses
     * with states of their own.
     */
    public static final net.minecraft.state.property.DirectionProperty FACING = net.minecraft.state.property.Properties.FACING;
    public static final net.minecraft.state.property.BooleanProperty ACTIVE = net.minecraft.state.property.BooleanProperty.of("active");

    public final TeBlock teBlock;
    private BlockEntityType<? extends TileEntityBlock> blockEntityType;

    public BlockTileEntity(Settings settings, TeBlock teBlock) {
        super(settings);
        this.teBlock = teBlock;
        if (hasMachineStates()) {
            setDefaultState(getStateManager().getDefaultState().with(FACING, Direction.NORTH).with(ACTIVE, false));
        }
    }

    private boolean hasMachineStates() {
        return getClass() == BlockTileEntity.class;
    }

    @Override
    protected void appendProperties(net.minecraft.state.StateManager.Builder<Block, BlockState> builder) {
        if (hasMachineStates()) {
            builder.add(FACING, ACTIVE);
        }
    }

    /**
     * The machine is placed facing the player, among the facings it supports - its front towards
     * the eye, as TileEntityBlock.getPlacementFacing chose it.
     */
    @Override
    public @Nullable BlockState getPlacementState(net.minecraft.item.ItemPlacementContext ctx) {
        BlockState state = super.getPlacementState(ctx);
        if (state == null || !state.contains(FACING)) {
            return state;
        }
        if (teBlock == TeBlock.luminator_flat) {
            // the luminator faces away from the surface it was put on
            return state.with(FACING, ctx.getSide());
        }
        return state.with(FACING, placementFacing(teBlock.getSupportedFacings(), ctx.getPlayer(), ctx.getSide()));
    }

    public static Direction placementFacing(java.util.Set<Direction> supported, @Nullable PlayerEntity placer, @Nullable Direction side) {
        if (supported.isEmpty()) {
            return Direction.DOWN;
        }
        if (placer != null) {
            net.minecraft.util.math.Vec3d dir = placer.getRotationVec(1.0F);
            Direction best = null;
            double maxMatch = Double.NEGATIVE_INFINITY;
            for (Direction facing : supported) {
                net.minecraft.util.math.Vec3i normal = facing.getOpposite().getVector();
                double match = dir.x * normal.getX() + dir.y * normal.getY() + dir.z * normal.getZ();
                if (match > maxMatch) {
                    maxMatch = match;
                    best = facing;
                }
            }
            return best;
        }
        return side != null && supported.contains(side.getOpposite()) ? side.getOpposite() : supported.iterator().next();
    }

    public void setBlockEntityType(BlockEntityType<? extends TileEntityBlock> type) {
        this.blockEntityType = type;
    }

    public BlockEntityType<? extends TileEntityBlock> getBlockEntityType() {
        return blockEntityType;
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return createCodec(BlockTileEntity::new);
    }

    // Fabric 1.21 constructor for codec
    private BlockTileEntity(Settings settings) {
        super(settings);
        this.teBlock = TeBlock.DUMMY;
    }

    /**
     * BlockWithEntity defaults to INVISIBLE (vanilla renders those blocks purely through a
     * BlockEntityRenderer). IC2 machines are plain baked models, so render them as models.
     */
    @Override
    protected BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        if (blockEntityType != null) {
            try {
                // Try to instantiate via TeBlock's TileEntity class
                Class<? extends TileEntityBlock> teClass = teBlock.getTeClass() != null ? (Class<? extends TileEntityBlock>) teBlock.getTeClass() : null;
                if (teClass != null) {
                    return TileEntityBlock.instantiate(teClass, blockEntityType, pos, state);
                }
            } catch (Exception e) {
                // fallback
            }
            // Fallback: try generic
            return blockEntityType.instantiate(pos, state);
        }
        return null;
    }

    /**
     * Opens the machine's guidef-driven screen. Machines without a guidef fall through to the
     * default behaviour, so blocks that only react to wrenching keep working.
     */
    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (world.isClient) {
            return ActionResult.SUCCESS;
        }

        BlockEntity be = world.getBlockEntity(pos);

        // a reactor chamber is just an extension of the reactor, so it opens the reactor's screen -
        // unless it has a screen of its own, as the fluid port with its upgrade slot has
        if (be instanceof ic2.api.reactor.IReactorChamber chamber
                && !(be instanceof TileEntityInventory own && own.hasGui())
                && chamber.getReactorInstance() instanceof TileEntityInventory core) {
            be = core;
        }

        // the block gets the click first: a tank fills from the bucket instead of opening
        if (be instanceof TileEntityBlock tile && tile.onActivated(player, net.minecraft.util.Hand.MAIN_HAND)) {
            return ActionResult.CONSUME;
        }

        if (be instanceof TileEntityInventory inventory && inventory.hasGui()) {
            if (!inventory.permitsAccess(player)) {
                player.sendMessage(inventory.getAccessDeniedMessage(), false);
                return ActionResult.CONSUME;
            }

            player.openHandledScreen(inventory);
            return ActionResult.CONSUME;
        }

        return ActionResult.PASS;
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        // machines only run server side; the client mirrors their state through block state and NBT
        return world.isClient ? null : (tickWorld, pos, tickState, be) -> {
            if (be instanceof TileEntityBlock tile) {
                tile.ensureLoaded();
                tile.tick();
            }
        };
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        super.onPlaced(world, pos, state, placer, itemStack);
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof TileEntityBlock teb) {
            if (!world.isClient) {
                teb.onPlaced(itemStack, placer);
            }
            teb.onLoad();
        }
    }

    /**
     * The drop comes from the block entity, as BlockTileEntity.getDrops built it in 1.12.2: a
     * wrench, or the {@code ignoreWrenchRequirement} setting, gives the block itself, mining it
     * gives what its TeBlock names - often just the machine casing. The contents of the slots are
     * spilled separately when the block goes, unless the block keeps them in its item.
     */
    @Override
    protected List<ItemStack> getDroppedStacks(BlockState state, LootContextParameterSet.Builder builder) {
        BlockEntity be = builder.getOptional(LootContextParameters.BLOCK_ENTITY);
        if (!(be instanceof TileEntityBlock tile)) {
            return List.of();
        }

        ItemStack tool = builder.getOptional(LootContextParameters.TOOL);
        boolean wrench = tool != null && tool.getItem() instanceof ic2.core.item.ItemWrench
                || ic2.core.util.ConfigUtil.getBool(ic2.core.init.MainConfig.get(), "balance/ignoreWrenchRequirement");
        return tile.getSelfDrops(wrench);
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType options) {
        super.appendTooltip(stack, context, tooltip, options);
        if (tooltipTile == null && blockEntityType != null) {
            BlockEntity be = createBlockEntity(BlockPos.ORIGIN, getDefaultState());
            tooltipTile = be instanceof TileEntityBlock teb ? teb : null;
        }
        if (tooltipTile != null) {
            tooltipTile.addInformation(stack, tooltip);
        }
    }

    @Override
    protected void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        super.neighborUpdate(state, world, pos, sourceBlock, sourcePos, notify);
        if (!world.isClient && world.getBlockEntity(pos) instanceof TileEntityBlock tile) {
            tile.onNeighborUpdate();
        }
    }

    /** An explosive among the blocks goes off instead of being blown to pieces. */
    @Override
    protected void onExploded(BlockState state, World world, BlockPos pos, net.minecraft.world.explosion.Explosion explosion,
                              java.util.function.BiConsumer<ItemStack, BlockPos> stackMerger) {
        if (!world.isClient && world.getBlockEntity(pos) instanceof TileEntityBlock tile && tile.onExploded(explosion)) {
            return;
        }
        super.onExploded(state, world, pos, explosion, stackMerger);
    }

    @Override
    protected void onProjectileHit(World world, BlockState state, BlockHitResult hit, net.minecraft.entity.projectile.ProjectileEntity projectile) {
        if (!world.isClient && world.getBlockEntity(hit.getBlockPos()) instanceof TileEntityBlock tile) {
            tile.onProjectileHit(projectile);
        }
    }

    /** A block entity that never sees a world, kept to answer for the item's tooltip. */
    private TileEntityBlock tooltipTile;

    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        BlockEntity be = world.getBlockEntity(pos);
        if (!world.isClient && be instanceof TileEntityBlock teb && teb.onRemovedByPlayer(player)) {
            return super.onBreak(world, pos, state, player);
        }
        if (be instanceof TileEntityBlock teb) {
            teb.onUnloaded();
        }
        return super.onBreak(world, pos, state, player);
    }

    /**
     * Drops what the machine held when it is broken or replaced. The original did this from
     * TileEntityBlock.onUnloaded; on Fabric the block is the one that still sees the block entity.
     */
    @Override
    protected void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock())) {
            BlockEntity be = world.getBlockEntity(pos);
            if (!world.isClient && be instanceof TileEntityBlock tile) {
                tile.onBlockRemoved();
            }
            if (be instanceof net.minecraft.inventory.Inventory inventory
                    && !(be instanceof TileEntityBlock tile && tile.keepsContentsInDrop())) {
                net.minecraft.util.ItemScatterer.spawn(world, pos, inventory);
                world.updateComparators(pos, this);
            }
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }

    @Override
    protected boolean hasComparatorOutput(BlockState state) {
        return true;
    }

    /** The comparator reads the block entity, as the 1.12.2 comparator component did. */
    @Override
    protected int getComparatorOutput(BlockState state, World world, BlockPos pos) {
        return world.getBlockEntity(pos) instanceof TileEntityBlock tile ? tile.getComparatorOutput() : 0;
    }

    /** The energy storage blocks and charge pads can give off a redstone signal, set in their window. */
    @Override
    protected boolean emitsRedstonePower(BlockState state) {
        String name = teBlock.getName();
        return name.equals("batbox") || name.equals("cesu") || name.equals("mfe") || name.equals("mfsu") || name.startsWith("chargepad");
    }

    @Override
    protected int getWeakRedstonePower(BlockState state, net.minecraft.world.BlockView world, BlockPos pos, Direction direction) {
        return world.getBlockEntity(pos) instanceof TileEntityBlock tile ? tile.getRedstoneOutput() : 0;
    }

    @Override
    protected void onEntityCollision(BlockState state, World world, BlockPos pos, net.minecraft.entity.Entity entity) {
        if (world.getBlockEntity(pos) instanceof TileEntityBlock tile) {
            tile.onEntityCollision(entity);
        }
    }

    @Override
    protected net.minecraft.util.shape.VoxelShape getOutlineShape(BlockState state, net.minecraft.world.BlockView world, BlockPos pos,
                                                                    net.minecraft.block.ShapeContext context) {
        if (teBlock == TeBlock.luminator_flat && state.contains(FACING)) {
            return luminatorShape(state.get(FACING));
        }
        return teBlock.getName().startsWith("chargepad") ? CHARGEPAD_SHAPE : super.getOutlineShape(state, world, pos, context);
    }

    /** The luminator is a 1/16 panel on the surface behind it. */
    private static net.minecraft.util.shape.VoxelShape luminatorShape(Direction facing) {
        Direction back = facing.getOpposite();
        double x0 = back.getOffsetX() > 0 ? 15 : 0, x1 = back.getOffsetX() < 0 ? 1 : 16;
        double y0 = back.getOffsetY() > 0 ? 15 : 0, y1 = back.getOffsetY() < 0 ? 1 : 16;
        double z0 = back.getOffsetZ() > 0 ? 15 : 0, z1 = back.getOffsetZ() < 0 ? 1 : 16;
        return Block.createCuboidShape(x0, y0, z0, x1, y1, z1);
    }

    private static final net.minecraft.util.shape.VoxelShape CHARGEPAD_SHAPE = Block.createCuboidShape(0.0, 0.0, 0.0, 16.0, 15.0, 16.0);

    public ItemStack getItemStack(TeBlock teBlock) {
        return new ItemStack(this.asItem());
    }

    public TeBlock getTeBlock() {
        return teBlock;
    }
}
