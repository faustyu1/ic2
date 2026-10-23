package ic2.core.block.steam;

import ic2.core.IC2;
import ic2.core.block.TileEntityInventory;
import ic2.core.block.invslot.InvSlotConsumableFuel;
import ic2.core.block.invslot.InvSlotOutput;
import ic2.core.init.IC2Blocks;
import ic2.core.init.IC2ItemVariants;
import ic2.core.ref.TeBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * Fabric port of TileEntityBurningBox - the firebox under the coke kiln.
 *
 * <p>It burns furnace fuel at a quarter of its furnace burn time, leaving ashes half the time a
 * piece burns out. The temperature it gives off its top climbs along a cubic curve to 1400 K in
 * 55 seconds of burning and falls back the same way once the fuel is gone.
 */
public class TileEntityBurningBox extends TileEntityInventory {
    protected int heat;
    protected int delta;
    public int activityMeter;
    public int ticksSinceLastActiveUpdate = IC2.random.nextInt(128);
    public int fuel;
    private int remainingFuel;
    public final InvSlotConsumableFuel fuelSlot;
    public final InvSlotOutput ashesSlot;

    public TileEntityBurningBox(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type == null ? IC2Blocks.BURNING_BOX_BE : type, pos, state, TeBlock.burning_box);
        this.fuelSlot = new InvSlotConsumableFuel(this, "fuel", 1, false);
        this.ashesSlot = new InvSlotOutput(this, "ashes", 1);
    }

    public TileEntityBurningBox(BlockPos pos, BlockState state) {
        this(null, pos, state);
    }

    private static ItemStack ashes() {
        Item item = IC2ItemVariants.get("misc_resource", "ashes");
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    @Override
    public void tick() {
        if (world == null || world.isClient) {
            return;
        }

        boolean needsInventoryUpdate = false;
        if (needsFuel()) {
            needsInventoryUpdate = gainFuel();
        }

        boolean newActive = work();
        if (needsInventoryUpdate) {
            markDirty();
        }

        if (!delayActiveUpdate()) {
            setActive(newActive);
        } else {
            if (ticksSinceLastActiveUpdate % 128 == 0) {
                setActive(activityMeter > 0);
                activityMeter = 0;
            }
            activityMeter += newActive ? 1 : -1;
            ticksSinceLastActiveUpdate++;
        }
    }

    /** The temperature in kelvin the box gives to what stands on it. */
    public int getProvidedHeat(Direction side) {
        return side == Direction.UP ? heat : 0;
    }

    public boolean needsFuel() {
        return fuel <= 0;
    }

    public boolean gainFuel() {
        if (!ashesSlot.canAdd(ashes())) {
            return false;
        }
        int fuelValue = fuelSlot.consumeFuel() / 4;
        if (fuelValue == 0) {
            return false;
        }
        fuel += fuelValue;
        remainingFuel = fuelValue;
        return true;
    }

    public boolean work() {
        if (fuel > 0) {
            fuel--;
            if (fuel == 0 && world.random.nextInt(2) == 1) {
                ashesSlot.add(ashes());
            }
            delta = Math.min(delta + 1, 1100);
            int temp = 55 - delta / 20;
            heat = 1400 + (int) (0.008 * -(temp * temp * temp));
            return true;
        }

        delta = Math.max(delta - 1, 0);
        int temp = delta / 20;
        heat = (int) (0.008 * (temp * temp * temp));
        return false;
    }

    public boolean delayActiveUpdate() {
        return false;
    }

    @Override
    public double getGuiValue(String name) {
        if ("fuel".equals(name)) {
            return fuel == 0 || remainingFuel == 0 ? 0.0 : (double) fuel / remainingFuel;
        }
        return super.getGuiValue(name);
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.readNbt(nbt, lookup);
        delta = nbt.getInt("delta");
        fuel = nbt.getInt("fuel");
        remainingFuel = nbt.getInt("remainingFuel");
    }

    @Override
    public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.writeNbt(nbt, lookup);
        nbt.putInt("delta", delta);
        nbt.putInt("fuel", fuel);
        nbt.putInt("remainingFuel", remainingFuel);
    }
}
