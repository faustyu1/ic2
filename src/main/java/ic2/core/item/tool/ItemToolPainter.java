package ic2.core.item.tool;

import ic2.core.IC2;
import ic2.core.block.BlockWall;
import ic2.core.block.wiring.TileEntityCable;
import ic2.core.init.IC2ItemVariants;
import ic2.core.init.IC2Sounds;
import ic2.core.item.ItemIC2;
import ic2.core.util.Ic2Color;
import java.util.List;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.sound.SoundCategory;
import net.minecraft.state.property.Property;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Fabric port of ItemToolPainter - a paint roller in one of sixteen colours.
 *
 * <p>It paints IC2 cables that take paint, construction foam walls, the vanilla blocks that come
 * in the sixteen dye colours (wool, carpet, stained glass and panes, terracotta, concrete and
 * concrete powder - plain glass, panes and terracotta take the colour too) and sheep. A painter
 * lasts 32 coats; the last one leaves a blank painter, unless automatic refill is on (the mode
 * key and right click) and a dye of the colour is in the inventory to fill it again.
 *
 * <p>1.12.2 kept the colour in the damage value of one item; since the flattening every colour
 * is its own item, {@code ic2:painter_<colour>}, and the blank one is {@code ic2:painter}.
 */
public class ItemToolPainter extends ItemIC2 {
    /** Coats a painter gives before it runs dry; the 32nd uses up the last bit of damage. */
    public static final int MAX_DAMAGE = 31;

    /** The vanilla blocks that come in every dye colour, named {@code <colour>_<suffix>}. */
    private static final List<String> DYED_SUFFIXES = List.of(
            "stained_glass_pane", "stained_glass", "concrete_powder", "concrete", "terracotta", "carpet", "wool");

    /** The plain blocks a first coat turns into their dyed kind. */
    private static final List<String[]> UNDYED = List.of(
            new String[] {"glass", "stained_glass"},
            new String[] {"glass_pane", "stained_glass_pane"},
            new String[] {"terracotta", "terracotta"});

    private final Ic2Color color;

    /** @param color the paint, or null for the blank painter */
    public ItemToolPainter(String name, Ic2Color color) {
        super(name, color == null ? new Item.Settings().maxCount(1) : new Item.Settings().maxCount(1).maxDamage(MAX_DAMAGE));
        this.color = color;
    }

    public Ic2Color getColor() {
        return color;
    }

    /** Sheep are painted through the entity hook, since a painter is no dye to the sheep. */
    public static void registerEvents() {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            ItemStack stack = player.getStackInHand(hand);
            if (!(stack.getItem() instanceof ItemToolPainter painter) || painter.color == null
                    || !(entity instanceof SheepEntity sheep) || sheep.getColor() == painter.color.mcColor) {
                return ActionResult.PASS;
            }
            if (!world.isClient) {
                sheep.setColor(painter.color.mcColor);
                painter.damagePainter(player, hand);
            }
            return ActionResult.SUCCESS;
        });
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        PlayerEntity player = context.getPlayer();
        if (color == null || player == null || IC2.keyboard.isModeSwitchKeyDown(player)) {
            return ActionResult.PASS;
        }

        World world = context.getWorld();
        BlockPos pos = context.getBlockPos();
        if (!paint(world, pos, color)) {
            return ActionResult.PASS;
        }

        if (!world.isClient) {
            damagePainter(player, context.getHand());
            world.playSound(null, pos, IC2Sounds.TOOLS_PAINTER, SoundCategory.PLAYERS, 1.0f, 1.0f);
        }
        return ActionResult.success(world.isClient);
    }

    /**
     * Gives the block at the position the colour, if it takes paint and has another colour now.
     *
     * @return whether the block changed
     */
    public static boolean paint(World world, BlockPos pos, Ic2Color color) {
        BlockState state = world.getBlockState(pos);

        if (world.getBlockEntity(pos) instanceof TileEntityCable cable) {
            return cable.recolor(color);
        }
        if (world.getBlockEntity(pos) instanceof ic2.core.block.TileEntityWall obscuredWall) {
            return obscuredWall.recolor(color);
        }
        if (state.getBlock() instanceof BlockWall wall) {
            return world.isClient ? wall.color != color : wall.recolor(world, pos, color);
        }

        Block dyed = dyedVariant(state.getBlock(), color);
        if (dyed == null || dyed == state.getBlock()) {
            return false;
        }
        if (!world.isClient) {
            world.setBlockState(pos, copyProperties(state, dyed.getDefaultState()));
        }
        return true;
    }

    /** The block a vanilla block becomes in that colour, or null when it takes no paint. */
    static Block dyedVariant(Block block, Ic2Color color) {
        Identifier id = Registries.BLOCK.getId(block);
        if (!id.getNamespace().equals("minecraft")) {
            return null;
        }

        String path = id.getPath();
        for (String suffix : DYED_SUFFIXES) {
            for (Ic2Color other : Ic2Color.values) {
                if (path.equals(other.mcColor.getName() + "_" + suffix)) {
                    return Registries.BLOCK.get(Identifier.ofVanilla(color.mcColor.getName() + "_" + suffix));
                }
            }
        }
        for (String[] undyed : UNDYED) {
            if (path.equals(undyed[0])) {
                return Registries.BLOCK.get(Identifier.ofVanilla(color.mcColor.getName() + "_" + undyed[1]));
            }
        }
        return null;
    }

    /** Keeps what the old block had that the new one has too - a pane's connections, water. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static BlockState copyProperties(BlockState from, BlockState to) {
        for (Property property : from.getProperties()) {
            if (to.contains(property)) {
                to = to.with(property, from.get(property));
            }
        }
        return to;
    }

    /** One coat used; the last one leaves a blank painter, or a refilled one. */
    public void damagePainter(PlayerEntity player, Hand hand) {
        if (player.getAbilities().creativeMode) {
            return;
        }

        ItemStack stack = player.getStackInHand(hand);
        if (stack.getDamage() < stack.getMaxDamage()) {
            stack.setDamage(stack.getDamage() + 1);
            return;
        }

        if (isAutoRefill(stack) && consumeDye(player)) {
            stack.setDamage(0);
            return;
        }

        Item blank = IC2ItemVariants.get("painter", null);
        player.setStackInHand(hand, blank == null ? ItemStack.EMPTY : new ItemStack(blank));
    }

    /** Takes one dye of the painter's colour out of the inventory. */
    private boolean consumeDye(PlayerEntity player) {
        TagKey<Item> dyes = TagKey.of(RegistryKeys.ITEM, Identifier.of("c", "dyes/" + color.mcColor.getName()));
        for (ItemStack stack : player.getInventory().main) {
            if (!stack.isEmpty() && stack.isIn(dyes)) {
                stack.decrement(1);
                return true;
            }
        }
        return false;
    }

    public static boolean isAutoRefill(ItemStack stack) {
        return stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt().getBoolean("autoRefill");
    }

    /** The mode key and right click switch automatic refill on and off. */
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (world.isClient || color == null || !IC2.keyboard.isModeSwitchKeyDown(player)) {
            return TypedActionResult.pass(stack);
        }

        boolean refill = !isAutoRefill(stack);
        NbtCompound nbt = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
        nbt.putBoolean("autoRefill", refill);
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
        player.sendMessage(Text.literal(refill
                ? "Painter automatic refill mode enabled"
                : "Painter automatic refill mode disabled"), false);
        return TypedActionResult.success(stack);
    }

    /** The colour, as the dye's own name. */
    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        if (color != null) {
            tooltip.add(Text.translatable("color.minecraft." + color.mcColor.getName()));
        }
    }
}
