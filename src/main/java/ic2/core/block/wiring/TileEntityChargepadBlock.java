package ic2.core.block.wiring;

import ic2.api.item.ElectricItem;
import ic2.core.IC2;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * Fabric port of TileEntityChargepadBlock - an energy storage block a player stands on to charge
 * everything they wear and carry.
 *
 * <p>Every second tick the pad charges the armour, then the inventory, of the last player that
 * stood on it, each item up to the pad's output a tick; it is active while it does. Energy comes
 * in from the sides and below except its face, and leaves through its face like any storage
 * block. Its redstone signal is on while it charges, or while it does not.
 */
public abstract class TileEntityChargepadBlock extends TileEntityElectricBlock {
    private int updateTicker = IC2.random.nextInt(getTickRate());
    private PlayerEntity player;

    public TileEntityChargepadBlock(BlockEntityType<?> type, BlockPos pos, BlockState state, int tier, int output, int maxStorage) {
        super(type, pos, state, tier, output, maxStorage);
    }

    protected int getTickRate() {
        return 2;
    }

    @Override
    public void onEntityCollision(Entity entity) {
        if (world != null && !world.isClient && entity instanceof PlayerEntity standing) {
            player = standing;
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (world == null || world.isClient || updateTicker++ % getTickRate() != 0) {
            return;
        }
        if (player != null && energyStored >= 1.0) {
            setActive(true);
            for (ItemStack stack : player.getInventory().armor) {
                chargeItem(stack, output);
            }
            for (ItemStack stack : player.getInventory().main) {
                chargeItem(stack, output);
            }
            player = null;
            markDirty();
        } else if (getActive()) {
            setActive(false);
            markDirty();
        }
    }

    protected void chargeItem(ItemStack stack, int chargeFactor) {
        if (stack.isEmpty() || ElectricItem.manager == null) {
            return;
        }
        double freeAmount = ElectricItem.manager.charge(stack, Double.POSITIVE_INFINITY, tier, true, true);
        if (freeAmount <= 0.0) {
            return;
        }
        double charge = Math.min(freeAmount, chargeFactor * getTickRate());
        charge = Math.min(charge, energyStored);
        energyStored -= ElectricItem.manager.charge(stack, charge, tier, true, false);
    }

    /** Energy comes in on every side but the face and the top. */
    @Override
    public boolean acceptsEnergyFrom(ic2.api.energy.tile.IEnergyEmitter emitter, Direction side) {
        return side != getFacing() && side != Direction.UP;
    }

    @Override
    protected int redstoneModes() {
        return 2;
    }

    @Override
    protected boolean shouldEmitRedstone() {
        return redstoneMode == 0 && getActive() || redstoneMode == 1 && !getActive();
    }

    @Override
    protected boolean shouldEmitEnergy() {
        return true;
    }

    @Override
    public String getRedstoneModeKey() {
        return "blockChargepad.gui.mod.redstone" + redstoneMode;
    }
}
