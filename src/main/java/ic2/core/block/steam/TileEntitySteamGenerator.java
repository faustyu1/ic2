package ic2.core.block.steam;

import ic2.api.energy.tile.IHeatSource;
import ic2.core.ExplosionIC2;
import ic2.core.block.TileEntityInventory;
import ic2.core.block.comp.Fluids;
import ic2.core.init.IC2Blocks;
import ic2.core.ref.FluidName;
import ic2.core.ref.TeBlock;
import ic2.core.util.BiomeUtil;
import ic2.core.util.LiquidUtil;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * Fabric port of TileEntitySteamGenerator - the steam boiler.
 *
 * <p>It draws up to 1200 HU a tick from the heat sources around it. Each HU warms the boiler by
 * 0.0005 degrees; the target temperature is 100 C plus 2.74 C per 2.2 bar of the pressure valve
 * (0-300). Below 100 C at zero pressure it only passes the water on; once hot it boils the set
 * amount of water a tick (0-1000 mB), 100 + pressure/2.2 HU per mB, into 100 mB of steam each -
 * superheated steam from 374 C on - which it pushes into its neighbours. Water it cannot use cools
 * it by 0.1 C a mB. Steam with nowhere to go blows the boiler up one time in ten, otherwise most
 * of it condenses back. Over 500 C the boiler explodes. Ordinary water calcifies it a mB at a time;
 * at 100 000 it stops for good, distilled water does not calcify.
 */
public class TileEntitySteamGenerator extends TileEntityInventory {
    private static final float MAX_HEAT = 500.0F;
    private static final float HEAT_PER_HU = 5.0E-4F;
    private static final float COOLING_PER_MB = 0.1F;
    private static final int MAX_HU_INPUT = 1200;
    private static final int MAX_CALCIFICATION = 100000;
    private static final int STEAM_EXPANSION = 100;
    private static final float EPSILON = 1.0E-4F;

    private enum OutputType {
        NONE(""),
        WATER("ic2.SteamGenerator.output.water"),
        DISTILLEDWATER("ic2.SteamGenerator.output.destiwater"),
        STEAM("ic2.SteamGenerator.output.steam"),
        SUPERHEATEDSTEAM("ic2.SteamGenerator.output.hotsteam");

        private final String name;

        OutputType(String name) {
            this.name = name;
        }
    }

    public final Fluids.InternalFluidTank waterTank;
    private int heatInput;
    private int inputMB;
    private int calcification;
    private int outputMB;
    private OutputType outputFluid = OutputType.NONE;
    private float systemHeat;
    private int pressure;

    public TileEntitySteamGenerator(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type == null ? IC2Blocks.STEAM_GENERATOR_BE : type, pos, state, TeBlock.steam_generator);
        this.waterTank = fluids().addTankInsert("waterTank", 10000,
                fluid -> fluid == LiquidUtil.getFluid("water") || fluid == FluidName.distilled_water.getInstance());
    }

    public TileEntitySteamGenerator(BlockPos pos, BlockState state) {
        this(null, pos, state);
    }

    @Override
    public void tick() {
        if (world == null || world.isClient) {
            return;
        }

        systemHeat = Math.max(systemHeat, BiomeUtil.getBiomeTemperature(world, pos));
        if (isCalcified()) {
            setActive(false);
        } else {
            setActive(work());
        }
        if (isRemoved()) {
            return;
        }
        if (!getActive()) {
            cooldown(0.01F);
        }
        markDirty();
    }

    private boolean work() {
        heatInput = requestHeat(MAX_HU_INPUT);
        if (heatInput <= 0) {
            return false;
        }

        outputMB = 0;
        outputFluid = OutputType.NONE;
        if (waterTank.getFluidAmount() < LiquidUtil.mb(1) || inputMB <= 0) {
            heatup(heatInput);
            return true;
        }

        Fluid inputFluid = waterTank.getFluid().getFluid();
        boolean hasDistilledWater = inputFluid == FluidName.distilled_water.getInstance();
        int maxAmount = (int) Math.min(inputMB, LiquidUtil.toMb(waterTank.getFluidAmount()));
        float hUneeded = 100.0F + pressure / 220.0F * 100.0F;
        float targetTemp = 100.0F + pressure / 220.0F * 100.0F * 2.74F;
        float reqHeat = targetTemp - systemHeat;
        float remainingHuInput = heatInput;
        if (reqHeat > EPSILON) {
            int heatReq = (int) Math.ceil(reqHeat / HEAT_PER_HU);
            if (heatInput <= heatReq) {
                heatup(heatInput);
                if (pressure == 0 && systemHeat < 99.9999F) {
                    // not boiling yet: the water just passes through
                    outputMB = maxAmount;
                    outputFluid = hasDistilledWater ? OutputType.DISTILLEDWATER : OutputType.WATER;
                    long transferred = LiquidUtil.distribute(this, FluidVariant.of(inputFluid), LiquidUtil.mb(maxAmount), false);
                    if (transferred > 0) {
                        waterTank.drainInternal(transferred, true);
                    }
                }
                return true;
            }

            heatup(heatReq);
            remainingHuInput -= heatReq;
            reqHeat = targetTemp - systemHeat;
        }

        float availableSystemHu = Math.min(-reqHeat / HEAT_PER_HU, MAX_HU_INPUT - heatInput);
        int activeAmount = Math.min(maxAmount, (int) ((remainingHuInput + availableSystemHu) / hUneeded));
        int totalAmount = activeAmount;
        remainingHuInput -= activeAmount * hUneeded;
        if (remainingHuInput < 0.0F) {
            cooldown(-remainingHuInput * HEAT_PER_HU);
            reqHeat = targetTemp - systemHeat;
        }

        if (reqHeat <= -0.1001F) {
            int coolingAmount = Math.min(maxAmount, (int) (-reqHeat / COOLING_PER_MB));
            coolingAmount = Math.min(coolingAmount, 20);
            cooldown(coolingAmount * COOLING_PER_MB);
            totalAmount = Math.max(activeAmount, coolingAmount);
        }

        if (remainingHuInput > 0.0F) {
            heatup(remainingHuInput);
        }
        if (totalAmount <= 0 || isRemoved()) {
            return true;
        }

        if (!hasDistilledWater) {
            calcification += totalAmount;
        }
        waterTank.drainInternal(LiquidUtil.mb(totalAmount), true);
        if (activeAmount <= 0) {
            return true;
        }

        outputMB = activeAmount * STEAM_EXPANSION;
        Fluid output;
        if (systemHeat >= 373.9999F) {
            output = FluidName.superheated_steam.getInstance();
            outputFluid = OutputType.SUPERHEATEDSTEAM;
        } else {
            output = FluidName.steam.getInstance();
            outputFluid = OutputType.STEAM;
        }

        int transferred = (int) LiquidUtil.toMb(LiquidUtil.distribute(this, FluidVariant.of(output), LiquidUtil.mb(outputMB), false));
        int remaining = outputMB - transferred;
        if (remaining > 0) {
            if (world.random.nextInt(10) == 0) {
                new ExplosionIC2(world, null, pos, 1, 1.0F, ExplosionIC2.Type.Heat).doExplosion();
            } else if (remaining >= STEAM_EXPANSION) {
                waterTank.fillInternal(FluidVariant.of(inputFluid), LiquidUtil.mb(remaining / STEAM_EXPANSION), true);
            }
        }
        return true;
    }

    private void heatup(float heatinput) {
        systemHeat += heatinput * HEAT_PER_HU;
        if (systemHeat > MAX_HEAT) {
            world.removeBlock(pos, false);
            new ExplosionIC2(world, null, pos, 10, 0.01F, ExplosionIC2.Type.Heat).doExplosion();
        }
    }

    private void cooldown(float cool) {
        systemHeat = Math.max(systemHeat - cool, BiomeUtil.getBiomeTemperature(world, pos));
    }

    private int requestHeat(int requestHeat) {
        int targetHeat = requestHeat;
        for (Direction dir : Direction.values()) {
            BlockEntity target = world.getBlockEntity(pos.offset(dir));
            if (target instanceof IHeatSource hs) {
                int request = hs.drawHeat(dir.getOpposite(), targetHeat, true);
                if (request > 0) {
                    targetHeat -= hs.drawHeat(dir.getOpposite(), request, false);
                    if (targetHeat == 0) {
                        return requestHeat;
                    }
                }
            }
        }
        return requestHeat - targetHeat;
    }

    /** The window's buttons: {@code input:<delta>} for the water, {@code pressure:<delta>} for the valve. */
    @Override
    public void onGuiEvent(PlayerEntity player, String event) {
        int colon = event.indexOf(':');
        if (colon < 0) {
            return;
        }
        int delta;
        try {
            delta = Integer.parseInt(event.substring(colon + 1));
        } catch (NumberFormatException e) {
            return;
        }
        if (event.startsWith("input:")) {
            setInputMB(inputMB + delta);
        } else if (event.startsWith("pressure:")) {
            setPressure(pressure + delta);
        }
    }

    public void setInputMB(int inputMB) {
        this.inputMB = Math.max(Math.min(inputMB, 1000), 0);
        markDirty();
    }

    public void setPressure(int pressure) {
        this.pressure = Math.max(Math.min(pressure, 300), 0);
        markDirty();
    }

    public int getOutputMB() {
        return outputMB;
    }

    public int getInputMB() {
        return inputMB;
    }

    public int getHeatInput() {
        return heatInput;
    }

    public int getPressure() {
        return pressure;
    }

    public float getSystemHeat() {
        return Math.round(systemHeat * 10.0F) / 10.0F;
    }

    public float getCalcification() {
        return Math.round((float) calcification / MAX_CALCIFICATION * 100.0F * 100.0F) / 100.0F;
    }

    public boolean isCalcified() {
        return calcification >= MAX_CALCIFICATION;
    }

    public String getOutputFluidName() {
        return outputFluid.name;
    }

    @Override
    public double getGuiValue(String name) {
        return switch (name) {
            case "heat" -> systemHeat / MAX_HEAT;
            case "calcification" -> (double) calcification / MAX_CALCIFICATION;
            default -> super.getGuiValue(name);
        };
    }

    @Override
    public double getGuiAmount(String name) {
        return switch (name) {
            case "heat" -> getSystemHeat();
            case "calcification" -> getCalcification();
            case "inputMB" -> inputMB;
            case "outputMB" -> outputMB;
            case "heatInput" -> heatInput;
            case "pressure" -> pressure;
            default -> super.getGuiAmount(name);
        };
    }

    @Override
    public boolean getGuiCondition(String name) {
        return switch (name) {
            case "outWater" -> outputFluid == OutputType.WATER;
            case "outDistilled" -> outputFluid == OutputType.DISTILLEDWATER;
            case "outSteam" -> outputFluid == OutputType.STEAM;
            case "outSuperheated" -> outputFluid == OutputType.SUPERHEATEDSTEAM;
            default -> super.getGuiCondition(name);
        };
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.readNbt(nbt, lookup);
        inputMB = nbt.getInt("inputmb");
        pressure = nbt.getInt("pressurevalve");
        systemHeat = nbt.getFloat("systemheat");
        calcification = nbt.getInt("calcification");
    }

    @Override
    public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.writeNbt(nbt, lookup);
        nbt.putInt("inputmb", inputMB);
        nbt.putInt("pressurevalve", pressure);
        nbt.putFloat("systemheat", systemHeat);
        nbt.putInt("calcification", calcification);
    }
}
