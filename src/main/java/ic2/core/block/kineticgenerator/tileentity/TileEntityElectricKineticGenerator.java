package ic2.core.block.kineticgenerator.tileentity;

import ic2.api.energy.tile.IEnergyEmitter;
import ic2.api.energy.tile.IKineticSource;
import ic2.core.block.invslot.InvSlotConsumable;
import ic2.core.block.invslot.InvSlotConsumableItemStack;
import ic2.core.block.invslot.InvSlotDischarge;
import ic2.core.block.machine.tileentity.TileEntityElectricMachine;
import ic2.core.init.IC2Blocks;
import ic2.core.init.IC2ItemVariants;
import ic2.core.init.MainConfig;
import ic2.core.ref.TeBlock;
import ic2.core.util.ConfigUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * Fabric port of TileEntityElectricKineticGenerator - the electric motor.
 *
 * <p>It turns EU into kU, four kU for each EU (times {@code balance/energy/kineticgenerator/electric}),
 * keeping up to 1000 kU. Each electric motor in its ten slots lets 100 kU a tick out of its front.
 * It holds 10 000 EU and takes up to tier 4 on every side but the front.
 */
public class TileEntityElectricKineticGenerator extends TileEntityElectricMachine implements IKineticSource {
    public static final int MAX_KU = 1000;

    public final InvSlotConsumable slotMotor;
    public final InvSlotDischarge dischargeSlot;
    private final float kuPerEU;
    public double ku;

    public TileEntityElectricKineticGenerator(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type == null ? IC2Blocks.ELECTRIC_KINETIC_BE : type, pos, state, TeBlock.electric_kinetic_generator, 10000, 4);
        this.kuPerEU = 4.0F * (float) ConfigUtil.getDouble(MainConfig.get(), "balance/energy/kineticgenerator/electric");
        Item motor = IC2ItemVariants.get("crafting", "electric_motor");
        this.slotMotor = new InvSlotConsumableItemStack(this, "motor", 10, new ItemStack(motor == null ? Items.AIR : motor));
        this.slotMotor.setStackSizeLimit(1);
        this.dischargeSlot = new InvSlotDischarge(this, "discharge", 4);
    }

    public TileEntityElectricKineticGenerator(BlockPos pos, BlockState state) {
        this(null, pos, state);
    }

    @Override
    public void tick() {
        if (world == null || world.isClient) {
            return;
        }

        drainDischargeSlot(dischargeSlot);
        boolean newActive = false;
        if (MAX_KU - ku > 1.0) {
            double max = Math.min(MAX_KU - ku, energyStored * kuPerEU);
            energyStored -= max / kuPerEU;
            ku += max;
            if (max > 0.0) {
                markDirty();
                newActive = true;
            }
        }
        setActive(newActive);
    }

    @Override
    public boolean acceptsEnergyFrom(IEnergyEmitter emitter, Direction side) {
        return side != getFacing();
    }

    /** What may leave a tick: 100 kU for every motor. */
    public int getMaxKU() {
        int counter = 0;
        int perMotor = getMaxKUForGUI() / 10;
        for (int i = 0; i < slotMotor.size(); i++) {
            if (!slotMotor.isEmpty(i)) {
                counter += perMotor;
            }
        }
        return counter;
    }

    public int getMaxKUForGUI() {
        return MAX_KU;
    }

    @Override
    public double getGuiAmount(String name) {
        return "ku".equals(name) ? getMaxKU() : super.getGuiAmount(name);
    }

    @Override
    public double getGuiCapacity(String name) {
        return "ku".equals(name) ? getMaxKUForGUI() : super.getGuiCapacity(name);
    }

    // ---------------------------------------------------------------- IKineticSource: out of the front

    @Override
    public int maxrequestkineticenergyTick(Direction directionFrom) {
        return drawKineticEnergy(directionFrom, Integer.MAX_VALUE, true);
    }

    @Override
    public int getConnectionBandwidth(Direction side) {
        return side != getFacing() ? 0 : getMaxKU();
    }

    @Override
    public int requestkineticenergy(Direction directionFrom, int amount) {
        return drawKineticEnergy(directionFrom, amount, false);
    }

    @Override
    public int drawKineticEnergy(Direction side, int request, boolean simulate) {
        if (side != getFacing()) {
            return 0;
        }
        int out = Math.min(request, (int) Math.min(getMaxKU(), ku));
        if (!simulate) {
            ku -= out;
            markDirty();
        }
        return out;
    }

    @Override
    public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.writeNbt(nbt, lookup);
        nbt.putDouble("ku", ku);
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.readNbt(nbt, lookup);
        ku = nbt.getDouble("ku");
    }
}
