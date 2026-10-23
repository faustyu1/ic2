package ic2.core.block.generator.tileentity;

import ic2.core.IC2;
import ic2.core.block.invslot.InvSlot;
import ic2.core.block.invslot.InvSlotConsumableLiquid;
import ic2.core.init.IC2Blocks;
import ic2.core.init.MainConfig;
import ic2.core.ref.TeBlock;
import ic2.core.util.ConfigUtil;
import ic2.core.util.LiquidUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;

/**
 * Fabric port of TileEntityWaterGenerator - the water mill.
 *
 * <p>It runs on the water blocks among the 26 around it: each adds one to a store every tick
 * ({@code balance/energy/generator/water} scales it), and every hundred in the store make one EU,
 * so a mill sunk in water gives about a quarter EU/t. Water poured into its slot is faster: a
 * bucket is 500 ticks at 1 EU/t, a cell (which leaves no container behind) 500 at 2 EU/t. The
 * gauge shows the poured water, at most 2000 ticks of it. Whether hoppers may use the slot is
 * {@code balance/watermillAutomation}.
 */
public class TileEntityWaterGenerator extends TileEntityBaseRotorGenerator {
    private static final int TICK_RATE = 128;

    public final InvSlotConsumableLiquid fuelSlot;

    private int ticker = IC2.random.nextInt(TICK_RATE);
    /** Water blocks around the mill, as last counted. */
    public int water = 0;
    /** Hundredths of an EU collected from the surrounding water. */
    public int microStorage = 0;
    public int maxWater = 2000;

    public TileEntityWaterGenerator(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type == null ? IC2Blocks.WATER_BE : type, pos, state, TeBlock.water_generator, 2.0, 1, 4, 2);
        boolean allowAutomation = ConfigUtil.getBool(MainConfig.get(), "balance/watermillAutomation");
        this.fuelSlot = new InvSlotConsumableLiquid(this, "fuel",
                allowAutomation ? InvSlot.Access.IO : InvSlot.Access.NONE, 1,
                InvSlot.InvSide.TOP, InvSlotConsumableLiquid.OpType.Drain) {
            @Override
            protected boolean acceptsLiquid(Fluid fluid) {
                return fluid != null && "water".equals(LiquidUtil.nameOf(fluid));
            }
        };
    }

    public TileEntityWaterGenerator(BlockPos pos, BlockState state) {
        this(IC2Blocks.WATER_BE, pos, state);
    }

    private static double energyMultiplier() {
        return ConfigUtil.getDouble(MainConfig.get(), "balance/energy/generator/water");
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (world != null && !world.isClient) {
            updateWaterCount();
        }
    }

    @Override
    public boolean gainFuel() {
        if (fuel + 500 > maxWater) {
            return false;
        }

        if (!fuelSlot.isEmpty()) {
            ItemStack liquid = fuelSlot.consume(1);
            if (liquid.isEmpty()) {
                return false;
            }

            fuel += 500;
            // a bucket leaves itself behind and runs at half the speed of a cell
            production = liquid.getItem().hasRecipeRemainder() ? 1.0 : 2.0;
            return true;
        }

        if (fuel <= 0) {
            flowPower();
            production = microStorage / 100;
            microStorage = (int) (microStorage - production * 100.0);
            if (production > 0.0) {
                fuel++;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isConverting() {
        return fuel > 0;
    }

    @Override
    public boolean needsFuel() {
        return fuel <= maxWater;
    }

    /** Adds the surrounding water to the store, recounting it every 128 ticks. */
    public void flowPower() {
        if (++ticker % TICK_RATE == 0) {
            updateWaterCount();
        }

        water = (int) Math.round(water * energyMultiplier());
        if (water > 0) {
            microStorage += water;
        }
    }

    /** Counts the water blocks in the 3x3x3 around the mill. */
    public void updateWaterCount() {
        int count = 0;
        for (int x = -1; x < 2; x++) {
            for (int y = -1; y < 2; y++) {
                for (int z = -1; z < 2; z++) {
                    if (world.getBlockState(pos.add(x, y, z)).isOf(Blocks.WATER)) {
                        count++;
                    }
                }
            }
        }
        water = count;
    }

    @Override
    public String getOperationSoundFile() {
        return "Generators/WatermillLoop.ogg";
    }

    @Override
    protected boolean delayActiveUpdate() {
        return true;
    }

    @Override
    protected boolean shouldRotorRotate() {
        return water > 0 || fuel > 0;
    }

    @Override
    protected float rotorSpeedFactor() {
        return fuel > 0 ? 1.0F : water / 25.0F;
    }

    @Override
    public double getGuiValue(String name) {
        return "water".equals(name) ? (double) fuel / maxWater : super.getGuiValue(name);
    }

    @Override
    public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.writeNbt(nbt, lookup);
        nbt.putInt("microStorage", microStorage);
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.readNbt(nbt, lookup);
        microStorage = nbt.getInt("microStorage");
    }
}
