package ic2.core.gametest;

import ic2.api.recipe.Recipes;
import ic2.core.block.TileEntityHeatSourceInventory;
import ic2.core.block.machine.tileentity.TileEntityBlastFurnace;
import ic2.core.block.machine.tileentity.TileEntityRecycler;
import ic2.core.init.IC2Blocks;
import ic2.core.init.IC2ItemVariants;
import ic2.core.item.ItemBooze;
import ic2.core.ref.FluidName;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraftforge.fluids.FluidStack;

/**
 * The recycler and its scrap, the scrap box, the blast furnace, and the mugs. Run with
 * {@code ./gradlew runGametest}.
 */
public class ScrapGameTest implements FabricGameTest {
    private static final BlockPos POS = new BlockPos(2, 2, 2);

    private static Item scrap() {
        return IC2ItemVariants.get("crafting", "scrap");
    }

    /** The default blacklist, as general.ini names it in 1.12.2 spelling. */
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void recyclerBlacklistComesFromTheConfig(TestContext ctx) {
        for (Item item : new Item[] {Items.GLASS_PANE, Items.STICK, Items.SNOWBALL, Items.SNOW, Items.SNOW_BLOCK,
                Registries.ITEM.get(Identifier.of("ic2", "scaffold"))}) {
            ctx.assertTrue(TileEntityRecycler.getIsItemBlacklisted(new ItemStack(item)), item + " is not blacklisted");
        }
        ctx.assertTrue(!TileEntityRecycler.getIsItemBlacklisted(new ItemStack(Items.COBBLESTONE)), "cobblestone is blacklisted");
        ctx.assertTrue(Recipes.recycler.apply(new ItemStack(Items.COBBLESTONE, 5), false).getOutput().iterator().next().isOf(scrap()),
                "cobblestone does not recycle into scrap");
        ctx.assertTrue(Recipes.recycler.apply(new ItemStack(Items.STICK), false).getOutput().isEmpty(), "a stick gives scrap");
        ctx.complete();
    }

    /**
     * 64 cobblestone take 64 * 45 ticks; one in eight gives scrap. Sticks are eaten for nothing.
     */
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void recyclerEatsEverythingAndSometimesGivesScrap(TestContext ctx) {
        ctx.setBlockState(POS, IC2Blocks.RECYCLER_BLOCK.getDefaultState());
        TileEntityRecycler recycler = ctx.getBlockEntity(POS);
        recycler.upgradeSlot.put(0, new ItemStack(IC2ItemVariants.get("upgrade", "energy_storage")));
        recycler.injectEnergy(Direction.UP, 10045, 32);

        recycler.setInput(new ItemStack(Items.STICK, 4));
        for (int i = 0; i < 4 * 45; i++) {
            recycler.tick();
        }
        ctx.assertTrue(recycler.getInput().isEmpty(), "sticks left: " + recycler.getInput());
        ctx.assertTrue(recycler.getOutput(0).isEmpty(), "sticks gave " + recycler.getOutput(0));
        ctx.assertTrue(recycler.getEnergy() == 10045 - 4 * 45, "energy " + recycler.getEnergy());

        recycler.setInput(new ItemStack(Items.COBBLESTONE, 64));
        for (int i = 0; i < 64 * 45; i++) {
            recycler.tick();
        }
        ItemStack out = recycler.getOutput(0);
        ctx.assertTrue(recycler.getInput().isEmpty(), "cobblestone left: " + recycler.getInput());
        // (7/8)^64 is two in ten thousand, eight in a row would be one in 1e57
        ctx.assertTrue(out.isOf(scrap()) && out.getCount() >= 1 && out.getCount() < 40, "scrap " + out);
        ctx.complete();
    }

    /** 44 drops, weights adding up to 51.7: a diamond is 0.1 of that. */
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void scrapBoxDropTable(TestContext ctx) {
        Map<ItemStack, Float> drops = Recipes.scrapboxDrops.getDrops();
        ctx.assertTrue(drops.size() == 44, "drops " + drops.size());
        float total = 0;
        float diamond = 0;
        for (Map.Entry<ItemStack, Float> drop : drops.entrySet()) {
            ctx.assertTrue(!drop.getKey().isEmpty(), "a drop is missing");
            total += drop.getValue();
            if (drop.getKey().isOf(Items.DIAMOND)) {
                diamond = drop.getValue();
            }
        }
        ctx.assertTrue(Math.abs(total - 1.0F) < 1e-4, "chances add up to " + total);
        ctx.assertTrue(Math.abs(diamond - 0.1F / 51.7F) < 1e-5, "diamond chance " + diamond);

        ItemStack box = new ItemStack(IC2ItemVariants.get("crafting", "scrap_box"), 3);
        ItemStack drop = Recipes.scrapboxDrops.getDrop(box, true);
        ctx.assertTrue(drop != null && !drop.isEmpty() && box.getCount() == 2, "drop " + drop + ", boxes " + box.getCount());
        ctx.assertTrue(Recipes.scrapboxDrops.getDrop(new ItemStack(Items.DIRT), false) == null, "dirt opens like a box");
        ctx.complete();
    }

    /** Right click throws the drop out; the creative player keeps the box. */
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void scrapBoxOpensOnRightClick(TestContext ctx) {
        ServerPlayerEntity player = ctx.createMockCreativeServerPlayerInWorld();
        BlockPos abs = ctx.getAbsolutePos(POS);
        player.refreshPositionAndAngles(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5, 0, 0);
        ItemStack box = new ItemStack(IC2ItemVariants.get("crafting", "scrap_box"), 5);
        player.setStackInHand(Hand.MAIN_HAND, box);
        box.getItem().use(ctx.getWorld(), player, Hand.MAIN_HAND);

        List<ItemEntity> thrown = ctx.getWorld().getEntitiesByClass(ItemEntity.class, new Box(abs).expand(3), e -> true);
        ctx.assertTrue(thrown.size() == 1, "thrown items: " + thrown.size());
        ctx.assertTrue(box.getCount() == 5, "a creative player used up a box");
        thrown.forEach(ItemEntity::discard);
        ctx.complete();
    }

    /**
     * Fed heat from the front, the furnace is hot at 50000; with air in its tank it makes steel and
     * slag from an iron ingot in 6000 ticks, a millibucket of air each. Without work it cools by
     * one a tick.
     */
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void blastFurnaceMakesSteelWithHeatAndAir(TestContext ctx) {
        ctx.setBlockState(POS, IC2Blocks.BLAST_FURNACE_BLOCK.getDefaultState());
        ctx.setBlockState(POS.west(), IC2Blocks.ELECTRIC_HEAT_BLOCK.getDefaultState());
        TileEntityBlastFurnace furnace = ctx.getBlockEntity(POS);
        TileEntityHeatSourceInventory heater = ctx.getBlockEntity(POS.west());
        furnace.setFacing(Direction.WEST);
        heater.setFacing(Direction.EAST);
        heater.setHeatBuffer(1_000_000);

        furnace.tick();
        ctx.assertTrue(furnace.heat == 0, "heated with nothing to do: " + furnace.heat);

        furnace.inputSlot.put(new ItemStack(Items.IRON_INGOT));
        furnace.tick();
        ctx.assertTrue(furnace.heat == 50100 && furnace.isHot(), "heat " + furnace.heat);
        ctx.assertTrue(furnace.getActive(), "hot furnace with iron is not active");
        ctx.assertTrue(furnace.getProgress() == 0, "progress without air " + furnace.getProgress());

        furnace.fluidTank.fillInternal(new FluidStack(FluidName.air.getInstance(), 8000), true);
        for (int i = 0; i < 6000; i++) {
            furnace.tick();
        }
        ctx.assertTrue(furnace.outputSlot.get(0).isOf(IC2ItemVariants.get("ingot", "steel")), "output " + furnace.outputSlot.get(0));
        ctx.assertTrue(furnace.outputSlot.get(1).isOf(IC2ItemVariants.get("misc_resource", "slag")), "slag " + furnace.outputSlot.get(1));
        ctx.assertTrue(furnace.inputSlot.isEmpty(), "iron left");
        ctx.assertTrue(furnace.fluidTank.getFluidAmount() == 2000, "air left " + furnace.fluidTank.getFluidAmount());

        int heat = furnace.heat;
        for (int i = 0; i < 10; i++) {
            furnace.tick();
        }
        ctx.assertTrue(furnace.heat == heat - 10 && !furnace.getActive(), "idle furnace heat " + furnace.heat + " from " + heat);
        ctx.complete();
    }

    /** Four dark coffees: Speed and Haste IV for 3900 ticks, and 200 ticks of nausea. */
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void coffeeStacksUp(TestContext ctx) {
        ServerPlayerEntity player = ctx.createMockCreativeServerPlayerInWorld();
        Item darkCoffee = IC2ItemVariants.get("mug", "dark_coffee");
        ItemStack left = ItemStack.EMPTY;
        for (int i = 0; i < 4; i++) {
            left = darkCoffee.finishUsing(new ItemStack(darkCoffee), ctx.getWorld(), player);
        }
        StatusEffectInstance speed = player.getStatusEffect(StatusEffects.SPEED);
        ctx.assertTrue(speed != null && speed.getAmplifier() == 3 && speed.getDuration() == 300 + 3 * 1200,
                "speed " + speed);
        ctx.assertTrue(player.getStatusEffect(StatusEffects.HASTE).getAmplifier() == 3, "haste");
        StatusEffectInstance nausea = player.getStatusEffect(StatusEffects.NAUSEA);
        ctx.assertTrue(nausea != null && nausea.getDuration() == 200, "nausea " + nausea);
        ctx.assertTrue(left.isOf(IC2ItemVariants.get("mug", "empty")), "left " + left);
        ctx.complete();
    }

    /** "Dark Beer": dig slowdown for 1200 ticks, the second mug doubles it and adds strength. */
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void beerIsNamedAndDrunkByItsValue(TestContext ctx) {
        ServerPlayerEntity player = ctx.createMockCreativeServerPlayerInWorld();
        Item booze = Registries.ITEM.get(Identifier.of("ic2", "booze"));
        ItemStack beer = new ItemStack(booze);
        // beer, solid ratio 3, hops 4, matured to "Beer"
        ItemBooze.setValue(beer, 1 | 3 << 7 | 4 << 10 | 2 << 13);
        ctx.assertTrue(beer.getName().getString().equals("Dark Beer"), "named " + beer.getName().getString());

        booze.finishUsing(beer.copy(), ctx.getWorld(), player);
        StatusEffectInstance fatigue = player.getStatusEffect(StatusEffects.MINING_FATIGUE);
        ctx.assertTrue(fatigue != null && fatigue.getAmplifier() == 0 && fatigue.getDuration() == 1200, "first mug " + fatigue);

        ItemStack left = booze.finishUsing(beer.copy(), ctx.getWorld(), player);
        fatigue = player.getStatusEffect(StatusEffects.MINING_FATIGUE);
        ctx.assertTrue(fatigue.getAmplifier() == 1 && fatigue.getDuration() == 2400, "second mug " + fatigue);
        StatusEffectInstance strength = player.getStatusEffect(StatusEffects.STRENGTH);
        ctx.assertTrue(strength != null && strength.getDuration() == 1200, "strength " + strength);
        ctx.assertTrue(left.isOf(IC2ItemVariants.get("mug", "empty")), "left " + left);
        ctx.complete();
    }
}
