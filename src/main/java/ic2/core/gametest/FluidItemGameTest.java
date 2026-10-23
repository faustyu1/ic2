package ic2.core.gametest;

import ic2.api.recipe.Recipes;
import ic2.core.IC2;
import ic2.core.block.heatgenerator.tileentity.TileEntityElectricHeatGenerator;
import ic2.core.block.heatgenerator.tileentity.TileEntityFluidHeatGenerator;
import ic2.core.block.heatgenerator.tileentity.TileEntityRTHeatGenerator;
import ic2.core.block.heatgenerator.tileentity.TileEntitySolidHeatGenerator;
import ic2.core.block.machine.tileentity.TileEntityCanner;
import ic2.core.block.machine.tileentity.TileEntityFermenter;
import ic2.core.block.storage.tank.TileEntityTank;
import ic2.core.init.IC2Blocks;
import ic2.core.init.IC2ClassicBlocks;
import ic2.core.init.IC2ItemVariants;
import ic2.core.init.IC2Items;
import ic2.core.item.IItemFluidTank;
import ic2.core.item.armor.ItemArmorJetpack;
import ic2.core.item.tool.ItemSprayer;
import ic2.core.ref.FluidName;
import ic2.core.util.Keyboard;
import java.util.EnumSet;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

/**
 * Fluid in items and the machines around it: the fuel jetpack and the CF pack, the canning machine
 * in its four modes, the fermenter, the four heat generators and the foam sprayer.
 * Run with {@code ./gradlew runGametest}.
 */
public class FluidItemGameTest implements FabricGameTest {
    private static final BlockPos A = new BlockPos(2, 2, 2);
    private static final BlockPos B = A.east();

    @SuppressWarnings("removal")
    private static ServerPlayerEntity player(TestContext ctx) {
        ServerPlayerEntity player = ctx.createMockCreativeServerPlayerInWorld();
        player.changeGameMode(GameMode.SURVIVAL);
        player.refreshPositionAndAngles(ctx.getAbsolute(new Vec3d(2.5, 2.0, 0.5)), 0.0F, 0.0F);
        return player;
    }

    private static FluidStack fluid(FluidName name, int amount) {
        return new FluidStack(name.getInstance(), amount);
    }

    private static FluidStack water(int amount) {
        return new FluidStack(FluidRegistry.getFluid("water"), amount);
    }

    private static Item item(String name) {
        return Registries.ITEM.get(Identifier.of("ic2", name));
    }

    private static void runTicks(net.minecraft.block.entity.BlockEntity be, int ticks) {
        for (int i = 0; i < ticks; i++) {
            ((ic2.core.block.TileEntityBlock) be).tick();
        }
    }

    // ---------------------------------------------------------------- the item tank

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void jetpackTakesBiogasFromATankAndFliesOnIt(TestContext ctx) {
        ServerPlayerEntity player = player(ctx);
        ctx.setBlockState(A, IC2Blocks.STEEL_TANK_BLOCK);
        TileEntityTank tank = (TileEntityTank) ctx.getBlockEntity(A);
        tank.getTank().setFluid(fluid(FluidName.biogas, 50000));

        ItemStack jetpack = new ItemStack(IC2Items.JETPACK);
        ItemArmorJetpack item = (ItemArmorJetpack) IC2Items.JETPACK;
        ctx.assertTrue(item.getChargeLevel(jetpack) == 0.0, "new jetpack not empty");
        player.setStackInHand(Hand.MAIN_HAND, jetpack);
        ctx.assertTrue(tank.onActivated(player, Hand.MAIN_HAND), "tank did not fill the jetpack");
        ctx.assertTrue(item.getAmount(jetpack) == 30000, "jetpack holds " + item.getAmount(jetpack));
        ctx.assertTrue(tank.getTank().getFluidAmount() == 20000, "tank gave " + (50000 - tank.getTank().getFluidAmount()));
        ctx.assertTrue(item.getItemBarStep(jetpack) == 13, "full jetpack bar not full");

        // a jetpack only takes biogas
        ItemStack empty = new ItemStack(IC2Items.JETPACK);
        ctx.assertTrue(item.fill(empty, water(1000), true) == 0, "jetpack took water");

        // two climbing ticks cost 4 mB
        ctx.assertTrue(item.drainEnergy(jetpack, 2) && item.drainEnergy(jetpack, 2), "jetpack did not burn");
        ctx.assertTrue(item.getAmount(jetpack) == 29996, "climbing cost " + (30000 - item.getAmount(jetpack)));
        ctx.assertFalse(item.drainEnergy(empty, 1), "an empty jetpack flew");

        // the CF pack goes back into a tank of foam
        ItemStack pack = new ItemStack(IC2Items.CF_PACK);
        IItemFluidTank packTank = (IItemFluidTank) IC2Items.CF_PACK;
        packTank.setFluid(pack, fluid(FluidName.construction_foam, 1000));
        tank.getTank().setFluid(null);
        player.setStackInHand(Hand.MAIN_HAND, pack);
        ctx.assertTrue(tank.onActivated(player, Hand.MAIN_HAND), "pack did not pour into the tank");
        ctx.assertTrue(packTank.isEmpty(pack) && tank.getTank().getFluidAmount() == 1000, "pack not emptied into the tank");
        ctx.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void machineSlotsDrainAPartFullItemAndPassItOn(TestContext ctx) {
        ctx.setBlockState(A, IC2Blocks.FLUID_HEAT_BLOCK);
        TileEntityFluidHeatGenerator firebox = (TileEntityFluidHeatGenerator) ctx.getBlockEntity(A);
        ItemArmorJetpack item = (ItemArmorJetpack) IC2Items.JETPACK;

        // a full jetpack fills the 10 000 mB tank and keeps the rest
        ItemStack full = item.filledCopy(new ItemStack(IC2Items.JETPACK), FluidName.biogas.getInstance());
        ctx.assertTrue(firebox.fluidSlot.accepts(full), "the firebox slot refused a jetpack of biogas");
        firebox.fluidSlot.put(full);
        runTicks(firebox, 1);
        ctx.assertTrue(firebox.getTankAmount() == 10000, "firebox tank holds " + firebox.getTankAmount());
        ctx.assertTrue(item.getAmount(firebox.fluidSlot.get()) == 20000, "jetpack kept " + item.getAmount(firebox.fluidSlot.get()));

        // once it is dry it goes to the output slot
        firebox.getFluidTank().setFluid(null);
        ItemStack small = new ItemStack(IC2Items.JETPACK);
        item.setFluid(small, fluid(FluidName.biogas, 500));
        firebox.fluidSlot.put(small);
        runTicks(firebox, 1);
        ctx.assertTrue(firebox.fluidSlot.isEmpty(), "dry jetpack stayed in the input slot");
        ctx.assertTrue(firebox.outputSlot.get().isOf(IC2Items.JETPACK), "dry jetpack not in the output slot");
        ctx.complete();
    }

    // ---------------------------------------------------------------- canner

    private static TileEntityCanner canner(TestContext ctx, TileEntityCanner.Mode mode) {
        ctx.setBlockState(A, IC2Blocks.CANNER_BLOCK);
        TileEntityCanner canner = (TileEntityCanner) ctx.getBlockEntity(A);
        canner.setMode(mode);
        canner.injectEnergy(Direction.UP, 800, 32);
        return canner;
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void cannerPutsFoodIntoTinCans(TestContext ctx) {
        TileEntityCanner canner = canner(ctx, TileEntityCanner.Mode.BottleSolid);
        canner.canInputSlot.put(new ItemStack(item("crafting_tin_can"), 10));
        ctx.assertTrue(canner.inputSlot.accepts(new ItemStack(Items.COOKED_BEEF)), "input refused cooked beef");
        canner.inputSlot.put(new ItemStack(Items.COOKED_BEEF, 2));

        runTicks(canner, TileEntityCanner.OPERATION_LENGTH - 1);
        ctx.assertTrue(canner.outputSlot.isEmpty(), "canned before the time was up");
        runTicks(canner, 1);
        ctx.assertTrue(canner.outputSlot.get().isOf(item("filled_tin_can"))
                && canner.outputSlot.get().getCount() == 8, "cooked beef did not make 8 cans: " + canner.outputSlot.get());
        ctx.assertTrue(canner.canInputSlot.get().getCount() == 2 && canner.inputSlot.get().getCount() == 1, "wrong amounts used");
        ctx.assertTrue(canner.getEnergy() == 0, "energy left: " + canner.getEnergy());
        ctx.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void cannerEnrichesWaterIntoFoamStraightIntoAPack(TestContext ctx) {
        TileEntityCanner canner = canner(ctx, TileEntityCanner.Mode.EnrichLiquid);
        canner.inputTank.setFluid(water(3000));
        canner.inputSlot.put(new ItemStack(item("crafting_cf_powder"), 3));
        ItemStack pack = new ItemStack(IC2Items.CF_PACK);
        canner.canInputSlot.put(pack);

        runTicks(canner, TileEntityCanner.OPERATION_LENGTH);
        IItemFluidTank packTank = (IItemFluidTank) IC2Items.CF_PACK;
        ctx.assertTrue(packTank.getAmount(canner.canInputSlot.get()) == 1000, "pack holds " + packTank.getAmount(canner.canInputSlot.get()));
        ctx.assertTrue(canner.outputTank.getFluidAmount() == 0, "foam went past the pack into the tank");
        ctx.assertTrue(canner.inputTank.getFluidAmount() == 2000 && canner.inputSlot.get().getCount() == 2, "inputs not used");

        // without a container the result goes to the output tank; bio chaff makes biomass
        canner.canInputSlot.clear();
        canner.inputSlot.put(new ItemStack(item("crafting_bio_chaff")));
        canner.injectEnergy(Direction.UP, 800, 32);
        runTicks(canner, TileEntityCanner.OPERATION_LENGTH);
        FluidStack out = canner.outputTank.getFluid();
        ctx.assertTrue(out != null && out.getFluid().equals(FluidName.biomass.getInstance()) && out.amount == 1000,
                "no biomass in the output tank: " + out);
        ctx.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void cannerFillsAndEmptiesContainersAndSwapsTanks(TestContext ctx) {
        ServerPlayerEntity player = player(ctx);
        TileEntityCanner canner = canner(ctx, TileEntityCanner.Mode.EmptyLiquid);
        canner.canInputSlot.put(new ItemStack(Items.LAVA_BUCKET));
        runTicks(canner, TileEntityCanner.OPERATION_LENGTH);
        ctx.assertTrue(canner.outputSlot.get().isOf(Items.BUCKET), "bucket not handed back");
        ctx.assertTrue(canner.outputTank.getFluidAmount() == 1000, "lava not in the output tank");

        // the swap button moves the lava to the input side, and the canner bottles it into a cell
        canner.onGuiEvent(player, "swapTanks");
        ctx.assertTrue(canner.inputTank.getFluidAmount() == 1000 && canner.outputTank.getFluidAmount() == 0, "tanks not swapped");
        canner.outputSlot.clear();
        // the mode button goes round the four modes; from EmptyLiquid the next is BottleLiquid
        canner.onGuiEvent(player, "mode");
        ctx.assertTrue(canner.getMode() == TileEntityCanner.Mode.BottleLiquid, "mode is " + canner.getMode());
        canner.canInputSlot.put(new ItemStack(IC2ItemVariants.get("fluid_cell", null)));
        canner.injectEnergy(Direction.UP, 800, 32);
        runTicks(canner, TileEntityCanner.OPERATION_LENGTH);
        ctx.assertTrue(canner.outputSlot.get().isOf(IC2ItemVariants.get("fluid_cell", "lava")), "no lava cell: " + canner.outputSlot.get());
        ctx.assertTrue(canner.inputTank.getFluidAmount() == 0, "lava left in the tank");

        // a foam sprayer is topped up with what the tank has
        canner.outputSlot.clear();
        canner.inputTank.setFluid(fluid(FluidName.construction_foam, 3000));
        canner.canInputSlot.put(new ItemStack(item("foam_sprayer")));
        canner.injectEnergy(Direction.UP, 800, 32);
        runTicks(canner, TileEntityCanner.OPERATION_LENGTH);
        ItemSprayer sprayer = (ItemSprayer) item("foam_sprayer");
        ctx.assertTrue(sprayer.getAmount(canner.outputSlot.get()) == 3000, "sprayer not filled: " + canner.outputSlot.get());
        ctx.complete();
    }

    // ---------------------------------------------------------------- heat and the fermenter

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void fermenterTurnsBiomassIntoBiogasWithHeat(TestContext ctx) {
        ctx.setBlockState(A, IC2Blocks.ELECTRIC_HEAT_BLOCK);
        ctx.setBlockState(B, IC2Blocks.FERMENTER_BLOCK);
        TileEntityElectricHeatGenerator heater = (TileEntityElectricHeatGenerator) ctx.getBlockEntity(A);
        TileEntityFermenter fermenter = (TileEntityFermenter) ctx.getBlockEntity(B);
        heater.setFacing(Direction.EAST);
        fermenter.setFacing(Direction.WEST);
        for (int i = 0; i < 10; i++) {
            heater.coilSlot.put(i, new ItemStack(IC2ItemVariants.get("crafting", "coil")));
        }
        ctx.assertTrue(heater.getMaxHeatEmittedPerTick() == 100, "ten coils give " + heater.getMaxHeatEmittedPerTick());
        heater.injectEnergy(Direction.UP, 10000, 128);
        fermenter.getInputTank().setFluid(fluid(FluidName.biomass, 1000));

        // 4000 HU at 100 HU a tick is one run every 40 ticks
        for (int i = 0; i < 41; i++) {
            heater.tick();
            fermenter.tick();
        }
        ctx.assertTrue(fermenter.getOutputTank().getFluidAmount() == 400, "biogas: " + fermenter.getOutputTank().getFluidAmount());
        ctx.assertTrue(fermenter.getInputTank().getFluidAmount() == 980, "biomass: " + fermenter.getInputTank().getFluidAmount());
        ctx.assertTrue(heater.getEnergy() < 10000 - 3900, "heat cost no EU: " + heater.getEnergy());

        // the biogas side fills a jetpack
        fermenter.fluidOutputCellInSlot.put(new ItemStack(IC2Items.JETPACK));
        fermenter.tick();
        ItemArmorJetpack jetpack = (ItemArmorJetpack) IC2Items.JETPACK;
        ctx.assertTrue(jetpack.getAmount(fermenter.fluidOutputCellInSlot.get()) == 400, "jetpack not fed from the fermenter");

        // and every 500 mB of biomass used gives a fertilizer
        fermenter.progress = TileEntityFermenter.biomassPerFertilizer();
        fermenter.tick();
        ctx.assertTrue(!fermenter.fertiliserSlot.isEmpty(), "no fertilizer");
        ctx.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void heatGeneratorsMakeHeatTheirWay(TestContext ctx) {
        // the solid firebox burns coal at a quarter of the furnace time, 20 HU a tick
        ctx.setBlockState(A, IC2Blocks.SOLID_HEAT_BLOCK);
        TileEntitySolidHeatGenerator solid = (TileEntitySolidHeatGenerator) ctx.getBlockEntity(A);
        solid.fuelSlot.put(new ItemStack(Items.COAL));
        runTicks(solid, 3);
        ctx.assertTrue(solid.fuel == 400 - 3 && solid.fuelSlot.isEmpty(), "coal not burnt: " + solid.fuel);
        ctx.assertTrue(solid.getHeatBuffer() == 20, "solid buffer " + solid.getHeatBuffer());
        ctx.assertTrue(solid.drawHeat(solid.getFacing(), 100, false) == 20, "solid gave the wrong heat");
        ctx.assertTrue(solid.drawHeat(solid.getFacing().getOpposite(), 100, false) == 0, "heat came out of the back");

        // the radioisotope generator doubles with every pellet: 3 pellets, 8 HU
        ctx.setBlockState(B, IC2Blocks.RT_HEAT_BLOCK);
        TileEntityRTHeatGenerator rt = (TileEntityRTHeatGenerator) ctx.getBlockEntity(B);
        for (int i = 0; i < 3; i++) {
            rt.fuelSlot.put(i, new ItemStack(IC2ItemVariants.get("nuclear", "rtg_pellet")));
        }
        runTicks(rt, 1);
        ctx.assertTrue(rt.getMaxHeatEmittedPerTick() == 8 && rt.getHeatBuffer() == 8, "rt gives " + rt.getMaxHeatEmittedPerTick());

        // the liquid firebox burns biogas, 32 HU a tick, 10 mB a second
        BlockPos c = A.north();
        ctx.setBlockState(c, IC2Blocks.FLUID_HEAT_BLOCK);
        TileEntityFluidHeatGenerator firebox = (TileEntityFluidHeatGenerator) ctx.getBlockEntity(c);
        ctx.assertTrue(Recipes.fluidHeatGenerator.acceptsFluid(FluidName.biogas.getInstance()), "biogas is no fuel");
        firebox.getFluidTank().setFluid(fluid(FluidName.biogas, 1000));
        // it only burns while something takes the heat away
        for (int i = 0; i < 21; i++) {
            firebox.tick();
            firebox.drawHeat(firebox.getFacing(), 100, false);
        }
        ctx.assertTrue(firebox.getMaxHeatEmittedPerTick() == 32, "firebox gives " + firebox.getMaxHeatEmittedPerTick());
        ctx.assertTrue(firebox.getTankAmount() == 990, "firebox burnt " + (1000 - firebox.getTankAmount()));

        // an electric heater without coils gives nothing
        BlockPos d = B.north();
        ctx.setBlockState(d, IC2Blocks.ELECTRIC_HEAT_BLOCK);
        TileEntityElectricHeatGenerator heater = (TileEntityElectricHeatGenerator) ctx.getBlockEntity(d);
        heater.injectEnergy(Direction.UP, 1000, 128);
        runTicks(heater, 5);
        ctx.assertTrue(heater.getHeatBuffer() == 0 && heater.getEnergy() == 1000, "a heater without coils made heat");
        ctx.complete();
    }

    // ---------------------------------------------------------------- foam sprayer

    private static void spray(TestContext ctx, ServerPlayerEntity player, ItemStack sprayer, BlockPos rel, Direction side) {
        player.setStackInHand(Hand.MAIN_HAND, sprayer);
        BlockPos abs = ctx.getAbsolutePos(rel);
        BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(abs).add(Vec3d.of(side.getVector()).multiply(0.5)), side, abs, false);
        sprayer.useOnBlock(new ItemUsageContext(player, Hand.MAIN_HAND, hit));
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void sprayerFoamsTenBlocksDrawingFromThePackFirst(TestContext ctx) {
        ServerPlayerEntity player = player(ctx);
        // looking down, so the foam spreads sideways and down but never back up
        player.refreshPositionAndAngles(ctx.getAbsolute(new Vec3d(4.5, 5.0, 4.5)), 0.0F, 90.0F);
        for (int x = 1; x <= 7; x++) {
            for (int z = 1; z <= 7; z++) {
                ctx.setBlockState(new BlockPos(x, 1, z), Blocks.STONE);
            }
        }

        ItemSprayer item = (ItemSprayer) item("foam_sprayer");
        ItemStack sprayer = item.filledCopy(new ItemStack(item), FluidName.construction_foam.getInstance());
        ItemStack pack = new ItemStack(IC2Items.CF_PACK);
        IItemFluidTank packTank = (IItemFluidTank) IC2Items.CF_PACK;
        packTank.setFluid(pack, fluid(FluidName.construction_foam, 500));
        player.equipStack(EquipmentSlot.CHEST, pack);

        spray(ctx, player, sprayer, new BlockPos(4, 1, 4), Direction.UP);
        int foam = 0;
        for (BlockPos pos : BlockPos.iterate(new BlockPos(0, 0, 0), new BlockPos(8, 4, 8))) {
            if (ctx.getBlockState(pos).isOf(IC2ClassicBlocks.FOAM)) foam++;
        }
        ctx.assertTrue(foam == 10, "foamed " + foam + " blocks");
        ctx.expectBlock(IC2ClassicBlocks.FOAM, new BlockPos(4, 2, 4));
        // 1000 mB: the 500 of the pack first, the rest from the sprayer
        ctx.assertTrue(packTank.isEmpty(player.getEquippedStack(EquipmentSlot.CHEST)), "pack not drawn first");
        ctx.assertTrue(item.getAmount(sprayer) == 8000 - 500, "sprayer holds " + item.getAmount(sprayer));

        // single mode: the mode key and right click, then one block
        IC2.keyboard.processKeyUpdate(player, Keyboard.Key.toInt(EnumSet.of(Keyboard.Key.modeSwitch)));
        sprayer.use(ctx.getWorld(), player, Hand.MAIN_HAND);
        IC2.keyboard.processKeyUpdate(player, 0);
        spray(ctx, player, sprayer, new BlockPos(1, 1, 1), Direction.UP);
        ctx.assertTrue(item.getAmount(sprayer) == 7500 - 100, "single mode used " + (7500 - item.getAmount(sprayer)));

        // a scaffold is filled in, dropping the scaffold
        ctx.setBlockState(new BlockPos(7, 2, 7), IC2ClassicBlocks.SCAFFOLD);
        spray(ctx, player, sprayer, new BlockPos(7, 2, 7), Direction.UP);
        ctx.expectBlock(IC2ClassicBlocks.FOAM, new BlockPos(7, 2, 7));

        // an empty sprayer does nothing
        ItemStack empty = new ItemStack(item);
        player.equipStack(EquipmentSlot.CHEST, ItemStack.EMPTY);
        spray(ctx, player, empty, new BlockPos(6, 1, 1), Direction.UP);
        ctx.expectBlock(Blocks.AIR, new BlockPos(6, 2, 1));
        ctx.complete();
    }

    // ---------------------------------------------------------------- windows

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void everyNewMachineOpensItsWindow(TestContext ctx) {
        ServerPlayerEntity player = player(ctx);
        net.minecraft.block.Block[] blocks = {IC2Blocks.CANNER_BLOCK, IC2Blocks.FERMENTER_BLOCK, IC2Blocks.SOLID_HEAT_BLOCK,
                IC2Blocks.FLUID_HEAT_BLOCK, IC2Blocks.ELECTRIC_HEAT_BLOCK, IC2Blocks.RT_HEAT_BLOCK};
        int[] machineSlots = {8, 7, 2, 2, 11, 6};
        for (int i = 0; i < blocks.length; i++) {
            BlockPos pos = new BlockPos(1 + i, 2, 2);
            ctx.setBlockState(pos, blocks[i]);
            ic2.core.block.TileEntityInventory tile = (ic2.core.block.TileEntityInventory) ctx.getBlockEntity(pos);
            ctx.assertTrue(tile.hasGui(), blocks[i] + " has no window (" + tile.getGuiName() + ")");
            net.minecraft.screen.ScreenHandler handler = tile.createMenu(0, player.getInventory(), player);
            int slots = handler.slots.size() - 36;
            ctx.assertTrue(slots == machineSlots[i], blocks[i] + " window has " + slots + " machine slots");
        }
        ctx.complete();
    }
}
