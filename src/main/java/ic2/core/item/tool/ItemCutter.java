package ic2.core.item.tool;

import ic2.core.block.wiring.BlockCable;
import ic2.core.block.wiring.CableType;
import ic2.core.init.IC2ItemVariants;
import ic2.core.item.ItemGradualInt;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.registry.Registries;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Fabric port of {@code ic2.core.item.tool.ItemToolCutter}: the insulation cutters.
 *
 * <p>Right clicking a cable adds a layer of insulation for one rubber and one point of wear;
 * sneaking takes a layer off for three points and drops the rubber back. Since the port gives
 * every insulation level its own block, that means swapping the block for its neighbour in the
 * series rather than editing a field.
 */
public class ItemCutter extends ItemGradualInt {

    public ItemCutter() {
        super("cutter", 60);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        BlockPos pos = context.getBlockPos();
        PlayerEntity player = context.getPlayer();
        BlockState state = world.getBlockState(pos);

        if (player == null || !(state.getBlock() instanceof BlockCable cable)) {
            return ActionResult.PASS;
        }

        return player.isSneaking()
                ? removeInsulation(world, pos, state, cable, player, context.getStack())
                : addInsulation(world, pos, state, cable, player, context.getStack());
    }

    private ActionResult addInsulation(World world, BlockPos pos, BlockState state, BlockCable cable,
                                       PlayerEntity player, ItemStack tool) {
        CableType type = cable.getCableType();
        int insulation = cable.getInsulation();
        if (insulation >= type.maxInsulation) {
            return ActionResult.PASS;
        }

        Block target = cableBlock(type, insulation + 1);
        if (target == null || !takeRubber(player)) {
            return ActionResult.PASS;
        }

        if (!world.isClient) {
            world.setBlockState(pos, copyConnections(state, target.getDefaultState()));
            damage(tool, player, 1);
        }

        return ActionResult.success(world.isClient);
    }

    private ActionResult removeInsulation(World world, BlockPos pos, BlockState state, BlockCable cable,
                                          PlayerEntity player, ItemStack tool) {
        int insulation = cable.getInsulation();
        if (insulation <= 0) {
            return ActionResult.PASS;
        }

        Block target = cableBlock(cable.getCableType(), insulation - 1);
        if (target == null) {
            return ActionResult.PASS;
        }

        if (!world.isClient) {
            world.setBlockState(pos, copyConnections(state, target.getDefaultState()));
            damage(tool, player, 3);
            dropRubber(world, pos);
        }

        return ActionResult.success(world.isClient);
    }

    /** The insulation level is a block of its own, named the way {@link CableType} spells it. */
    private static Block cableBlock(CableType type, int insulation) {
        Block block = Registries.BLOCK.get(Identifier.of("ic2", type.getName(insulation, null)));
        return block == Registries.BLOCK.get(Identifier.of("minecraft", "air")) ? null : block;
    }

    /** Keep the six connection booleans, so the cable does not visibly disconnect and reconnect. */
    private static BlockState copyConnections(BlockState from, BlockState to) {
        for (net.minecraft.state.property.Property<?> property : from.getProperties()) {
            if (to.contains(property)) {
                to = copyProperty(from, to, property);
            }
        }
        return to;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static BlockState copyProperty(BlockState from, BlockState to,
                                           net.minecraft.state.property.Property property) {
        return to.with(property, from.get(property));
    }

    private static boolean takeRubber(PlayerEntity player) {
        Item rubber = IC2ItemVariants.get("crafting", "rubber");
        if (rubber == null) return false;
        if (player.isCreative()) return true;

        for (int slot = 0; slot < player.getInventory().size(); slot++) {
            ItemStack stack = player.getInventory().getStack(slot);
            if (stack.isOf(rubber)) {
                stack.decrement(1);
                return true;
            }
        }
        return false;
    }

    private static void dropRubber(World world, BlockPos pos) {
        Item rubber = IC2ItemVariants.get("crafting", "rubber");
        if (rubber == null) return;

        world.spawnEntity(new ItemEntity(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                new ItemStack(rubber)));
    }

    private void damage(ItemStack tool, PlayerEntity player, int amount) {
        if (player.isCreative()) return;
        tool.damage(amount, player, net.minecraft.entity.EquipmentSlot.MAINHAND);
    }
}
