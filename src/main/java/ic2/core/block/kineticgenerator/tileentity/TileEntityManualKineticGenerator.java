package ic2.core.block.kineticgenerator.tileentity;

import ic2.api.energy.tile.IKineticSource;
import ic2.core.block.TileEntityBlock;
import ic2.core.init.IC2Blocks;
import ic2.core.init.MainConfig;
import ic2.core.util.ConfigUtil;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * Fabric port of TileEntityManualKineticGenerator - the crank handle.
 *
 * <p>Each right click by a player who is not starving puts 400 kU (20 for a fake player) times
 * {@code balance/energy/kineticgenerator/manual} into it, up to 1000 kU, and costs 0.25 exhaustion.
 * At most ten clicks a tick count. The kinetic energy leaves on any side.
 */
public class TileEntityManualKineticGenerator extends TileEntityBlock implements IKineticSource {
    public static final int MAX_CLICKS_PER_TICK = 10;
    public static final int MAX_KU = 1000;

    public int clicks;
    public int currentKU;

    public TileEntityManualKineticGenerator(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type == null ? IC2Blocks.MANUAL_KINETIC_BE : type, pos, state);
    }

    public TileEntityManualKineticGenerator(BlockPos pos, BlockState state) {
        this(null, pos, state);
    }

    private static float outputModifier() {
        return Math.round(ConfigUtil.getDouble(MainConfig.get(), "balance/energy/kineticgenerator/manual"));
    }

    @Override
    public void tick() {
        clicks = 0;
    }

    @Override
    public boolean onActivated(PlayerEntity player, Hand hand) {
        playerClicked(player);
        return true;
    }

    public void playerClicked(PlayerEntity player) {
        if (player.getHungerManager().getFoodLevel() <= 6 || !(player instanceof ServerPlayerEntity) || clicks >= MAX_CLICKS_PER_TICK) {
            return;
        }

        int ku = player instanceof FakePlayer ? 20 : 400;
        ku = (int) (ku * outputModifier());
        currentKU = Math.min(currentKU + ku, MAX_KU);
        player.addExhaustion(0.25F);
        clicks++;
        markDirty();
    }

    @Override
    public int maxrequestkineticenergyTick(Direction directionFrom) {
        return drawKineticEnergy(directionFrom, Integer.MAX_VALUE, true);
    }

    @Override
    public int getConnectionBandwidth(Direction side) {
        return MAX_KU;
    }

    @Override
    public int requestkineticenergy(Direction directionFrom, int amount) {
        return drawKineticEnergy(directionFrom, amount, false);
    }

    @Override
    public int drawKineticEnergy(Direction side, int request, boolean simulate) {
        int max = Math.min(currentKU, request);
        if (!simulate) {
            currentKU -= max;
        }
        return max;
    }

    @Override
    public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.writeNbt(nbt, lookup);
        nbt.putInt("currentKU", currentKU);
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.readNbt(nbt, lookup);
        currentKU = nbt.getInt("currentKU");
    }
}
