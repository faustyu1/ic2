package ic2.core.block.machine.tileentity;

import ic2.api.upgrade.IUpgradableBlock;
import ic2.api.upgrade.UpgradableProperty;
import ic2.core.IC2;
import ic2.core.block.TileEntityInventory;
import ic2.core.block.comp.Fluids;
import ic2.core.block.generator.tileentity.TileEntitySolarGenerator;
import ic2.core.block.invslot.InvSlot;
import ic2.core.block.invslot.InvSlotConsumableLiquid;
import ic2.core.block.invslot.InvSlotConsumableLiquidByManager;
import ic2.core.block.invslot.InvSlotConsumableLiquidByTank;
import ic2.core.block.invslot.InvSlotOutput;
import ic2.core.block.invslot.InvSlotUpgrade;
import ic2.core.init.IC2Blocks;
import ic2.core.ref.FluidName;
import ic2.core.ref.TeBlock;
import ic2.core.util.LiquidUtil;
import java.util.EnumSet;
import java.util.Set;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.fluid.Fluid;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.BiomeDictionary;

/**
 * Fabric port of TileEntitySolarDestiller - the solar distiller.
 *
 * <p>While more than half the sunlight reaches the block above it, it distils 1 mB of water from
 * its 10 000 mB water tank into its 10 000 mB distilled water tank every 72 ticks - 36 in hot
 * biomes, 144 in cold ones. No energy.
 */
public class TileEntitySolarDistiller extends TileEntityInventory implements IUpgradableBlock {
    public final Fluids.InternalFluidTank inputTank;
    public final Fluids.InternalFluidTank outputTank;
    private int tickrate = -1;
    private int updateTicker;
    private float skyLight;
    public final InvSlotOutput wateroutputSlot;
    public final InvSlotOutput destiwateroutputSlott;
    public final InvSlotConsumableLiquidByManager waterinputSlot;
    public final InvSlotConsumableLiquidByTank destiwaterinputSlot;
    public final InvSlotUpgrade upgradeSlot;

    public TileEntitySolarDistiller(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type == null ? IC2Blocks.SOLAR_DISTILLER_BE : type, pos, state, TeBlock.solar_distiller);
        this.inputTank = fluids().addTankInsert("inputTank", 10000, fluid -> fluid == water());
        this.outputTank = fluids().addTankExtract("outputTank", 10000);
        this.waterinputSlot = new InvSlotConsumableLiquidByManager(this, "waterInput", InvSlot.Access.I, 1, InvSlot.InvSide.TOP,
                InvSlotConsumableLiquid.OpType.Drain, new ic2.api.recipe.ILiquidAcceptManager() {
                    @Override
                    public boolean acceptsFluid(Fluid fluid) {
                        return fluid == water();
                    }

                    @Override
                    public Set<Fluid> getAcceptedFluids() {
                        return Set.of(water());
                    }
                });
        this.destiwaterinputSlot = new InvSlotConsumableLiquidByTank(this, "destilledWaterInput", InvSlot.Access.I, 1,
                InvSlot.InvSide.BOTTOM, InvSlotConsumableLiquid.OpType.Fill, outputTank);
        this.wateroutputSlot = new InvSlotOutput(this, "waterOutput", 1);
        this.destiwateroutputSlott = new InvSlotOutput(this, "destilledWaterOutput", 1);
        this.upgradeSlot = new InvSlotUpgrade(this, "upgrade", 3);
    }

    public TileEntitySolarDistiller(BlockPos pos, BlockState state) {
        this(null, pos, state);
    }

    private static Fluid water() {
        return LiquidUtil.getFluid("water");
    }

    @Override
    public void tick() {
        if (world == null || world.isClient) {
            return;
        }
        if (tickrate < 0) {
            tickrate = getTickRate();
            updateTicker = IC2.random.nextInt(tickrate);
        }

        waterinputSlot.processIntoTank(inputTank, wateroutputSlot);
        if (++updateTicker >= tickrate) {
            updateSunVisibility();
            if (canWork()) {
                inputTank.drainInternal(LiquidUtil.mb(1), true);
                outputTank.fillInternal(FluidName.distilled_water.getVariant(), LiquidUtil.mb(1), true);
                markDirty();
            }
            updateTicker = 0;
        }
        destiwaterinputSlot.processFromTank(outputTank, destiwateroutputSlott);
        upgradeSlot.tick();
    }

    public void updateSunVisibility() {
        skyLight = TileEntitySolarGenerator.getSkyLight(world, pos.up());
    }

    public int getTickRate() {
        var biome = world.getBiome(pos);
        if (BiomeDictionary.hasType(biome, BiomeDictionary.Type.HOT)) {
            return 36;
        }
        return BiomeDictionary.hasType(biome, BiomeDictionary.Type.COLD) ? 144 : 72;
    }

    public boolean canWork() {
        return inputTank.getFluidAmount() >= LiquidUtil.mb(1) && outputTank.getFluidAmount() < outputTank.getCapacity() && skyLight > 0.5;
    }

    @Override
    public boolean getGuiCondition(String name) {
        return "working".equals(name) ? canWork() : super.getGuiCondition(name);
    }

    @Override
    public Set<UpgradableProperty> getUpgradableProperties() {
        return EnumSet.of(UpgradableProperty.ItemConsuming, UpgradableProperty.ItemProducing, UpgradableProperty.FluidProducing);
    }

    @Override
    public double getEnergy() {
        return 40.0;
    }

    @Override
    public boolean useEnergy(double amount) {
        return true;
    }
}
