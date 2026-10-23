package ic2.core.block.reactor.tileentity;

import ic2.core.block.invslot.InvSlotConsumableItemStack;
import ic2.core.block.invslot.InvSlotUpgrade;
import ic2.core.block.machine.tileentity.TileEntityElectricMachine;
import ic2.core.item.reactor.ItemReactorCondensator;
import ic2.core.ref.TeBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import java.util.EnumSet;
import java.util.Set;

/**
 * Fabric port of TileEntityAbstractRCI - the box that refills a reactor's condensators.
 *
 * <p>It sits against the reactor or one of its chambers, and once a condensator in the grid is
 * more than 85% spent it swaps in one unit of the coolant it is stocked with and resets the
 * component. Refilling a nearly full condensator would waste the coolant, which is why the
 * original waited for that threshold. It takes energy on every side that does not touch the
 * reactor or one of its chambers.
 */
public abstract class TileEntityAbstractRCI extends TileEntityElectricMachine {
    private static final double ENERGY_PER_OPERATION = 1000.0;
    private static final double REFILL_THRESHOLD = 0.85;

    private final ItemStack target;
    public final InvSlotConsumableItemStack inputSlot;
    public final InvSlotUpgrade upgradeSlot;

    private TileEntityNuclearReactorElectric reactor;
    /** The sides energy comes in on: all but those against the reactor or a chamber. */
    private Set<Direction> sinkSides = EnumSet.allOf(Direction.class);

    protected TileEntityAbstractRCI(BlockEntityType<?> type, BlockPos pos, BlockState state, TeBlock teBlock,
                                    ItemStack target, ItemStack coolant) {
        super(type, pos, state, teBlock, 48000, 2);
        this.target = target;
        this.inputSlot = new InvSlotConsumableItemStack(this, "input", 9, coolant);
        this.upgradeSlot = new InvSlotUpgrade(this, "upgrade", 4);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (world != null && !world.isClient) {
            updateEnergyFacings();
        }
        updateReactor();
    }

    @Override
    public void onNeighborUpdate() {
        super.onNeighborUpdate();
        updateEnergyFacings();
        updateReactor();
    }

    @Override
    public void setFacing(Direction facing) {
        super.setFacing(facing);
        if (world != null && !world.isClient) {
            updateEnergyFacings();
            updateReactor();
        }
    }

    @Override
    public boolean acceptsEnergyFrom(ic2.api.energy.tile.IEnergyEmitter emitter, Direction side) {
        return sinkSides.contains(side);
    }

    /**
     * Works out the sides energy comes in on; when they changed, the box leaves the energy net and
     * joins it again so the net sees them, as the energy component did.
     */
    public void updateEnergyFacings() {
        Set<Direction> sides = EnumSet.noneOf(Direction.class);
        for (Direction side : Direction.values()) {
            var te = world.getBlockEntity(pos.offset(side));
            if (!(te instanceof TileEntityNuclearReactorElectric) && !(te instanceof TileEntityReactorChamberElectric)) {
                sides.add(side);
            }
        }

        if (!sides.equals(sinkSides)) {
            sinkSides = sides;
            if (isAddedToEnet()) {
                ic2.api.energy.EnergyNet.instance.removeTile(this);
                ic2.api.energy.EnergyNet.instance.addTile(this);
            }
        }
    }

    @Override
    public void tick() {
        if (world == null || world.isClient) {
            return;
        }

        updateReactor();
        setActive(!inputSlot.isEmpty() && energyStored >= ENERGY_PER_OPERATION && reactor != null);

        boolean changed = false;
        if (getActive()) {
            for (int i = 0; i < reactor.reactorSlot.rawSize(); i++) {
                ItemStack component = reactor.reactorSlot.get(i);
                if (component.isEmpty() || !ItemStack.areItemsEqual(component, target)) {
                    continue;
                }

                if (!(component.getItem() instanceof ItemReactorCondensator condensator)) {
                    continue;
                }

                int max = condensator.getMaxCustomDamage(component);
                if (max <= 0 || (double) condensator.getCustomDamage(component) / max <= REFILL_THRESHOLD) {
                    continue;
                }

                if (!inputSlot.consume(1).isEmpty() && useEnergy(ENERGY_PER_OPERATION)) {
                    condensator.setCustomDamage(component, 0);
                    changed = true;
                }
            }
        }

        changed |= upgradeSlot.tickNoMark();
        if (changed) {
            markDirty();
        }
    }

    /** The reactor this box faces, straight ahead or through one of its chambers. */
    private void updateReactor() {
        if (world == null) {
            reactor = null;
            return;
        }

        var facing = world.getBlockEntity(pos.offset(getFacing().getOpposite()));
        if (facing instanceof TileEntityNuclearReactorElectric core) {
            reactor = core;
        } else if (facing instanceof TileEntityReactorChamberElectric chamber
                && chamber.getReactorInstance() instanceof TileEntityNuclearReactorElectric core) {
            reactor = core;
        } else {
            reactor = null;
        }
    }
}
