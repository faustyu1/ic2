package ic2.core.gametest;

import ic2.api.crops.CropCard;
import ic2.api.crops.Crops;
import ic2.core.block.machine.tileentity.TileEntityCropHarvester;
import ic2.core.block.machine.tileentity.TileEntityCropmatron;
import ic2.core.crop.BlockCrop;
import ic2.core.crop.IC2Crops;
import ic2.core.crop.TileEntityCrop;
import ic2.core.init.IC2Blocks;
import ic2.core.init.IC2ItemVariants;
import ic2.core.init.IC2Items;
import ic2.core.item.ItemCropSeed;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.block.FarmlandBlock;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameMode;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

/**
 * The crop port checked in a running server: the clicks go through the real interaction path,
 * the machines scan a real neighbourhood. Run with {@code ./gradlew runGametest}; the game
 * only loads this class when started with {@code -Dfabric-api.gametest}.
 */
public class CropGameTest implements FabricGameTest {
    private static final BlockPos SOIL = new BlockPos(2, 1, 2);
    private static final BlockPos CROP = SOIL.up();

    private static ServerPlayerEntity player(TestContext ctx) {
        ServerPlayerEntity player = ctx.createMockCreativeServerPlayerInWorld();
        player.changeGameMode(GameMode.SURVIVAL);
        return player;
    }

    private static void use(TestContext ctx, ServerPlayerEntity player, ItemStack stack, BlockPos rel, Direction side) {
        player.setStackInHand(Hand.MAIN_HAND, stack);
        BlockPos abs = ctx.getAbsolutePos(rel);
        BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(abs).add(Vec3d.of(side.getVector()).multiply(0.5)), side, abs, false);
        player.interactionManager.interactBlock(player, ctx.getWorld(), player.getStackInHand(Hand.MAIN_HAND), Hand.MAIN_HAND, hit);
    }

    private static int itemsAround(TestContext ctx, BlockPos rel, net.minecraft.item.Item item) {
        BlockPos abs = ctx.getAbsolutePos(rel);
        List<ItemEntity> list = ctx.getWorld().getEntitiesByClass(ItemEntity.class, new Box(abs).expand(2), e -> e.getStack().isOf(item));
        return list.stream().mapToInt(e -> e.getStack().getCount()).sum();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void stickSeedGrowHarvest(TestContext ctx) {
        ctx.setBlockState(SOIL, Blocks.FARMLAND.getDefaultState());
        ServerPlayerEntity player = player(ctx);

        // the stick goes on top of the farmland
        use(ctx, player, new ItemStack(IC2Items.CROP_STICK, 3), SOIL, Direction.UP);
        ctx.expectBlock(IC2Blocks.CROP_BLOCK, CROP);
        ctx.assertTrue(player.getStackInHand(Hand.MAIN_HAND).getCount() == 2, "stick not consumed");

        // a vanilla seed is planted by the crop
        use(ctx, player, new ItemStack(Items.WHEAT_SEEDS, 5), CROP, Direction.UP);
        TileEntityCrop crop = ctx.getBlockEntity(CROP);
        ctx.assertTrue(crop.getCrop() == IC2Crops.cropWheat, "wheat not planted: " + crop.getCrop());
        ctx.assertTrue(player.getStackInHand(Hand.MAIN_HAND).getCount() == 4, "seed not consumed");

        // wheat needs light 9 to grow, which the test structure does not have; corium does not
        CropCard corium = Crops.instance.getCropCard("ic2", "corium");
        crop.reset();
        ctx.assertTrue(crop.tryPlantIn(corium, 1, 0, 0, 31, 0), "corium not planted");
        for (int i = 0; i < 20 && crop.getCurrentSize() < 4; i++) {
            crop.setGrowthPoints(30000);
            crop.performTick();
        }
        ctx.assertTrue(crop.getCurrentSize() == 4, "corium did not grow: " + crop.getCurrentSize());

        // an empty hand harvests
        int before = itemsAround(ctx, CROP, Items.LEATHER);
        for (int i = 0; i < 30 && itemsAround(ctx, CROP, Items.LEATHER) == before; i++) {
            crop.setCurrentSize(4);
            use(ctx, player, ItemStack.EMPTY, CROP, Direction.UP);
        }
        ctx.assertTrue(itemsAround(ctx, CROP, Items.LEATHER) > before, "no leather harvested");
        ctx.assertTrue(crop.getCurrentSize() == 1, "size after harvest " + crop.getCurrentSize());

        // the render data and the sync carry the crop
        ctx.assertTrue(crop.getRenderData() instanceof TileEntityCrop.CropRenderState s && s.crop() == corium && s.size() == 1, "render data");
        NbtCompound nbt = crop.createNbt(ctx.getWorld().getRegistryManager());
        TileEntityCrop copy = new TileEntityCrop(IC2Blocks.CROP_BE, crop.getPos(), crop.getCachedState());
        copy.read(nbt, ctx.getWorld().getRegistryManager());
        ctx.assertTrue(copy.getCrop() == corium && copy.getCurrentSize() == 1 && copy.getStatResistance() == 31, "nbt round trip");
        ctx.assertTrue(crop.toUpdatePacket() != null, "no update packet");

        // breaking gives one stick back
        ctx.getWorld().breakBlock(ctx.getAbsolutePos(CROP), true);
        ctx.assertTrue(itemsAround(ctx, CROP, IC2Items.CROP_STICK) == 1, "stick drops: " + itemsAround(ctx, CROP, IC2Items.CROP_STICK));
        ctx.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void crossStickTrowelSeedBagAndSoil(TestContext ctx) {
        ctx.setBlockState(SOIL, Blocks.FARMLAND.getDefaultState());
        ServerPlayerEntity player = player(ctx);
        use(ctx, player, new ItemStack(IC2Items.CROP_STICK, 4), SOIL, Direction.UP);
        TileEntityCrop crop = ctx.getBlockEntity(CROP);

        // a second stick makes a cross-crop, a left click takes it back
        use(ctx, player, new ItemStack(IC2Items.CROP_STICK, 4), CROP, Direction.UP);
        ctx.assertTrue(crop.isCrossingBase(), "no cross-crop");
        crop.leftClick(player);
        ctx.assertTrue(!crop.isCrossingBase(), "cross-crop stick not removed");

        // weed is pulled by the trowel, which leaves the stick
        crop.setCrop(Crops.weed);
        crop.setCurrentSize(4);
        use(ctx, player, new ItemStack(Registries.ITEM.get(Identifier.of("ic2", "weeding_trowel"))), CROP, Direction.UP);
        ctx.assertTrue(crop.getCrop() == null, "weed still there");
        ctx.assertTrue(itemsAround(ctx, CROP, IC2ItemVariants.get("crop_res", "weed")) == 4, "weed items: " + itemsAround(ctx, CROP, IC2ItemVariants.get("crop_res", "weed")));

        // a seed bag is planted whole
        CropCard corium = Crops.instance.getCropCard("ic2", "corium");
        ItemStack bag = ItemCropSeed.generateItemStackFromValues(corium, 5, 6, 7, 4);
        ctx.assertTrue(bag.getName().getString().contains("Corium"), "bag name " + bag.getName().getString());
        use(ctx, player, bag, CROP, Direction.UP);
        ctx.assertTrue(crop.getCrop() == corium && crop.getStatGrowth() == 5 && crop.getStatGain() == 6 && crop.getStatResistance() == 7, "bag not planted");
        ctx.assertTrue(player.getStackInHand(Hand.MAIN_HAND).isEmpty(), "bag not used up");

        // fertilizer and water are stored
        use(ctx, player, new ItemStack(TileEntityCrop.fertilizer(), 2), CROP, Direction.UP);
        ctx.assertTrue(crop.getStorageNutrients() == 100, "nutrients " + crop.getStorageNutrients());
        use(ctx, player, new ItemStack(Items.WATER_BUCKET), CROP, Direction.UP);
        ctx.assertTrue(crop.getStorageWater() == 200, "water " + crop.getStorageWater());
        ctx.assertTrue(player.getStackInHand(Hand.MAIN_HAND).isOf(Items.BUCKET), "bucket not emptied");

        // pick block gives the bag back
        ItemStack picked = crop.getPickStack();
        ctx.assertTrue(picked.getItem() instanceof ItemCropSeed seed && seed.getCropFromStack(picked) == corium, "pick stack");

        // the light state follows the plant: redwheat at 7 glows
        crop.setCrop(IC2Crops.cropRedwheat);
        crop.setCurrentSize(7);
        crop.dirty = true;
        crop.tick();
        ctx.assertTrue(ctx.getBlockState(CROP).get(BlockCrop.LIGHT) == 7, "light " + ctx.getBlockState(CROP).get(BlockCrop.LIGHT));

        // without farmland the stick is gone
        ctx.setBlockState(SOIL, Blocks.DIRT.getDefaultState());
        ctx.expectBlock(Blocks.AIR, CROP);
        ctx.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void weedSpreadsAndEmptySticksGrowWeed(TestContext ctx) {
        BlockPos soilA = new BlockPos(1, 1, 1);
        BlockPos soilB = new BlockPos(2, 1, 1);
        ctx.setBlockState(soilA, Blocks.FARMLAND.getDefaultState());
        ctx.setBlockState(soilB, Blocks.FARMLAND.getDefaultState());
        ctx.setBlockState(soilA.up(), IC2Blocks.CROP_BLOCK.getDefaultState());
        ctx.setBlockState(soilB.up(), IC2Blocks.CROP_BLOCK.getDefaultState());
        TileEntityCrop weed = ctx.getBlockEntity(soilA.up());
        TileEntityCrop other = ctx.getBlockEntity(soilB.up());

        // an empty stick turns into weed now and then (1 in 100 a growth tick)
        for (int i = 0; i < 2000 && other.getCrop() == null; i++) {
            other.performTick();
        }
        ctx.assertTrue(other.getCrop() == Crops.weed, "empty stick never grew weed");
        other.reset();

        weed.setCrop(Crops.weed);
        weed.setCurrentSize(4);
        for (int i = 0; i < 2000 && other.getCrop() == null; i++) {
            weed.performWeedWork();
        }
        ctx.assertTrue(other.getCrop() == Crops.weed, "weed did not spread");
        ctx.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void cropmatronAndHarvester(TestContext ctx) {
        ctx.setBlockState(SOIL, Blocks.FARMLAND.getDefaultState());
        ctx.setBlockState(CROP, IC2Blocks.CROP_BLOCK.getDefaultState());
        BlockPos dry = new BlockPos(3, 1, 2);
        ctx.setBlockState(dry, Blocks.FARMLAND.getDefaultState());
        TileEntityCrop crop = ctx.getBlockEntity(CROP);
        crop.tryPlantIn(IC2Crops.cropWheat, 1, 1, 1, 1, 0);

        BlockPos matronPos = new BlockPos(0, 2, 2);
        ctx.setBlockState(matronPos, IC2Blocks.CROPMATRON_BLOCK.getDefaultState());
        TileEntityCropmatron matron = ctx.getBlockEntity(matronPos);
        ctx.assertTrue(matron.hasGui(), "cropmatron has no gui");
        matron.injectEnergy(Direction.UP, 10000, 32);
        matron.getWaterTank().fillInternal(new FluidStack(FluidRegistry.getFluid("water"), 2000), true);
        matron.fertilizerSlot.put(new ItemStack(TileEntityCrop.fertilizer(), 3));
        for (int i = 0; i < 9 * 9 * 3; i++) {
            matron.scan();
        }
        ctx.assertTrue(crop.getStorageWater() == 200, "matron water " + crop.getStorageWater());
        ctx.assertTrue(crop.getStorageNutrients() == 90, "matron nutrients " + crop.getStorageNutrients());
        ctx.assertTrue(matron.fertilizerSlot.get().getCount() == 2, "fertilizer used " + matron.fertilizerSlot.get().getCount());
        ctx.assertTrue(ctx.getBlockState(dry).get(FarmlandBlock.MOISTURE) == 7, "farmland not watered");

        BlockPos harvesterPos = new BlockPos(4, 2, 2);
        ctx.setBlockState(harvesterPos, IC2Blocks.CROP_HARVESTER_BLOCK.getDefaultState());
        TileEntityCropHarvester harvester = ctx.getBlockEntity(harvesterPos);
        ctx.assertTrue(harvester.hasGui(), "harvester has no gui");
        harvester.injectEnergy(Direction.UP, 10000, 32);
        crop.setCurrentSize(7);
        for (int round = 0; round < 20 && harvester.contentSlot.isEmpty(); round++) {
            crop.setCurrentSize(7);
            for (int i = 0; i < 9 * 9 * 3; i++) {
                harvester.scan();
            }
        }
        ctx.assertTrue(!harvester.contentSlot.isEmpty() && harvester.contentSlot.get(0).isOf(Items.WHEAT), "nothing harvested");
        ctx.complete();
    }
}
