package ic2.core.block;

import ic2.core.init.IC2Blocks;
import ic2.core.init.IC2Entities;
import ic2.core.item.ItemWrench;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

/**
 * Fabric port of {@code ic2.core.block.EntityNuke}: a lit nuke. It burns fifteen seconds, and a
 * wrench defuses it - the nuke and everything in it are gone, but nothing blows up.
 */
public class EntityNuke extends EntityIC2Explosive {
    public EntityNuke(EntityType<? extends EntityNuke> type, World world) {
        super(type, world);
        // a summoned one has no block that made it
        setRenderBlockState(IC2Blocks.NUKE_BLOCK.getDefaultState());
        dropRate = 0.05F;
        damageVsEntitys = 1.5F;
    }

    public EntityNuke(World world, double x, double y, double z, float power, int radiationRange) {
        super(IC2Entities.NUKE, world, x, y, z, 300, power, 0.05F, 1.5F, IC2Blocks.NUKE_BLOCK.getDefaultState(), radiationRange);
    }

    @Override
    public ActionResult interact(PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (!getWorld().isClient && stack.getItem() instanceof ItemWrench wrench && wrench.canTakeDamage(stack, 1)) {
            wrench.damage(stack, 1, player);
            discard();
            return ActionResult.SUCCESS;
        }
        return ActionResult.PASS;
    }
}
