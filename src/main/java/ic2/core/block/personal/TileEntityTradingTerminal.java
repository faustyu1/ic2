package ic2.core.block.personal;

import ic2.api.upgrade.IUpgradableBlock;
import ic2.api.upgrade.UpgradableProperty;
import ic2.core.block.TileEntityInventory;
import ic2.core.block.invslot.InvSlotUpgrade;
import ic2.core.init.IC2Blocks;
import java.util.Collections;
import java.util.Set;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;

/**
 * Fabric port of TileEntityTradingTerminal - the terminal that reaches the wireless trade-o-mats
 * of the trading market within its range: 512 blocks, doubled by each remote interface upgrade in
 * its hidden upgrade slot (up to 16, put in by shift-clicking). 1.12.2 never finished its window -
 * it showed a placeholder list - so the window here only tells how many trade-o-mats are in range.
 */
public class TileEntityTradingTerminal extends TileEntityInventory implements IUpgradableBlock, InvSlotUpgrade.Listener {
    public static final int DEFAULT_RANGE = 512;

    protected int range = DEFAULT_RANGE;
    public final InvSlotUpgrade rangeUpgrade;

    public TileEntityTradingTerminal(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type == null ? IC2Blocks.TRADING_TERMINAL_BE : type, pos, state);
        this.rangeUpgrade = new InvSlotUpgrade(this, "range", 1);
        this.rangeUpgrade.setStackSizeLimit(16);
    }

    public TileEntityTradingTerminal(BlockPos pos, BlockState state) {
        this(null, pos, state);
    }

    @Override
    public void tick() {
        if (world == null || world.isClient) {
            return;
        }
        rangeUpgrade.tick();
    }

    @Override
    public void onUpgradesChanged() {
        if (rangeUpgrade != null) {
            range = rangeUpgrade.getRemoteRange(DEFAULT_RANGE);
        }
    }

    public int getRange() {
        return range;
    }

    /** The number of wireless trade-o-mats within range. */
    public int countTraders() {
        return world == null ? 0 : (int) TradingMarket.get(world).tradersAround(pos, range).count();
    }

    @Override
    public double getGuiAmount(String name) {
        return switch (name) {
            case "range" -> range;
            case "traders" -> countTraders();
            default -> super.getGuiAmount(name);
        };
    }

    @Override
    public Set<UpgradableProperty> getUpgradableProperties() {
        return Collections.singleton(UpgradableProperty.RemotelyAccessible);
    }

    @Override
    public double getEnergy() {
        return 0.0;
    }

    @Override
    public boolean useEnergy(double amount) {
        return false;
    }
}
