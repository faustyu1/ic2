package ic2.core.gametest;

import ic2.api.item.ElectricItem;
import ic2.core.IC2;
import ic2.core.entity.EntityBoatCarbon;
import ic2.core.entity.EntityBoatElectric;
import ic2.core.entity.EntityBoatRubber;
import ic2.core.init.IC2Entities;
import ic2.core.init.IC2ItemVariants;
import ic2.core.init.IC2Items;
import ic2.core.util.Keyboard;
import java.util.EnumSet;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

/** The IC2 boats in a running world. Run with {@code ./gradlew runGametest}. */
public class BoatGameTest implements FabricGameTest {

    /** A carbon canoe broken by a hit drops itself, not planks and sticks. */
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void carbonBoatDropsItself(TestContext ctx) {
        ctx.setBlockState(new BlockPos(2, 0, 2), Blocks.STONE.getDefaultState());
        EntityBoatCarbon boat = ctx.spawnEntity(IC2Entities.CARBON_BOAT, new BlockPos(2, 1, 2));
        boat.damage(ctx.getWorld().getDamageSources().generic(), 100.0F);
        ctx.assertTrue(boat.isRemoved(), "boat not broken");
        List<ItemEntity> drops = drops(ctx);
        ctx.assertTrue(drops.size() == 1 && drops.get(0).getStack().isOf(IC2ItemVariants.get("boat", "carbon")),
                "drops " + drops.stream().map(ItemEntity::getStack).toList());
        ctx.complete();
    }

    /**
     * A rubber dinghy resting on the ground that lands from more than three blocks bursts into a
     * damaged dinghy. Boats only notice the ground a tick after touching it, so the landing is
     * handed to the boat directly once it has settled on the stone.
     */
    @GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 100)
    public void rubberBoatBurstsOnAFall(TestContext ctx) {
        for (int x = 0; x < 5; x++) {
            for (int z = 0; z < 5; z++) {
                ctx.setBlockState(new BlockPos(x, 0, z), Blocks.STONE.getDefaultState());
            }
        }
        EntityBoatRubber boat = ctx.spawnEntity(IC2Entities.RUBBER_BOAT, new BlockPos(2, 1, 2));
        ctx.runAtTick(20, () -> {
            boat.fallDistance = 5.0F;
            try {
                java.lang.reflect.Method fall = net.minecraft.entity.vehicle.BoatEntity.class.getDeclaredMethod("fall",
                        double.class, boolean.class, net.minecraft.block.BlockState.class, BlockPos.class);
                fall.setAccessible(true);
                fall.invoke(boat, 0.0, true, Blocks.STONE.getDefaultState(), boat.getBlockPos().down());
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
            ctx.assertTrue(boat.isRemoved(), "dinghy survived the fall");
            List<ItemEntity> drops = drops(ctx);
            ctx.assertTrue(drops.size() == 1 && drops.get(0).getStack().isOf(IC2ItemVariants.get("boat", "broken_rubber")),
                    "drops " + drops.stream().map(ItemEntity::getStack).toList());
            ctx.complete();
        });
    }

    /** The electric boat floats on lava without burning. */
    @GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 100)
    public void electricBoatFloatsOnLava(TestContext ctx) {
        for (int x = 0; x < 5; x++) {
            for (int z = 0; z < 5; z++) {
                ctx.setBlockState(new BlockPos(x, 0, z), Blocks.STONE.getDefaultState());
                boolean border = x == 0 || z == 0 || x == 4 || z == 4;
                ctx.setBlockState(new BlockPos(x, 1, z), border ? Blocks.STONE.getDefaultState() : Blocks.LAVA.getDefaultState());
            }
        }
        EntityBoatElectric boat = ctx.spawnEntity(IC2Entities.ELECTRIC_BOAT, new BlockPos(2, 2, 2));
        double surface = ctx.getAbsolutePos(new BlockPos(2, 1, 2)).getY() + 0.5;
        ctx.runAtTick(60, () -> {
            ctx.assertTrue(!boat.isRemoved() && !boat.isOnFire(), "boat burnt");
            ctx.assertTrue(boat.getY() > surface, "boat sank to " + boat.getY());
            ctx.complete();
        });
    }

    /**
     * With the forward key held and a charged batpack worn, the electric boat takes 4 EU a tick
     * and speeds up at 1.5 times a boat's rate; otherwise at a quarter of it.
     */
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void electricBoatRunsOnTheBatpack(TestContext ctx) {
        EntityBoatElectric boat = ctx.spawnEntity(IC2Entities.ELECTRIC_BOAT, new BlockPos(2, 2, 2));
        ServerPlayerEntity player = ctx.createMockCreativeServerPlayerInWorld();
        ItemStack batpack = new ItemStack(IC2Items.BATPACK);
        ElectricItem.manager.charge(batpack, 60000.0, Integer.MAX_VALUE, true, false);
        player.getInventory().armor.set(2, batpack);
        player.startRiding(boat, true);
        try {
            boat.tick();
            ctx.assertTrue(!boat.isAccelerated() && boat.getAccelerationFactor() == 0.25, "accelerated without the key");

            IC2.keyboard.processKeyUpdate(player, Keyboard.Key.toInt(EnumSet.of(Keyboard.Key.forward)));
            double before = ElectricItem.manager.getCharge(batpack);
            boat.tick();
            ctx.assertTrue(boat.isAccelerated() && boat.getAccelerationFactor() == 1.5, "not accelerated");
            ctx.assertTrue(before - ElectricItem.manager.getCharge(batpack) == 4.0,
                    "used " + (before - ElectricItem.manager.getCharge(batpack)));
        } finally {
            IC2.keyboard.processKeyUpdate(player, 0);
            player.discard();
        }
        ctx.complete();
    }

    private static List<ItemEntity> drops(TestContext ctx) {
        return ctx.getWorld().getEntitiesByClass(ItemEntity.class, new Box(ctx.getAbsolutePos(new BlockPos(2, 1, 2))).expand(3.0), e -> true);
    }
}
