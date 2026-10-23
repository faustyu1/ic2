package ic2.core.block.personal;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.stream.Stream;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Fabric port of TradingMarket - the wireless trade-o-mats of a world, which a trading terminal
 * finds within its range. A trade-o-mat joins when a remote interface upgrade is used on it.
 *
 * <p>1.12.2 kept one market in each WorldData; here the market itself keeps one per world.
 */
public final class TradingMarket {
    private static final Map<World, TradingMarket> MARKETS = Collections.synchronizedMap(new WeakHashMap<>());

    private final Set<BlockPos> traders = new HashSet<>();

    public static TradingMarket get(World world) {
        return MARKETS.computeIfAbsent(world, ignored -> new TradingMarket());
    }

    public void registerTradeOMat(TileEntityTradeOMat tradeOMat) {
        traders.add(tradeOMat.getPos().toImmutable());
    }

    public void unregisterTradeOMat(TileEntityTradeOMat tradeOMat) {
        traders.remove(tradeOMat.getPos());
    }

    /** The trade-o-mats within the radius of a position. */
    public Stream<BlockPos> tradersAround(BlockPos position, int radius) {
        long squareRadius = (long) radius * radius;
        return traders.stream().filter(pos -> position.getSquaredDistance(pos) <= squareRadius);
    }
}
