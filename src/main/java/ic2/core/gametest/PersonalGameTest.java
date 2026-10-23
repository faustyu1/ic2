package ic2.core.gametest;

import ic2.core.block.personal.TileEntityEnergyOMat;
import ic2.core.block.personal.TileEntityPersonalChest;
import ic2.core.block.personal.TileEntityTradeOMat;
import ic2.core.block.personal.TileEntityTradingTerminal;
import ic2.core.init.IC2Blocks;
import ic2.core.init.IC2ItemVariants;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.GameMode;

/** The trade-o-mat, the energy-o-mat and the trading terminal. Run with {@code ./gradlew runGametest}. */
public class PersonalGameTest implements FabricGameTest {
    private static final BlockPos POS = new BlockPos(2, 2, 2);

    /**
     * One emerald buys two diamonds out of the chest next to it: the emerald goes into the chest,
     * the diamonds into the output, and the stock counts the trades the chest can still serve.
     */
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void tradeOMatTradesFromItsNeighbour(TestContext ctx) {
        ctx.setBlockState(POS, IC2Blocks.TRADE_O_MAT_BLOCK.getDefaultState());
        ctx.setBlockState(POS.east(), Blocks.CHEST.getDefaultState());
        TileEntityTradeOMat trader = ctx.getBlockEntity(POS);
        ctx.<net.minecraft.block.entity.ChestBlockEntity>getBlockEntity(POS.east()).setStack(0, new ItemStack(Items.DIAMOND, 5));
        trader.demandSlot.put(new ItemStack(Items.EMERALD));
        trader.offerSlot.put(new ItemStack(Items.DIAMOND, 2));
        trader.updateStock();
        ctx.assertTrue(trader.stock == 2, "stock " + trader.stock);

        ctx.assertTrue(!trader.inputSlot.accepts(new ItemStack(Items.GOLD_INGOT)), "takes what it does not want");
        trader.inputSlot.put(new ItemStack(Items.EMERALD, 3));
        trader.tick();
        ctx.assertTrue(trader.outputSlot.get().isOf(Items.DIAMOND) && trader.outputSlot.get().getCount() == 2, "output " + trader.outputSlot.get());
        ctx.assertTrue(trader.inputSlot.get().getCount() == 2, "input " + trader.inputSlot.get());
        ctx.assertTrue(LogisticsGameTest.count(ctx, POS.east(), Items.EMERALD) == 1, "payment not stored");
        ctx.assertTrue(LogisticsGameTest.count(ctx, POS.east(), Items.DIAMOND) == 3, "diamonds " + LogisticsGameTest.count(ctx, POS.east(), Items.DIAMOND));
        ctx.assertTrue(trader.totalTradeCount == 1 && trader.stock == 1, "trades " + trader.totalTradeCount + " stock " + trader.stock);

        // the next emerald buys the next two diamonds
        trader.tick();
        ctx.assertTrue(trader.outputSlot.get().getCount() == 4 && trader.totalTradeCount == 2, "second trade " + trader.outputSlot.get());
        // one diamond left in the chest is not enough for another
        trader.tick();
        ctx.assertTrue(trader.totalTradeCount == 2 && trader.inputSlot.get().getCount() == 1, "traded without stock");

        // unlimited stock trades from nothing
        trader.infinite = true;
        trader.tick();
        ctx.assertTrue(trader.totalTradeCount == 3 && trader.stock == -1 && trader.inputSlot.isEmpty(), "infinite " + trader.stock);
        ctx.complete();
    }

    /** The placer owns it; anyone may open it, but only the owner gets the configuration window. */
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void tradeOMatBelongsToItsPlacer(TestContext ctx) {
        ctx.setBlockState(POS, IC2Blocks.TRADE_O_MAT_BLOCK.getDefaultState());
        TileEntityTradeOMat trader = ctx.getBlockEntity(POS);
        PlayerEntity owner = ctx.createMockPlayer(GameMode.SURVIVAL);
        PlayerEntity customer = ctx.createMockPlayer(GameMode.SURVIVAL);
        trader.onPlaced(new ItemStack(IC2Blocks.TRADE_O_MAT_BLOCK), owner);
        ctx.assertTrue(owner.getUuid().equals(trader.getOwnerId()), "not owned by the placer");
        ctx.assertTrue(trader.permitsAccess(customer), "customer cannot open it");
        ctx.assertTrue(owner.getUuid().equals(trader.getOwnerId()), "customer took it over");
        ctx.assertTrue(trader.getGuiName(owner).startsWith("trade_o_mat") && !trader.getGuiName(owner).endsWith("closed"), "owner window " + trader.getGuiName(owner));
        if (!customer.hasPermissionLevel(2)) {
            ctx.assertTrue(trader.getGuiName(customer).equals("trade_o_mat_closed"), "customer window " + trader.getGuiName(customer));
            ctx.assertTrue(!trader.wrenchCanRemove(customer), "customer wrenches it");
        }
        ctx.assertTrue(trader.wrenchCanRemove(owner), "owner cannot wrench it");

        // a remote interface upgrade makes it wireless; the terminal finds it within 512 blocks
        owner.setStackInHand(Hand.MAIN_HAND, new ItemStack(IC2ItemVariants.get("upgrade", "remote_interface")));
        ctx.assertTrue(trader.onActivated(owner, Hand.MAIN_HAND), "upgrade not used");
        ctx.assertTrue(trader.isWireless() && owner.getStackInHand(Hand.MAIN_HAND).isEmpty(), "not wireless");
        ctx.assertTrue(trader.getSelfDrops(true).size() == 2, "upgrade not in the drops");

        ctx.setBlockState(POS.east(2), IC2Blocks.TRADING_TERMINAL_BLOCK.getDefaultState());
        TileEntityTradingTerminal terminal = ctx.getBlockEntity(POS.east(2));
        ctx.assertTrue(terminal.getRange() == 512, "range " + terminal.getRange());
        ctx.assertTrue(terminal.countTraders() >= 1, "terminal does not see the trade-o-mat");
        terminal.rangeUpgrade.put(new ItemStack(IC2ItemVariants.get("upgrade", "remote_interface")));
        ctx.assertTrue(terminal.getRange() == 1024, "range with an upgrade " + terminal.getRange());
        trader.setWireless(false);
        ctx.complete();
    }

    /** A trade-o-mat stocks from its owner's safe next to it, which is closed to everybody else. */
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void tradeOMatStocksFromTheOwnersSafe(TestContext ctx) {
        PlayerEntity owner = ctx.createMockPlayer(GameMode.SURVIVAL);
        ctx.setBlockState(POS, IC2Blocks.TRADE_O_MAT_BLOCK.getDefaultState());
        ctx.setBlockState(POS.west(), IC2Blocks.PERSONAL_CHEST_BLOCK.getDefaultState());
        TileEntityTradeOMat trader = ctx.getBlockEntity(POS);
        TileEntityPersonalChest safe = ctx.getBlockEntity(POS.west());
        safe.contentSlot.put(0, new ItemStack(Items.GOLD_INGOT, 9));
        trader.offerSlot.put(new ItemStack(Items.GOLD_INGOT, 3));

        trader.updateStock();
        ctx.assertTrue(trader.stock == 0, "a stranger's safe counted " + trader.stock);
        trader.onPlaced(new ItemStack(IC2Blocks.TRADE_O_MAT_BLOCK), owner);
        safe.permitsAccess(owner);
        trader.updateStock();
        ctx.assertTrue(trader.stock == 3, "stock from the safe " + trader.stock);
        ctx.complete();
    }

    /**
     * A diamond pays for 1000 EU: it goes into the chest next door, then the energy-o-mat takes
     * 1000 EU and no more from the net on any side but its face.
     */
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void energyOMatSellsEnergy(TestContext ctx) {
        ctx.setBlockState(POS, IC2Blocks.ENERGY_O_MAT_BLOCK.getDefaultState());
        ctx.setBlockState(POS.up(), Blocks.CHEST.getDefaultState());
        TileEntityEnergyOMat seller = ctx.getBlockEntity(POS);
        seller.setFacing(Direction.NORTH);
        seller.demandSlot.put(new ItemStack(Items.DIAMOND));
        ctx.assertTrue(seller.getDemandedEnergy() == 0, "demands energy before payment");

        seller.inputSlot.put(new ItemStack(Items.DIAMOND));
        seller.tick();
        ctx.assertTrue(seller.paidFor == 1000 && seller.inputSlot.isEmpty(), "paid " + seller.paidFor);
        ctx.assertTrue(LogisticsGameTest.count(ctx, POS.up(), Items.DIAMOND) == 1, "payment not stored");
        ctx.assertTrue(seller.getDemandedEnergy() == 1000, "demand " + seller.getDemandedEnergy());
        ctx.assertTrue(!seller.acceptsEnergyFrom(null, Direction.NORTH) && seller.acceptsEnergyFrom(null, Direction.SOUTH), "sides");
        ctx.assertTrue(seller.emitsEnergyTo(null, Direction.NORTH) && !seller.emitsEnergyTo(null, Direction.SOUTH), "output side");

        double rejected = seller.injectEnergy(Direction.SOUTH, 1500, 32);
        ctx.assertTrue(rejected == 500 && seller.euBuffer == 1000 && seller.paidFor == 0, "rejected " + rejected + " buffer " + seller.euBuffer);
        ctx.assertTrue(seller.getOfferedEnergy() == 1000, "offered " + seller.getOfferedEnergy());

        // the owner lowers the price per payment; it never goes below 100
        PlayerEntity owner = ctx.createMockPlayer(GameMode.SURVIVAL);
        seller.onPlaced(new ItemStack(IC2Blocks.ENERGY_O_MAT_BLOCK), owner);
        seller.onGuiEvent(owner, "offer:-1000");
        ctx.assertTrue(seller.euOffer == 100, "offer " + seller.euOffer);
        seller.onGuiEvent(owner, "offer:10000");
        ctx.assertTrue(seller.euOffer == 10100, "offer " + seller.euOffer);
        ctx.complete();
    }
}
