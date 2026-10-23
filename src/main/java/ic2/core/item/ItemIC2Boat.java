package ic2.core.item;

import ic2.core.entity.EntityIC2Boat;
import ic2.core.init.IC2Entities;
import java.util.List;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

/**
 * Fabric port of ItemIC2Boat - the IC2 boat items. Used on water or ground they put their boat
 * there, turned the way the player looks; the damaged rubber dinghy places nothing.
 */
public class ItemIC2Boat extends ItemIC2 {
    public ItemIC2Boat(String name) {
        this(name, new Settings());
    }

    protected ItemIC2Boat(String name, Settings settings) {
        super(name, settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        EntityIC2Boat boat = makeBoat(world);
        if (boat == null) {
            return TypedActionResult.pass(stack);
        }

        BlockHitResult hitPos = raycast(world, player, RaycastContext.FluidHandling.ANY);
        if (hitPos.getType() == HitResult.Type.MISS) {
            return TypedActionResult.pass(stack);
        }

        // nothing in the way between the player and the spot
        Vec3d look = player.getRotationVec(1.0F).multiply(5.0);
        Vec3d eyes = player.getEyePos();
        List<Entity> entities = world.getOtherEntities(player, player.getBoundingBox().stretch(look).expand(1.0), Entity::canHit);
        for (Entity entity : entities) {
            Box box = entity.getBoundingBox().expand(entity.getTargetingMargin());
            if (box.contains(eyes)) {
                return TypedActionResult.pass(stack);
            }
        }

        if (hitPos.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = hitPos.getBlockPos();
            if (world.getBlockState(pos).isOf(Blocks.SNOW)) {
                pos = pos.down();
            }

            float yaw = (float) (((MathHelper.floor(player.getYaw() * 4.0F / 360.0F + 0.5) & 3) - 1) * 90);
            boat.place(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, yaw);
            if (!world.isSpaceEmpty(boat, boat.getBoundingBox().expand(-0.1))) {
                return TypedActionResult.fail(stack);
            }

            if (!world.isClient) {
                world.spawnEntity(boat);
            }
            stack.decrementUnlessCreative(1, player);
        }

        return TypedActionResult.success(stack, world.isClient);
    }

    /** The boat this item places, or null for the damaged dinghy. */
    protected EntityIC2Boat makeBoat(World world) {
        EntityType<? extends EntityIC2Boat> type = switch (name) {
            case "boat_carbon" -> IC2Entities.CARBON_BOAT;
            case "boat_rubber" -> IC2Entities.RUBBER_BOAT;
            case "boat_electric" -> IC2Entities.ELECTRIC_BOAT;
            default -> null;
        };
        return type == null ? null : type.create(world);
    }
}
