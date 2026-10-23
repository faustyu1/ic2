package ic2.core.item.tool;

import ic2.api.event.LaserEvent;
import ic2.api.item.ElectricItem;
import ic2.core.IC2;
import ic2.core.init.IC2Sounds;
import ic2.core.item.ItemElectric;
import java.util.List;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Rarity;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Fabric port of {@code ic2.core.item.tool.ItemToolMiningLaser}: a gun that fires
 * {@link EntityMiningLaser} shots, in eight modes switched with the mode key and right click.
 *
 * <ul>
 * <li>mining - unlimited range, power 5, 1250 EU;</li>
 * <li>low-focus - range 4, power 5, one block, 100 EU;</li>
 * <li>long-range - power 20, 5000 EU;</li>
 * <li>horizontal - aimed at a block, a level shot at the height of that block, 3000 EU;</li>
 * <li>super-heat - power 8, smelts what it breaks, 2500 EU;</li>
 * <li>scatter - 5x5 shots of power 12 fanning out, 10000 EU;</li>
 * <li>explosive - power 12, blows up on the first hit, 5000 EU;</li>
 * <li>3x3 - aimed at a block, a level 3x3 tunnel, or a vertical 3x3 shaft when aimed steeply,
 * 3000 EU.</li>
 * </ul>
 *
 * <p>The horizontal and 3x3 modes act when the laser is used on a block; used in the air they cost
 * their entry of the right-click table (0 and 7500 EU) and fire nothing, as in 1.12.2. The sound
 * that went to the clients as an item event is played from the server.
 */
public class ItemMiningLaser extends ItemElectric {
    public static final int MODE_MINING = 0;
    public static final int MODE_LOW_FOCUS = 1;
    public static final int MODE_LONG_RANGE = 2;
    public static final int MODE_HORIZONTAL = 3;
    public static final int MODE_SUPER_HEAT = 4;
    public static final int MODE_SCATTER = 5;
    public static final int MODE_EXPLOSIVE = 6;
    public static final int MODE_3X3 = 7;
    public static final int MODE_COUNT = 8;

    private static final int[] CONSUME = {1250, 100, 5000, 0, 2500, 10000, 5000, 7500};
    private static final double AIMED_CONSUME = 3000.0;
    private static final String[] MODE_KEYS = {"mining", "lowFocus", "longRange", "horizontal", "superHeat", "scatter", "explosive", "3x3"};

    public ItemMiningLaser() {
        super("mining_laser", 3, 300000, 512, new Settings().maxCount(1).rarity(Rarity.UNCOMMON));
    }

    public static int getMode(ItemStack stack) {
        return stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt().getInt("laserSetting");
    }

    public static void setMode(ItemStack stack, int mode) {
        NbtCompound data = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
        data.putInt("laserSetting", mode);
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(data));
    }

    public static Text getModeName(int mode) {
        return Text.translatable("tooltip.mode." + MODE_KEYS[Math.floorMod(mode, MODE_COUNT)]);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);
        int mode = getMode(stack);
        if (mode >= 0 && mode < MODE_COUNT) {
            tooltip.add(Text.translatable("tooltip.mode", getModeName(mode)));
        }
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (world.isClient) {
            return TypedActionResult.pass(stack);
        }

        int laserSetting = getMode(stack);
        if (IC2.keyboard.isModeSwitchKeyDown(player)) {
            laserSetting = (laserSetting + 1) % MODE_COUNT;
            setMode(stack, laserSetting);
            player.sendMessage(Text.translatable("tooltip.mode", getModeName(laserSetting)), true);
            return TypedActionResult.success(stack);
        }

        if (!ElectricItem.manager.use(stack, CONSUME[Math.floorMod(laserSetting, MODE_COUNT)], player)) {
            return TypedActionResult.fail(stack);
        }

        boolean shot = switch (laserSetting) {
            case MODE_MINING -> shootLaser(stack, world, player, Float.POSITIVE_INFINITY, 5.0F, Integer.MAX_VALUE, false, false);
            case MODE_LOW_FOCUS -> shootLaser(stack, world, player, 4.0F, 5.0F, 1, false, false);
            case MODE_LONG_RANGE -> shootLaser(stack, world, player, Float.POSITIVE_INFINITY, 20.0F, Integer.MAX_VALUE, false, false);
            case MODE_SUPER_HEAT -> shootLaser(stack, world, player, Float.POSITIVE_INFINITY, 8.0F, Integer.MAX_VALUE, false, true);
            case MODE_SCATTER -> {
                scatter(stack, world, player);
                yield true;
            }
            case MODE_EXPLOSIVE -> shootLaser(stack, world, player, Float.POSITIVE_INFINITY, 12.0F, Integer.MAX_VALUE, true, false);
            default -> false;
        };
        if (shot) {
            playShotSound(world, player, laserSetting);
        }

        return TypedActionResult.success(stack);
    }

    /** Twenty-five shots in a 5x5 fan, the side ones a block apart at eight blocks out. */
    private void scatter(ItemStack stack, World world, PlayerEntity player) {
        Vec3d look = player.getRotationVector();
        Vec3d right = look.crossProduct(new Vec3d(0.0, 1.0, 0.0));
        if (right.lengthSquared() < 1.0E-4) {
            double angle = Math.toRadians(player.getYaw()) - Math.PI / 2;
            right = new Vec3d(Math.sin(angle), 0.0, -Math.cos(angle));
        } else {
            right = right.normalize();
        }

        Vec3d up = right.crossProduct(look);
        look = look.multiply(8.0);

        for (int r = -2; r <= 2; r++) {
            for (int u = -2; u <= 2; u++) {
                Vec3d dir = look.add(right.multiply(r)).add(up.multiply(u)).normalize();
                shootLaser(stack, world, dir, player, Float.POSITIVE_INFINITY, 12.0F, Integer.MAX_VALUE, false, false);
            }
        }
    }

    /**
     * The horizontal and 3x3 modes, aimed at a block. The rest go on to {@link #use}, which is
     * where the client sends them when this passes.
     */
    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        PlayerEntity player = context.getPlayer();
        ItemStack stack = context.getStack();
        int mode = getMode(stack);
        if (player == null || IC2.keyboard.isModeSwitchKeyDown(player) || mode != MODE_HORIZONTAL && mode != MODE_3X3) {
            return ActionResult.PASS;
        }

        World world = context.getWorld();
        if (world.isClient) {
            return ActionResult.SUCCESS;
        }

        BlockPos pos = context.getBlockPos();
        Vec3d dir = player.getRotationVector();
        double angle = dir.y;

        if (Math.abs(angle) < 1.0 / Math.sqrt(2.0)) {
            if (ElectricItem.manager.use(stack, AIMED_CONSUME, player)) {
                dir = new Vec3d(dir.x, 0.0, dir.z).normalize();
                Vec3d eye = player.getEyePos();
                Vec3d start = adjustStartPos(new Vec3d(eye.x, pos.getY() + 0.5, eye.z), dir);
                if (shootLaser(stack, world, start, dir, player, Float.POSITIVE_INFINITY, 5.0F, Integer.MAX_VALUE, false, false)) {
                    if (mode == MODE_3X3) {
                        shootLaser(stack, world, start.add(0.0, -1.0, 0.0), dir, player, Float.POSITIVE_INFINITY, 5.0F, Integer.MAX_VALUE, false, false);
                        shootLaser(stack, world, start.add(0.0, 1.0, 0.0), dir, player, Float.POSITIVE_INFINITY, 5.0F, Integer.MAX_VALUE, false, false);
                        Direction facing = player.getHorizontalFacing();
                        // the side shots go across the way the player faces
                        boolean alongX = facing == Direction.SOUTH || facing == Direction.NORTH;
                        for (int side = -1; side <= 1; side += 2) {
                            for (int dy = -1; dy <= 1; dy++) {
                                Vec3d offset = alongX ? new Vec3d(side, dy, 0.0) : new Vec3d(0.0, dy, side);
                                shootLaser(stack, world, start.add(offset), dir, player, Float.POSITIVE_INFINITY, 5.0F, Integer.MAX_VALUE, false, false);
                            }
                        }
                    }
                    playShotSound(world, player, mode);
                }
            }
        } else if (mode == MODE_3X3) {
            if (ElectricItem.manager.use(stack, AIMED_CONSUME, player)) {
                dir = new Vec3d(0.0, dir.y, 0.0).normalize();
                Vec3d eye = player.getEyePos();
                Vec3d start = adjustStartPos(new Vec3d(pos.getX() + 0.5, eye.y, pos.getZ() + 0.5), dir);
                if (shootLaser(stack, world, start, dir, player, Float.POSITIVE_INFINITY, 5.0F, Integer.MAX_VALUE, false, false)) {
                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dz = -1; dz <= 1; dz++) {
                            if (dx == 0 && dz == 0) continue;
                            shootLaser(stack, world, start.add(dx, 0.0, dz), dir, player, Float.POSITIVE_INFINITY, 5.0F, Integer.MAX_VALUE, false, false);
                        }
                    }
                    playShotSound(world, player, mode);
                }
            }
        } else {
            player.sendMessage(Text.literal("Mining laser aiming angle too steep"), true);
        }

        return ActionResult.CONSUME;
    }

    private static Vec3d adjustStartPos(Vec3d pos, Vec3d dir) {
        return pos.add(dir.multiply(0.2));
    }

    public boolean shootLaser(ItemStack stack, World world, LivingEntity owner, float range, float power, int blockBreaks, boolean explosive, boolean smelt) {
        return shootLaser(stack, world, owner.getRotationVector(), owner, range, power, blockBreaks, explosive, smelt);
    }

    public boolean shootLaser(ItemStack stack, World world, Vec3d dir, LivingEntity owner, float range, float power, int blockBreaks,
                              boolean explosive, boolean smelt) {
        Vec3d start = adjustStartPos(owner.getEyePos(), dir);
        return shootLaser(stack, world, start, dir, owner, range, power, blockBreaks, explosive, smelt);
    }

    public boolean shootLaser(ItemStack stack, World world, Vec3d start, Vec3d dir, LivingEntity owner, float range, float power,
                              int blockBreaks, boolean explosive, boolean smelt) {
        EntityMiningLaser entity = new EntityMiningLaser(world, start, dir, owner, range, power, blockBreaks, explosive);
        LaserEvent.LaserShootEvent event = new LaserEvent.LaserShootEvent(world, entity, owner, range, power, blockBreaks,
                explosive, smelt, stack);
        if (!LaserEvent.LaserShootEvent.EVENT.invoker().allow(event)) {
            return false;
        }

        entity.copyDataFromEvent(event);
        world.spawnEntity(entity);
        return true;
    }

    private static void playShotSound(World world, PlayerEntity player, int mode) {
        SoundEvent sound = switch (mode) {
            case MODE_LOW_FOCUS -> IC2Sounds.TOOLS_MINING_LASER_MINING_LASER_LOW_FOCUS;
            case MODE_LONG_RANGE -> IC2Sounds.TOOLS_MINING_LASER_MINING_LASER_LONG_RANGE;
            case MODE_SCATTER, MODE_3X3 -> IC2Sounds.TOOLS_MINING_LASER_MINING_LASER_SCATTER;
            case MODE_EXPLOSIVE -> IC2Sounds.TOOLS_MINING_LASER_MINING_LASER_EXPLOSIVE;
            default -> IC2Sounds.TOOLS_MINING_LASER_MINING_LASER;
        };
        world.playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundCategory.PLAYERS, 1.0F, 1.0F);
    }

    /** A tool is not a battery: it never powers anything else, as in 1.12.2. */
    @Override
    public boolean canProvideEnergy(ItemStack stack) {
        return false;
    }
}
