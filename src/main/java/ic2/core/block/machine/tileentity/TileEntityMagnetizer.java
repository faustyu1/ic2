package ic2.core.block.machine.tileentity;

import ic2.api.upgrade.IUpgradableBlock;
import ic2.api.upgrade.UpgradableProperty;
import ic2.core.block.invslot.InvSlotDischarge;
import ic2.core.block.invslot.InvSlotUpgrade;
import ic2.core.init.IC2Blocks;
import ic2.core.ref.TeBlock;
import java.util.EnumSet;
import java.util.Set;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;

/**
 * Fabric port of TileEntityMagnetizer: powers a pole of iron fences in front of it, which lifts a
 * player standing in it - 2 EU a tick, shared between the magnetizers of the pole - see
 * {@link ic2.core.block.BlockIC2Fence}. 100 EU, tier 1.
 *
 * <p>Whether it has the energy to lift is shown as its active state, which is also what the
 * client reads to move the player, since the energy itself only lives on the server.
 */
public class TileEntityMagnetizer extends TileEntityElectricMachine implements IUpgradableBlock, InvSlotUpgrade.Listener {
    public static final int DEFAULT_MAX_ENERGY = 100;
    private static final double BOOST_ENERGY = 2.0;

    public final InvSlotUpgrade upgradeSlot;
    public final InvSlotDischarge dischargeSlot;

    public TileEntityMagnetizer(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type == null ? IC2Blocks.MAGNETIZER_BE : type, pos, state, TeBlock.magnetizer, DEFAULT_MAX_ENERGY, 1);
        this.upgradeSlot = new InvSlotUpgrade(this, "upgrade", 4);
        this.dischargeSlot = new InvSlotDischarge(this, "discharge", 1);
    }

    public TileEntityMagnetizer(BlockPos pos, BlockState state) {
        this(null, pos, state);
    }

    @Override
    public void onUpgradesChanged() {
        setTier(upgradeSlot.getTier(defaultTier));
        setCapacity(upgradeSlot.getEnergyStorage(DEFAULT_MAX_ENERGY, 0, 0));
    }

    @Override
    public void tick() {
        if (world == null || world.isClient) {
            return;
        }
        drainDischargeSlot(dischargeSlot);
        setActive(canBoost());
    }

    /** How far up and down the pole it reaches. */
    public int distance() {
        return 20 + upgradeSlot.augmentation;
    }

    public boolean canBoost() {
        return world != null && world.isClient ? getCachedState().get(ic2.core.block.BlockTileEntity.ACTIVE) : getEnergy() >= BOOST_ENERGY;
    }

    public void boost(double multiplier) {
        if (world != null && !world.isClient) {
            useEnergy(BOOST_ENERGY * multiplier);
        }
    }

    @Override
    public Set<UpgradableProperty> getUpgradableProperties() {
        return EnumSet.of(UpgradableProperty.Augmentable, UpgradableProperty.RedstoneSensitive, UpgradableProperty.Transformer,
                UpgradableProperty.EnergyStorage);
    }
}
