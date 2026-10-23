package ic2.core.block.kineticgenerator.tileentity;

import ic2.api.energy.tile.IHeatSource;
import ic2.api.energy.tile.IKineticSource;
import ic2.api.recipe.ILiquidAcceptManager;
import ic2.api.recipe.ILiquidHeatExchangerManager;
import ic2.api.recipe.Recipes;
import ic2.api.upgrade.IUpgradableBlock;
import ic2.api.upgrade.UpgradableProperty;
import ic2.core.block.TileEntityInventory;
import ic2.core.block.comp.Fluids;
import ic2.core.block.invslot.InvSlot;
import ic2.core.block.invslot.InvSlotConsumableLiquid;
import ic2.core.block.invslot.InvSlotConsumableLiquidByManager;
import ic2.core.block.invslot.InvSlotConsumableLiquidByTank;
import ic2.core.block.invslot.InvSlotOutput;
import ic2.core.block.invslot.InvSlotUpgrade;
import ic2.core.init.IC2Blocks;
import ic2.core.ref.TeBlock;
import ic2.core.util.LiquidUtil;
import java.util.EnumSet;
import java.util.Set;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * Fabric port of TileEntityStirlingKineticGenerator - turns heat into kinetic energy.
 *
 * <p>It draws up to 1000 HU into its buffer from the heat sources on every side but its front,
 * and spends a quarter of that buffer each tick: three parts of four become kU at four kU per HU,
 * the fourth heats the coolant in its input tank into the hot fluid of the output tank (coolant to
 * hot coolant, water to steam... at the rates of {@link Recipes#liquidHeatupManager}). Without
 * fluid to heat or room for the hot one it stops. Up to 2000 kU wait for the kinetic consumer in
 * front of it.
 */
public class TileEntityStirlingKineticGenerator extends TileEntityInventory implements IKineticSource, IUpgradableBlock {
    private static final int PARTS_KU = 3;
    private static final int PARTS_LIQUID = 1;
    private static final int PARTS_TOTAL = 4;

    public final Fluids.InternalFluidTank inputTank;
    public final Fluids.InternalFluidTank outputTank;
    public final InvSlotOutput hotoutputSlot;
    public final InvSlotOutput cooloutputSlot;
    public final InvSlotConsumableLiquidByTank hotfluidinputSlot;
    public final InvSlotConsumableLiquidByManager coolfluidinputSlot;
    public final InvSlotUpgrade upgradeSlot;
    private int heatbuffer;
    private final int maxHeatbuffer = 1000;
    private int kUBuffer;
    private final int maxkUBuffer = 2000;
    private int liquidHeatStored;

    public TileEntityStirlingKineticGenerator(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type == null ? IC2Blocks.STIRLING_KINETIC_BE : type, pos, state, TeBlock.stirling_kinetic_generator);
        ILiquidAcceptManager heatable = heatable();
        this.inputTank = fluids().addTankInsert("inputTank", 2000,
                fluid -> heatable != null && heatable.acceptsFluid(fluid));
        this.outputTank = fluids().addTankExtract("outputTank", 2000);
        this.hotoutputSlot = new InvSlotOutput(this, "hotOutputSlot", 1);
        this.cooloutputSlot = new InvSlotOutput(this, "outputSlot", 1);
        this.coolfluidinputSlot = new InvSlotConsumableLiquidByManager(this, "coolfluidinputSlot", InvSlot.Access.I, 1,
                InvSlot.InvSide.TOP, InvSlotConsumableLiquid.OpType.Drain, heatable);
        this.hotfluidinputSlot = new InvSlotConsumableLiquidByTank(this, "hotfluidoutputSlot", InvSlot.Access.I, 1,
                InvSlot.InvSide.BOTTOM, InvSlotConsumableLiquid.OpType.Fill, outputTank);
        this.upgradeSlot = new InvSlotUpgrade(this, "upgrade", 3);
    }

    public TileEntityStirlingKineticGenerator(BlockPos pos, BlockState state) {
        this(null, pos, state);
    }

    private static ILiquidAcceptManager heatable() {
        return Recipes.liquidHeatupManager == null ? null : Recipes.liquidHeatupManager.getSingleDirectionLiquidManager();
    }

    @Override
    public void tick() {
        if (world == null || world.isClient) {
            return;
        }

        coolfluidinputSlot.processIntoTank(inputTank, cooloutputSlot);
        hotfluidinputSlot.processFromTank(outputTank, hotoutputSlot);
        if (heatbuffer < maxHeatbuffer) {
            heatbuffer += drawHu(maxHeatbuffer - heatbuffer);
        }

        boolean newActive = false;
        FluidVariant input = inputTank.getFluid();
        ILiquidAcceptManager heatable = heatable();
        if (!inputTank.isEmpty() && outputTank.getSpace() > 0
                && heatable != null && heatable.acceptsFluid(input.getFluid()) && kUBuffer < maxkUBuffer) {
            ILiquidHeatExchangerManager.HeatExchangeProperty property = Recipes.liquidHeatupManager.getHeatExchangeProperty(input.getFluid());
            if (outputTank.isEmpty() || outputTank.getFluid().getFluid() == property.outputFluid) {
                int heatbufferToUse = heatbuffer / PARTS_TOTAL;
                int convertibleMb = (int) LiquidUtil.toMb(Math.min(outputTank.getSpace(), inputTank.getFluidAmount()));
                heatbufferToUse = Math.min(heatbufferToUse,
                        (convertibleMb * property.huPerMB
                                - liquidHeatStored) / PARTS_LIQUID);
                heatbufferToUse = Math.min(heatbufferToUse, (maxkUBuffer - kUBuffer) / PARTS_KU);
                if (heatbufferToUse > 0) {
                    kUBuffer += heatbufferToUse * PARTS_KU * 4;
                    liquidHeatStored += heatbufferToUse * PARTS_LIQUID;
                    heatbuffer -= heatbufferToUse * PARTS_TOTAL;
                    newActive = true;
                }

                if (liquidHeatStored >= property.huPerMB) {
                    int mbToConvert = liquidHeatStored / property.huPerMB;
                    FluidVariant output = FluidVariant.of(property.outputFluid);
                    mbToConvert = (int) LiquidUtil.toMb(inputTank.drainInternal(LiquidUtil.mb(mbToConvert), false));
                    mbToConvert = (int) LiquidUtil.toMb(outputTank.fillInternal(output, LiquidUtil.mb(mbToConvert), false));
                    liquidHeatStored -= mbToConvert * property.huPerMB;
                    inputTank.drainInternal(LiquidUtil.mb(mbToConvert), true);
                    outputTank.fillInternal(output, LiquidUtil.mb(mbToConvert), true);
                }
            }
        }

        setActive(newActive);
        upgradeSlot.tick();
        markDirty();
    }

    /** Draws heat from the heat sources on every side but the front. */
    private int drawHu(int amount) {
        if (amount <= 0) {
            return 0;
        }

        int tmpAmount = amount;
        for (Direction dir : Direction.values()) {
            if (dir == getFacing()) {
                continue;
            }
            BlockEntity te = world.getBlockEntity(pos.offset(dir));
            if (te instanceof IHeatSource hs) {
                int request = hs.drawHeat(dir.getOpposite(), tmpAmount, true);
                if (request > 0) {
                    tmpAmount -= hs.drawHeat(dir.getOpposite(), request, false);
                    if (tmpAmount <= 0) {
                        break;
                    }
                }
            }
        }
        return amount - tmpAmount;
    }

    public int getHeatBuffer() {
        return heatbuffer;
    }

    public int getKuBuffer() {
        return kUBuffer;
    }

    // ---------------------------------------------------------------- IKineticSource: out of the front

    @Override
    public int maxrequestkineticenergyTick(Direction directionFrom) {
        return Math.min(kUBuffer, getConnectionBandwidth(directionFrom));
    }

    @Override
    public int getConnectionBandwidth(Direction side) {
        return side != getFacing() ? 0 : maxkUBuffer;
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
        request = Math.min(request, kUBuffer);
        if (!simulate) {
            kUBuffer -= request;
        }
        return request;
    }

    // ---------------------------------------------------------------- upgrades

    @Override
    public Set<UpgradableProperty> getUpgradableProperties() {
        return EnumSet.of(UpgradableProperty.ItemConsuming, UpgradableProperty.ItemProducing,
                UpgradableProperty.FluidConsuming, UpgradableProperty.FluidProducing);
    }

    @Override
    public double getEnergy() {
        return 40.0;
    }

    @Override
    public boolean useEnergy(double amount) {
        return true;
    }

    // ---------------------------------------------------------------- saving

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.readNbt(nbt, lookup);
        heatbuffer = nbt.getInt("heatbuffer");
        // 1.12.2 read "kubuffer" but wrote "kUBuffer", so the buffer was lost on every load
        kUBuffer = nbt.contains("kUBuffer") ? nbt.getInt("kUBuffer") : nbt.getInt("kubuffer");
        liquidHeatStored = nbt.getInt("liquidHeatStored");
    }

    @Override
    public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.writeNbt(nbt, lookup);
        nbt.putInt("heatbuffer", heatbuffer);
        nbt.putInt("kUBuffer", kUBuffer);
        nbt.putInt("liquidHeatStored", liquidHeatStored);
    }
}
