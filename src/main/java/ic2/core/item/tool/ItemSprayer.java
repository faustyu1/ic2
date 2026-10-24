package ic2.core.item.tool;

import ic2.api.item.IBoxable;
import ic2.core.IC2;
import ic2.core.init.IC2ClassicBlocks;
import ic2.core.item.IItemFluidTank;
import ic2.core.item.ItemIC2;
import ic2.core.item.armor.ItemArmorCFPack;
import ic2.core.ref.FluidName;
import ic2.core.util.LiquidUtil;
import java.util.ArrayDeque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import net.minecraft.block.BlockState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Fabric port of {@code ic2.core.item.tool.ItemSprayer}: the construction foam sprayer.
 *
 * <p>It holds 8000 mB of construction foam and takes more from a CF pack worn on the chest - the
 * pack first. Used on a block it sprays soft foam over up to ten free blocks next to the face it
 * hit (one in single mode, the mode key and right click switch), spreading away from the player;
 * a cable it is used on is buried in foam together with the cables joined to it that are not
 * foamed yet. Every foam block costs 100 mB. The canner or a tank fills it, and pointed at a
 * pool of liquid construction foam it sucks up a bucket of it a click.
 *
 * <p>Not ported: filling in scaffolds (the port has none).
 */
public class ItemSprayer extends ItemIC2 implements IItemFluidTank, IBoxable {
    public static final int CAPACITY = 8000;
    public static final int FLUID_PER_FOAM = 100;

    private enum Target {
        Any,
        Cable
    }

    public ItemSprayer() {
        super("foam_sprayer", new Item.Settings().maxCount(1));
    }

    // ---- the tank ----

    @Override
    public long getCapacity(ItemStack stack) {
        return LiquidUtil.mb(CAPACITY);
    }

    @Override
    public boolean canHold(ItemStack stack, Fluid fluid) {
        return fluid != null && fluid.equals(FluidName.construction_foam.getInstance());
    }

    @Override
    public Fluid getCreativeFluid() {
        return FluidName.construction_foam.getInstance();
    }

    @Override
    public boolean isItemBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getItemBarStep(ItemStack stack) {
        return fluidBarStep(stack);
    }

    @Override
    public int getItemBarColor(ItemStack stack) {
        return fluidBarColor(stack);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);
        appendFluidTooltip(stack, tooltip);
        tooltip.add(Text.translatable("tooltip.mode", Text.translatable(isSingle(stack) ? "tooltip.mode.single" : "tooltip.mode.normal")));
    }

    @Override
    public boolean canBeStoredInToolbox(ItemStack stack) {
        return true;
    }

    // ---- mode ----

    private static boolean isSingle(ItemStack stack) {
        return stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt().getInt("mode") != 0;
    }

    /** Ten blocks a spray normally, one in single mode. */
    protected int getMaxFoamBlocks(ItemStack stack) {
        return isSingle(stack) ? 1 : 10;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (!world.isClient && IC2.keyboard.isModeSwitchKeyDown(player)) {
            NbtCompound data = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
            int mode = data.getInt("mode") == 0 ? 1 : 0;
            data.putInt("mode", mode);
            stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(data));
            player.sendMessage(Text.translatable("tooltip.mode",
                    Text.translatable(mode == 0 ? "tooltip.mode.normal" : "tooltip.mode.single")), true);
            return TypedActionResult.success(stack);
        }
        if (!IC2.keyboard.isModeSwitchKeyDown(player) && suckUpFoam(world, player, stack, null)) {
            return TypedActionResult.success(stack, world.isClient);
        }
        return TypedActionResult.pass(stack);
    }

    /**
     * Takes a source block of liquid construction foam the player points at into the tank, as the
     * 1.12.2 sprayer drained the fluid block in front of the block it was used on.
     *
     * @param clicked the block the sprayer was used on, which a pool must be in front of; null when
     *                used in the air
     */
    private boolean suckUpFoam(World world, PlayerEntity player, ItemStack stack, BlockPos clicked) {
        Vec3d eye = player.getEyePos();
        Vec3d end = eye.add(player.getRotationVector().multiply(player.getBlockInteractionRange()));
        net.minecraft.util.hit.BlockHitResult hit = world.raycast(new net.minecraft.world.RaycastContext(eye, end,
                net.minecraft.world.RaycastContext.ShapeType.OUTLINE, net.minecraft.world.RaycastContext.FluidHandling.SOURCE_ONLY, player));
        if (hit.getType() != net.minecraft.util.hit.HitResult.Type.BLOCK || hit.getBlockPos().equals(clicked)) {
            return false;
        }
        BlockPos pos = hit.getBlockPos();
        net.minecraft.fluid.FluidState fluid = world.getFluidState(pos);
        if (!fluid.isStill() || !fluid.getFluid().matchesType(FluidName.construction_foam.getInstance())
                || getCapacity(stack) - getAmount(stack) < LiquidUtil.mb(1000)) {
            return false;
        }
        if (!world.isClient) {
            world.setBlockState(pos, net.minecraft.block.Blocks.AIR.getDefaultState());
            fill(stack, net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant.of(FluidName.construction_foam.getInstance()), LiquidUtil.mb(1000), true);
            world.playSound(null, pos, net.minecraft.sound.SoundEvents.ITEM_BUCKET_FILL, net.minecraft.sound.SoundCategory.BLOCKS, 1.0F, 1.0F);
        }
        return true;
    }

    // ---- spraying ----

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        PlayerEntity player = context.getPlayer();
        if (player == null || IC2.keyboard.isModeSwitchKeyDown(player)) {
            return ActionResult.PASS;
        }
        World world = context.getWorld();
        ItemStack stack = context.getStack();
        if (suckUpFoam(world, player, stack, context.getBlockPos())) {
            return ActionResult.success(world.isClient);
        }
        if (world.isClient) {
            return ActionResult.SUCCESS;
        }

        int available = (int) LiquidUtil.toMb(getAmount(stack));
        ItemStack pack = player.getEquippedStack(EquipmentSlot.CHEST);
        if (!(pack.getItem() instanceof ItemArmorCFPack)) {
            // a CF pack on the back feeds the sprayer as well
            pack = ic2.core.item.armor.jetpack.BackSlot.get(player);
        }
        ItemArmorCFPack cfPack = pack.getItem() instanceof ItemArmorCFPack p && !p.isEmpty(pack) ? p : null;
        if (cfPack != null) {
            available += (int) LiquidUtil.toMb(cfPack.getAmount(pack));
        }

        int maxFoamBlocks = available / FLUID_PER_FOAM;
        if (maxFoamBlocks == 0) {
            return ActionResult.FAIL;
        }
        maxFoamBlocks = Math.min(maxFoamBlocks, getMaxFoamBlocks(stack));

        BlockPos pos = context.getBlockPos();
        Target target;
        if (canPlaceFoam(world, pos, Target.Cable)) {
            target = Target.Cable;
        } else {
            pos = pos.offset(context.getSide());
            target = Target.Any;
        }

        Vec3d view = player.getRotationVector();
        Direction viewFacing = Direction.getFacing(view.x, view.y, view.z);
        int amount = sprayFoam(world, pos, viewFacing.getOpposite(), target, maxFoamBlocks) * FLUID_PER_FOAM;
        if (amount <= 0) {
            return ActionResult.PASS;
        }

        if (cfPack != null) {
            var drained = cfPack.drain(pack, LiquidUtil.mb(amount), true);
            amount -= drained == null ? 0 : (int) LiquidUtil.toMb(drained.amount());
        }
        if (amount > 0) {
            drain(stack, LiquidUtil.mb(amount), true);
        }
        return ActionResult.SUCCESS;
    }

    /**
     * Fills free blocks with foam outwards from {@code pos}, never towards {@code excludedDir}.
     *
     * @return how many blocks were foamed
     */
    public int sprayFoam(World world, BlockPos pos, Direction excludedDir, Target target, int maxFoamBlocks) {
        if (!canPlaceFoam(world, pos, target)) {
            return 0;
        }

        Queue<BlockPos> toCheck = new ArrayDeque<>();
        Set<BlockPos> positions = new LinkedHashSet<>();
        toCheck.add(pos);

        BlockPos current;
        while ((current = toCheck.poll()) != null && positions.size() < maxFoamBlocks) {
            if (canPlaceFoam(world, current, target) && positions.add(current)) {
                for (Direction dir : Direction.values()) {
                    if (dir != excludedDir) {
                        toCheck.add(current.offset(dir));
                    }
                }
            }
        }

        int failedPlacements = 0;
        BlockState foam = IC2ClassicBlocks.FOAM.getDefaultState();
        for (BlockPos targetPos : positions) {
            BlockState state = world.getBlockState(targetPos);
            if (world.getBlockEntity(targetPos) instanceof ic2.core.block.wiring.TileEntityCable cable) {
                if (!cable.foam()) {
                    failedPlacements++;
                }
            } else if (!world.setBlockState(targetPos, foam)) {
                failedPlacements++;
            }
        }

        return positions.size() - failedPlacements;
    }

    private static boolean canPlaceFoam(World world, BlockPos pos, Target target) {
        return switch (target) {
            case Any -> world.getBlockState(pos).isReplaceable();
            case Cable -> world.getBlockEntity(pos) instanceof ic2.core.block.wiring.TileEntityCable cable && !cable.isFoamed();
        };
    }
}
