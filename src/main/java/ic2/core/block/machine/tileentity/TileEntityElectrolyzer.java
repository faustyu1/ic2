package ic2.core.block.machine.tileentity;

import ic2.api.recipe.IElectrolyzerRecipeManager;
import ic2.api.recipe.Recipes;
import ic2.api.upgrade.IUpgradableBlock;
import ic2.api.upgrade.UpgradableProperty;
import ic2.core.block.comp.Fluids;
import ic2.core.block.invslot.InvSlotDischarge;
import ic2.core.block.invslot.InvSlotUpgrade;
import ic2.core.init.IC2Blocks;
import ic2.core.recipe.ElectrolyzerRecipeManager;
import ic2.core.ref.FluidName;
import ic2.core.ref.TeBlock;
import ic2.core.util.LiquidUtil;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraftforge.fluids.FluidStack;

/**
 * Fabric port of TileEntityElectrolyzer - splits fluids into their parts.
 *
 * <p>Water (40 mB, 32 EU a tick for 200 ticks) becomes 26 mB of hydrogen in the tank below it
 * and 13 mB of oxygen in the tank above. Each output goes into the IC2 tank on its side of the
 * recipe; without a tank there, or with no room in it, the electrolyzer waits. 8000 mB input tank,
 * 32 000 EU, tier 2.
 */
public class TileEntityElectrolyzer extends TileEntityElectricMachine implements IUpgradableBlock {
    protected int progress;
    protected IElectrolyzerRecipeManager.ElectrolyzerRecipe recipe;
    protected final Fluids.InternalFluidTank input;
    public final InvSlotUpgrade upgradeSlot;
    public final InvSlotDischarge dischargeSlot;

    public TileEntityElectrolyzer(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type == null ? IC2Blocks.ELECTROLYZER_BE : type, pos, state, TeBlock.electrolyzer, 32000, 2);
        this.input = fluids().addTankInsert("input", 8000, Fluids.fluidPredicate(Recipes.electrolyzer));
        this.upgradeSlot = new InvSlotUpgrade(this, "upgradeSlot", 4);
        this.dischargeSlot = new InvSlotDischarge(this, "discharge", 2);
    }

    public TileEntityElectrolyzer(BlockPos pos, BlockState state) {
        this(null, pos, state);
    }

    public static void init() {
        if (Recipes.electrolyzer != null) {
            return;
        }
        Recipes.electrolyzer = new ElectrolyzerRecipeManager();
        Recipes.electrolyzer.addRecipe("water", 40, 32,
                new IElectrolyzerRecipeManager.ElectrolyzerOutput(FluidName.hydrogen.getName(), 26, Direction.DOWN),
                new IElectrolyzerRecipeManager.ElectrolyzerOutput(FluidName.oxygen.getName(), 13, Direction.UP));
    }

    @Override
    public void tick() {
        if (world == null || world.isClient) {
            return;
        }

        drainDischargeSlot(dischargeSlot);
        boolean needsInvUpdate = false;
        if (canOperate()) {
            setActive(true);
            useEnergy(recipe.EUaTick);
            progress++;
            if (progress >= recipe.ticksNeeded) {
                operate();
                progress = 0;
                needsInvUpdate = true;
            }
        } else {
            setActive(false);
            progress = 0;
        }

        needsInvUpdate |= upgradeSlot.tickNoMark();
        if (needsInvUpdate) {
            markDirty();
        }
    }

    protected boolean canOperate() {
        if (input.getFluid() == null || Recipes.electrolyzer == null) {
            return false;
        }
        recipe = Recipes.electrolyzer.getElectrolysisInformation(input.getFluid().getFluid());
        if (recipe == null || getEnergy() < recipe.EUaTick || input.getFluidAmount() < recipe.inputAmount) {
            return false;
        }
        for (IElectrolyzerRecipeManager.ElectrolyzerOutput output : recipe.outputs) {
            if (!canFillTank(output.tankDirection, output.getOutput())) {
                return false;
            }
        }
        return true;
    }

    protected void operate() {
        input.drainInternal(recipe.inputAmount, true);
        for (IElectrolyzerRecipeManager.ElectrolyzerOutput output : recipe.outputs) {
            fillTank(output.tankDirection, output.getOutput());
        }
    }

    protected boolean canFillTank(Direction facing, FluidStack fluid) {
        BlockEntity te = world.getBlockEntity(pos.offset(facing));
        return fluid != null && te instanceof TileEntityTank && LiquidUtil.fillTile(te, facing, fluid, true) == fluid.amount;
    }

    protected void fillTank(Direction facing, FluidStack fluid) {
        BlockEntity te = world.getBlockEntity(pos.offset(facing));
        if (te instanceof TileEntityTank) {
            LiquidUtil.fillTile(te, facing, fluid, false);
        }
    }

    public Fluids.InternalFluidTank getInput() {
        return input;
    }

    public boolean hasRecipe() {
        return recipe != null;
    }

    public IElectrolyzerRecipeManager.ElectrolyzerRecipe getCurrentRecipe() {
        return recipe;
    }

    /** The recipe of the fluid in the tank, for the window even while the machine is idle. */
    private IElectrolyzerRecipeManager.ElectrolyzerRecipe shownRecipe() {
        if (input.getFluid() == null || Recipes.electrolyzer == null) {
            return null;
        }
        return Recipes.electrolyzer.getElectrolysisInformation(input.getFluid().getFluid());
    }

    /**
     * The window shows the outputs of the recipe as five fluid slots, {@code out0} to {@code out4};
     * they are read-only views made up here, not tanks of the machine.
     */
    @Override
    public Fluids.InternalFluidTank getFluidTank(String name) {
        if (name.startsWith("out") && name.length() == 4) {
            IElectrolyzerRecipeManager.ElectrolyzerRecipe shown = shownRecipe();
            int index = name.charAt(3) - '0';
            Fluids.InternalFluidTank view = new Fluids.InternalFluidTank(name, 1000, Collections.emptySet(), Collections.emptySet(), fluid -> false);
            if (shown != null && index >= 0 && index < shown.outputs.length) {
                FluidStack output = shown.outputs[index].getOutput();
                if (output != null) {
                    view.setFluid(output);
                }
            }
            return view;
        }
        return super.getFluidTank(name);
    }

    @Override
    public double getGuiValue(String name) {
        if ("progress".equals(name)) {
            return recipe == null ? 0.0 : (double) progress / recipe.ticksNeeded;
        }
        return super.getGuiValue(name);
    }

    @Override
    public boolean getGuiCondition(String name) {
        if (name.startsWith("outputs")) {
            IElectrolyzerRecipeManager.ElectrolyzerRecipe shown = shownRecipe();
            return shown != null && shown.outputs.length == name.charAt(7) - '0';
        }
        return super.getGuiCondition(name);
    }

    @Override
    public Set<UpgradableProperty> getUpgradableProperties() {
        return EnumSet.of(UpgradableProperty.FluidConsuming);
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.readNbt(nbt, lookup);
        progress = nbt.getInt("progress");
    }

    @Override
    public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.writeNbt(nbt, lookup);
        nbt.putInt("progress", progress);
    }
}
