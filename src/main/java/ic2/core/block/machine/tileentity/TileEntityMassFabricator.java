package ic2.core.block.machine.tileentity;

import ic2.api.energy.EnergyNet;
import ic2.api.recipe.IRecipeInput;
import ic2.api.recipe.MachineRecipeResult;
import ic2.api.recipe.Recipes;
import ic2.api.upgrade.IUpgradableBlock;
import ic2.api.upgrade.UpgradableProperty;
import ic2.core.block.invslot.InvSlotDischarge;
import ic2.core.block.invslot.InvSlotOutput;
import ic2.core.block.invslot.InvSlotProcessable;
import ic2.core.block.invslot.InvSlotUpgrade;
import ic2.core.init.IC2Blocks;
import ic2.core.init.IC2ItemVariants;
import ic2.core.init.MainConfig;
import ic2.core.ref.TeBlock;
import ic2.core.util.ConfigUtil;
import java.util.EnumSet;
import java.util.Set;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;

/**
 * Fabric port of TileEntityMassFabricator - the older mass fabricator, which makes UU-matter as
 * an item.
 *
 * <p>A full buffer of 1 000 000 EU makes one UU-matter, but only once it has also burnt the same
 * amount of scrap value: amplifier gives ten times its amplification as scrap value, and each tick
 * it burns as much of it as the buffer holds above what was already burnt, at most the power of
 * its tier. A redstone signal stops it taking energy.
 */
public class TileEntityMassFabricator extends TileEntityElectricMachine implements IUpgradableBlock, InvSlotUpgrade.Listener {
    private static final int SCRAP_FACTOR = 10;

    public int scrap;
    public int consumedScrap;
    protected double maxScrapConsumption;
    public final InvSlotProcessable<IRecipeInput, Integer, ItemStack> amplifierSlot;
    public final InvSlotOutput outputSlot;
    public final InvSlotUpgrade upgradeSlot;
    public final InvSlotDischarge dischargeSlot;

    public TileEntityMassFabricator(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type == null ? IC2Blocks.MASS_FABRICATOR_BE : type, pos, state, TeBlock.mass_fabricator, TileEntityMatter.energyPerUu(),
                ConfigUtil.getInt(MainConfig.get(), "balance/massFabricatorTier"));
        this.maxScrapConsumption = EnergyNet.instance == null ? 128 : EnergyNet.instance.getPowerFromTier(defaultTier);
        this.amplifierSlot = new InvSlotProcessable<>(this, "scrap", 1, Recipes.matterAmplifier);
        this.outputSlot = new InvSlotOutput(this, "output", 1);
        this.upgradeSlot = new InvSlotUpgrade(this, "upgrade", 4);
        this.dischargeSlot = new InvSlotDischarge(this, "discharge", tier);
    }

    public TileEntityMassFabricator(BlockPos pos, BlockState state) {
        this(null, pos, state);
    }

    private static int requiredScrap() {
        return TileEntityMatter.energyPerUu();
    }

    @Override
    public void onUpgradesChanged() {
        int newTier = upgradeSlot.getTier(defaultTier);
        setTier(newTier);
        if (EnergyNet.instance != null) {
            maxScrapConsumption = EnergyNet.instance.getPowerFromTier(newTier);
        }
    }

    protected boolean redstonePowered() {
        return world != null && upgradeSlot.getRedstoneInput(world.getReceivedRedstonePower(pos)) > 0;
    }

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
        if (scrap < 100000) {
            MachineRecipeResult<IRecipeInput, Integer, ItemStack> recipe = amplifierSlot.process();
            if (recipe != null) {
                amplifierSlot.consume(recipe);
                scrap += recipe.getOutput() * SCRAP_FACTOR;
            }
        }

        double scrapConversion = Math.min(Math.min(scrap, getEnergy() - consumedScrap), maxScrapConsumption);
        boolean newActivity = false;
        if (scrapConversion > 0.0) {
            consumedScrap = (int) (consumedScrap + scrapConversion);
            scrap = (int) (scrap - scrapConversion);
            newActivity = true;
            if (getEnergy() >= maxEnergy && consumedScrap >= requiredScrap()) {
                ItemStack matter = new ItemStack(IC2ItemVariants.get("misc_resource", "matter"));
                if (outputSlot.canAdd(matter)) {
                    outputSlot.add(matter);
                    useEnergy(maxEnergy);
                    consumedScrap = 0;
                    needsInvUpdate = true;
                } else {
                    newActivity = false;
                }
            }
        }
        setActive(newActivity);
        if (needsInvUpdate) {
            markDirty();
        }
    }

    public int getScrap() {
        return scrap / SCRAP_FACTOR;
    }

    public int getScrapProgress() {
        return (int) Math.min(100.0F * ((float) consumedScrap / requiredScrap()), 100.0F);
    }

    public int getEnergyProgress() {
        return (int) Math.min(100.0 * getEnergy() / maxEnergy, 100.0);
    }

    @Override
    public double getGuiAmount(String name) {
        return switch (name) {
            case "energyProgress" -> getEnergyProgress();
            case "scrapProgress" -> getScrapProgress();
            case "scrap" -> getScrap();
            default -> super.getGuiAmount(name);
        };
    }

    @Override
    public boolean getGuiCondition(String name) {
        return switch (name) {
            case "scrap" -> scrap > 0;
            case "dev" -> false;
            default -> super.getGuiCondition(name);
        };
    }

    @Override
    public Set<UpgradableProperty> getUpgradableProperties() {
        return EnumSet.of(UpgradableProperty.RedstoneSensitive, UpgradableProperty.Transformer,
                UpgradableProperty.ItemConsuming, UpgradableProperty.ItemProducing);
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.readNbt(nbt, lookup);
        scrap = nbt.getInt("scrap");
        consumedScrap = nbt.getInt("consumedScrap");
    }

    @Override
    public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.writeNbt(nbt, lookup);
        nbt.putInt("scrap", scrap);
        nbt.putInt("consumedScrap", consumedScrap);
    }
}
