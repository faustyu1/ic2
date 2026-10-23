package ic2.core.block.kineticgenerator.tileentity;

import ic2.api.energy.tile.IKineticSource;
import ic2.api.upgrade.IUpgradableBlock;
import ic2.api.upgrade.UpgradableProperty;
import ic2.core.ExplosionIC2;
import ic2.core.IC2;
import ic2.core.block.TileEntityInventory;
import ic2.core.block.comp.Fluids;
import ic2.core.block.invslot.InvSlotConsumable;
import ic2.core.block.invslot.InvSlotConsumableItemStack;
import ic2.core.block.invslot.InvSlotUpgrade;
import ic2.core.block.machine.tileentity.TileEntityCondenser;
import ic2.core.init.IC2Blocks;
import ic2.core.init.IC2ItemVariants;
import ic2.core.init.MainConfig;
import ic2.core.ref.FluidName;
import ic2.core.ref.TeBlock;
import ic2.core.util.ConfigUtil;
import ic2.core.util.LiquidUtil;
import java.util.EnumSet;
import java.util.Set;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

/**
 * Fabric port of TileEntitySteamKineticGenerator - the steam turbine.
 *
 * <p>With a turbine in its slot it takes all the steam in its 21 000 mB tank every tick and gives
 * 2 kU per mB of steam, 4 per mB of superheated steam (times {@code balance/energy/kineticgenerator/steam}),
 * out of its front. The used steam goes on into condensers next to it - superheated steam, which
 * leaves as plain steam, into a further turbine too; steam with nowhere to go is vented, and now
 * and then that blows up. Plain steam condenses inside: a tenth of it adds to the condensation,
 * every 100 of which becomes one mB of distilled water in its 1000 mB tank. Water in the tank
 * throttles the turbine by its share of the tank, a full tank stops it. Every second of work wears
 * the turbine, twice as fast on plain steam.
 */
public class TileEntitySteamKineticGenerator extends TileEntityInventory implements IKineticSource, IUpgradableBlock {
    protected final Fluids.InternalFluidTank steamTank;
    protected final Fluids.InternalFluidTank distilledWaterTank;
    public final InvSlotUpgrade upgradeSlot;
    public final InvSlotConsumable turbineSlot;
    private int kUoutput;
    private boolean ventingSteam;
    private boolean throttled;
    private boolean isTurbineFilledWithWater;
    private int condensationProgress;
    private int updateTicker = IC2.random.nextInt(getTickRate());

    public TileEntitySteamKineticGenerator(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type == null ? IC2Blocks.STEAM_KINETIC_BE : type, pos, state, TeBlock.steam_kinetic_generator);
        this.upgradeSlot = new InvSlotUpgrade(this, "upgrade", 1);
        Item turbine = IC2ItemVariants.get("crafting", "steam_turbine");
        this.turbineSlot = new InvSlotConsumableItemStack(this, "Turbineslot", 1, new ItemStack(turbine == null ? Items.AIR : turbine));
        this.turbineSlot.setStackSizeLimit(1);
        this.steamTank = fluids().addTankInsert("steamTank", 21000,
                Fluids.fluidPredicate(FluidName.steam.getInstance(), FluidName.superheated_steam.getInstance()));
        this.distilledWaterTank = fluids().addTank("distilledWaterTank", 1000,
                fluid -> fluid == FluidName.distilled_water.getInstance() || fluid == FluidRegistry.getFluid("water"));
    }

    public TileEntitySteamKineticGenerator(BlockPos pos, BlockState state) {
        this(null, pos, state);
    }

    private static float outputModifier() {
        return (float) ConfigUtil.getDouble(MainConfig.get(), "balance/energy/kineticgenerator/steam");
    }

    @Override
    public void tick() {
        if (world == null || world.isClient) {
            return;
        }

        boolean needsInvUpdate = false;
        if (distilledWaterTank.getCapacity() - distilledWaterTank.getFluidAmount() >= 1 && isTurbineFilledWithWater) {
            isTurbineFilledWithWater = false;
        }

        if (steamTank.getFluidAmount() > 0 && !isTurbineFilledWithWater && !turbineSlot.isEmpty()) {
            if (!getActive()) {
                setActive(true);
                needsInvUpdate = true;
            }

            boolean hotSteam = steamTank.getFluid().getFluid() == FluidName.superheated_steam.getInstance();
            boolean turbineDoneWork = turbineDoWork(hotSteam);
            if (updateTicker++ >= getTickRate()) {
                if (turbineDoneWork) {
                    turbineSlot.damage(hotSteam ? 1 : 2, false);
                }
                updateTicker = 0;
            }
            needsInvUpdate = true;
        } else if (getActive()) {
            setActive(false);
            needsInvUpdate = true;
            kUoutput = 0;
        }

        needsInvUpdate |= upgradeSlot.tickNoMark();
        if (needsInvUpdate) {
            markDirty();
        }
    }

    private int handleSteam(boolean hotSteam) {
        int amount = steamTank.getFluidAmount();
        steamTank.drainInternal(amount, true);
        int kuWorkBuffer = amount * 2 * (hotSteam ? 2 : 1);
        if (hotSteam) {
            outputSteam(amount, true);
        } else {
            int condensation = amount / 10;
            condensationProgress += condensation;
            outputSteam(amount - condensation, false);
        }
        return kuWorkBuffer;
    }

    private boolean turbineDoWork(boolean hotSteam) {
        int rawOutput = handleSteam(hotSteam);
        int waterAmount = distilledWaterTank.getFluidAmount();
        float throttle;
        if (waterAmount == 0) {
            throttled = false;
            throttle = 1.0F;
        } else {
            throttled = true;
            throttle = 1.0F - (float) waterAmount / distilledWaterTank.getCapacity();
        }

        kUoutput = (int) (rawOutput * throttle * outputModifier());
        if (condensationProgress >= 100) {
            FluidStack drop = new FluidStack(FluidName.distilled_water.getInstance(), 1);
            if (distilledWaterTank.fillInternal(drop, false) == 1) {
                condensationProgress -= 100;
                distilledWaterTank.fillInternal(drop, true);
            } else {
                isTurbineFilledWithWater = true;
            }
        }
        return kUoutput > 0;
    }

    /** The used steam goes into condensers, or further turbines when it was superheated; the rest is vented. */
    private void outputSteam(int amount, boolean hotSteam) {
        for (Direction dir : Direction.values()) {
            BlockEntity te = world.getBlockEntity(pos.offset(dir));
            if (te instanceof TileEntityCondenser || hotSteam && te instanceof TileEntitySteamKineticGenerator) {
                int transAmount = LiquidUtil.fillTile(te, dir.getOpposite(), new FluidStack(FluidName.steam.getInstance(), amount), false);
                if (transAmount > 0) {
                    amount -= transAmount;
                    if (amount <= 0) {
                        break;
                    }
                }
            }
        }

        if (amount > 0) {
            ventingSteam = true;
            if (world.random.nextInt(10) == 0) {
                new ExplosionIC2(world, null, pos, 1, 1.0F, ExplosionIC2.Type.Heat).doExplosion();
            }
        } else {
            ventingSteam = false;
        }
    }

    public int getKUoutput() {
        return kUoutput;
    }

    public Fluids.InternalFluidTank getDistilledWaterTank() {
        return distilledWaterTank;
    }

    public Fluids.InternalFluidTank getSteamTank() {
        return steamTank;
    }

    public boolean hasTurbine() {
        return !turbineSlot.isEmpty();
    }

    public boolean isVentingSteam() {
        return ventingSteam;
    }

    public boolean isThrottled() {
        return throttled;
    }

    public boolean isTurbineBlockedByWater() {
        return isTurbineFilledWithWater;
    }

    public int getTickRate() {
        return 20;
    }

    @Override
    public double getGuiAmount(String name) {
        return "output".equals(name) ? kUoutput : super.getGuiAmount(name);
    }

    @Override
    public boolean getGuiCondition(String name) {
        return switch (name) {
            case "hasTurbine" -> hasTurbine();
            case "venting" -> hasTurbine() && ventingSteam;
            case "throttled" -> hasTurbine() && throttled;
            case "blocked" -> isTurbineFilledWithWater;
            case "active" -> getActive();
            default -> super.getGuiCondition(name);
        };
    }

    // ---------------------------------------------------------------- IKineticSource: out of the front

    @Override
    public int maxrequestkineticenergyTick(Direction directionFrom) {
        return getConnectionBandwidth(directionFrom);
    }

    @Override
    public int getConnectionBandwidth(Direction side) {
        return side == getFacing() ? kUoutput : 0;
    }

    @Override
    public int requestkineticenergy(Direction directionFrom, int amount) {
        return drawKineticEnergy(directionFrom, amount, false);
    }

    @Override
    public int drawKineticEnergy(Direction side, int request, boolean simulate) {
        // as in 1.12.2, the turbine's whole output each time it is asked
        return side == getFacing() ? kUoutput : 0;
    }

    // ---------------------------------------------------------------- upgrades

    @Override
    public Set<UpgradableProperty> getUpgradableProperties() {
        return EnumSet.of(UpgradableProperty.ItemConsuming, UpgradableProperty.FluidConsuming, UpgradableProperty.FluidProducing);
    }

    @Override
    public double getEnergy() {
        return 0.0;
    }

    @Override
    public boolean useEnergy(double amount) {
        return false;
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.readNbt(nbt, lookup);
        condensationProgress = nbt.getInt("condensationprogress");
    }

    @Override
    public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.writeNbt(nbt, lookup);
        nbt.putInt("condensationprogress", condensationProgress);
    }
}
