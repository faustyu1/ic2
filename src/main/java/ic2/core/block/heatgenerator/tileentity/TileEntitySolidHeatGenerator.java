package ic2.core.block.heatgenerator.tileentity;

import ic2.core.block.TileEntityHeatSourceInventory;
import ic2.core.block.invslot.InvSlotConsumableFuel;
import ic2.core.block.invslot.InvSlotOutput;
import ic2.core.init.IC2Blocks;
import ic2.core.init.IC2ItemVariants;
import ic2.core.init.MainConfig;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;

/**
 * Fabric port of {@code TileEntitySolidHeatGenerator}: the solid fuel firebox. It burns furnace
 * fuel at a quarter of the furnace burn time, 20 HU a tick (times {@code balance/energy/
 * heatgenerator/solid}), and leaves ashes half of the time a piece burns out.
 */
public class TileEntitySolidHeatGenerator extends TileEntityHeatSourceInventory {
    private int heatbuffer = 0;
    public int fuel = 0;
    public int itemFuelTime = 0;
    public final InvSlotConsumableFuel fuelSlot;
    public final InvSlotOutput outputslot;

    public TileEntitySolidHeatGenerator(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type == null ? IC2Blocks.SOLID_HEAT_BE : type, pos, state);
        this.fuelSlot = new InvSlotConsumableFuel(this, "fuel", 1, false);
        this.outputslot = new InvSlotOutput(this, "output", 1);
    }

    public TileEntitySolidHeatGenerator(BlockPos pos, BlockState state) {
        this(null, pos, state);
    }

    public static int emittedHU() {
        return Math.round(20.0F * (float) MainConfig.get().getDouble("balance/energy/heatgenerator/solid", 1.0));
    }

    private static ItemStack ashes() {
        Item item = IC2ItemVariants.get("misc_resource", "ashes");
        return new ItemStack(item == null ? Items.AIR : item);
    }

    @Override
    public void updateEntityServer() {
        super.updateEntityServer();
        boolean changed = false;
        if (needsFuel()) {
            changed = gainFuel();
        }

        boolean active = gainHeat();
        if (changed || active) {
            markDirty();
        }
        setActive(active);
    }

    public boolean gainHeat() {
        if (!isConverting()) {
            return false;
        }

        heatbuffer += getMaxHeatEmittedPerTick();
        fuel--;
        if (fuel == 0 && world.random.nextInt(2) == 1) {
            outputslot.add(ashes());
        }
        return true;
    }

    public boolean needsFuel() {
        return fuel <= 0 && getHeatBuffer() == 0;
    }

    public boolean gainFuel() {
        if (!outputslot.canAdd(ashes())) {
            return false;
        }

        int fuelValue = fuelSlot.consumeFuel() / 4;
        if (fuelValue == 0) {
            return false;
        }
        fuel += fuelValue;
        itemFuelTime = fuelValue;
        return true;
    }

    public boolean isConverting() {
        return fuel > 0;
    }

    @Override
    protected int fillHeatBuffer(int maxAmount) {
        int amount = Math.min(maxAmount, heatbuffer);
        heatbuffer -= amount;
        return amount;
    }

    @Override
    public int getMaxHeatEmittedPerTick() {
        return emittedHU();
    }

    @Override
    public double getGuiValue(String name) {
        if ("fuel".equals(name)) {
            return fuel == 0 || itemFuelTime == 0 ? 0.0 : (double) fuel / itemFuelTime;
        }
        return super.getGuiValue(name);
    }

    @Override
    public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.writeNbt(nbt, registries);
        nbt.putInt("fuel", fuel);
        nbt.putInt("itemFuelTime", itemFuelTime);
        nbt.putInt("heatbuffer", heatbuffer);
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.readNbt(nbt, registries);
        fuel = nbt.getInt("fuel");
        itemFuelTime = nbt.getInt("itemFuelTime");
        heatbuffer = nbt.getInt("heatbuffer");
    }
}
