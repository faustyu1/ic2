package ic2.core.block.machine.tileentity;

import ic2.api.recipe.IRecipeInput;
import ic2.api.recipe.MachineRecipeResult;
import ic2.api.recipe.Recipes;
import ic2.api.upgrade.IUpgradableBlock;
import ic2.api.upgrade.UpgradableProperty;
import ic2.core.block.comp.Fluids;
import ic2.core.block.invslot.InvSlot;
import ic2.core.block.invslot.InvSlotConsumableLiquid;
import ic2.core.block.invslot.InvSlotConsumableLiquidByTank;
import ic2.core.block.invslot.InvSlotDischarge;
import ic2.core.block.invslot.InvSlotOutput;
import ic2.core.block.invslot.InvSlotProcessable;
import ic2.core.block.invslot.InvSlotUpgrade;
import ic2.core.init.IC2Blocks;
import ic2.core.init.IC2Items;
import ic2.core.init.MainConfig;
import ic2.core.recipe.MatterAmplifierRecipeManager;
import ic2.core.ref.FluidName;
import ic2.core.ref.TeBlock;
import ic2.core.util.ConfigUtil;
import ic2.core.util.LiquidUtil;
import java.util.EnumSet;
import java.util.Set;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;

/**
 * Fabric port of TileEntityMatter - the matter fabricator.
 *
 * <p>Every 1 000 000 EU (times {@code balance/uuEnergyFactor}) it fills 1 mB of UU-matter into its
 * 8000 mB tank, which cells take or pipes drain. Amplifier - scrap (5000) or scrap boxes (45 000) -
 * multiplies what it gets: each EU coming in while it has amplifier counts five more times, using
 * up as much amplifier. A redstone signal stops it taking energy. Tier 3, transformer upgrades.
 */
public class TileEntityMatter extends TileEntityElectricMachine implements IUpgradableBlock, InvSlotUpgrade.Listener {
    public int scrap;
    private double lastEnergy;
    public final InvSlotUpgrade upgradeSlot;
    public final InvSlotProcessable<IRecipeInput, Integer, ItemStack> amplifierSlot;
    public final InvSlotOutput outputSlot;
    public final InvSlotConsumableLiquid containerslot;
    public final InvSlotDischarge dischargeSlot;
    public final Fluids.InternalFluidTank fluidTank;

    public TileEntityMatter(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type == null ? IC2Blocks.MATTER_BE : type, pos, state, TeBlock.matter_generator, energyPerUu(),
                ConfigUtil.getInt(MainConfig.get(), "balance/matterFabricatorTier"));
        this.upgradeSlot = new InvSlotUpgrade(this, "upgrade", 4);
        this.amplifierSlot = new InvSlotProcessable<>(this, "scrap", 1, Recipes.matterAmplifier);
        this.outputSlot = new InvSlotOutput(this, "output", 1);
        this.fluidTank = fluids().addTank("fluidTank", 8000, Fluids.fluidPredicate(FluidName.uu_matter.getInstance()));
        this.containerslot = new InvSlotConsumableLiquidByTank(this, "container", InvSlot.Access.I, 1, InvSlot.InvSide.TOP,
                InvSlotConsumableLiquid.OpType.Fill, fluidTank);
        this.dischargeSlot = new InvSlotDischarge(this, "discharge", tier);
    }

    public TileEntityMatter(BlockPos pos, BlockState state) {
        this(null, pos, state);
    }

    static int energyPerUu() {
        return (int) Math.round(1000000.0 * ConfigUtil.getDouble(MainConfig.get(), "balance/uuEnergyFactor"));
    }

    public static void init() {
        if (Recipes.matterAmplifier != null) {
            return;
        }
        Recipes.matterAmplifier = new MatterAmplifierRecipeManager();
        addAmplifier(new ItemStack(IC2Items.SCRAP), 1, 5000);
        addAmplifier(new ItemStack(IC2Items.SCRAP_BOX), 1, 45000);
    }

    public static void addAmplifier(ItemStack input, int amount, int amplification) {
        Recipes.matterAmplifier.addRecipe(Recipes.inputFactory.forStack(input, amount), amplification, null, false);
    }

    @Override
    public void onUpgradesChanged() {
        setTier(upgradeSlot.getTier(defaultTier));
    }

    protected boolean redstonePowered() {
        return world != null && upgradeSlot.getRedstoneInput(world.getReceivedRedstonePower(pos)) > 0;
    }

    /** A redstone signal turns the energy input off, as the 1.12.2 redstone component did. */
    @Override
    public double getDemandedEnergy() {
        return redstonePowered() ? 0.0 : super.getDemandedEnergy();
    }

    @Override
    public void tick() {
        if (world == null || world.isClient) {
            return;
        }

        boolean needsInvUpdate = upgradeSlot.tickNoMark();
        if (redstonePowered() || getEnergy() <= 0.0) {
            setActive(false);
            return;
        }

        drainDischargeSlot(dischargeSlot);
        if (scrap > 0) {
            double bonus = Math.min(scrap, getEnergy() - lastEnergy);
            if (bonus > 0.0) {
                energyStored += 5.0 * bonus;
                scrap = (int) (scrap - bonus);
            }
        }
        setActive(true);

        if (scrap < 10000) {
            MachineRecipeResult<IRecipeInput, Integer, ItemStack> recipe = amplifierSlot.process();
            if (recipe != null) {
                amplifierSlot.consume(recipe);
                scrap += recipe.getOutput();
            }
        }

        if (getEnergy() >= maxEnergy) {
            needsInvUpdate |= attemptGeneration();
        }
        needsInvUpdate |= containerslot.processFromTank(fluidTank, outputSlot);
        lastEnergy = getEnergy();
        if (needsInvUpdate) {
            markDirty();
        }
    }

    public boolean attemptGeneration() {
        if (fluidTank.getFluidAmount() + LiquidUtil.mb(1) > fluidTank.getCapacity()) {
            return false;
        }
        fluidTank.fillInternal(FluidName.uu_matter.getVariant(), LiquidUtil.mb(1), true);
        useEnergy(maxEnergy);
        return true;
    }

    public int getProgress() {
        return (int) Math.min(100.0 * getEnergy() / maxEnergy, 100.0);
    }

    @Override
    public double getGuiAmount(String name) {
        return switch (name) {
            case "progress" -> getProgress();
            case "scrap" -> scrap;
            default -> super.getGuiAmount(name);
        };
    }

    @Override
    public boolean getGuiCondition(String name) {
        return "scrap".equals(name) ? scrap > 0 : super.getGuiCondition(name);
    }

    @Override
    public Set<UpgradableProperty> getUpgradableProperties() {
        return EnumSet.of(UpgradableProperty.RedstoneSensitive, UpgradableProperty.Transformer,
                UpgradableProperty.ItemConsuming, UpgradableProperty.ItemProducing, UpgradableProperty.FluidProducing);
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.readNbt(nbt, lookup);
        scrap = nbt.getInt("scrap");
        lastEnergy = nbt.getDouble("lastEnergy");
    }

    @Override
    public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.writeNbt(nbt, lookup);
        nbt.putInt("scrap", scrap);
        nbt.putDouble("lastEnergy", lastEnergy);
    }
}
