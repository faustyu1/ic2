package ic2.core.block.machine.tileentity;

import ic2.api.energy.tile.IHeatSource;
import ic2.api.recipe.IRecipeInput;
import ic2.api.recipe.MachineRecipeResult;
import ic2.api.recipe.Recipes;
import ic2.api.upgrade.IUpgradableBlock;
import ic2.api.upgrade.UpgradableProperty;
import ic2.core.block.TileEntityInventory;
import ic2.core.block.comp.Fluids;
import ic2.core.block.invslot.InvSlot;
import ic2.core.block.invslot.InvSlotConsumableLiquid;
import ic2.core.block.invslot.InvSlotConsumableLiquidByTank;
import ic2.core.block.invslot.InvSlotOutput;
import ic2.core.block.invslot.InvSlotProcessable;
import ic2.core.block.invslot.InvSlotUpgrade;
import ic2.core.init.IC2Blocks;
import ic2.core.recipe.BasicMachineRecipeManager;
import ic2.core.ref.FluidName;
import ic2.core.ref.TeBlock;
import ic2.core.util.LiquidUtil;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * Fabric port of TileEntityBlastFurnace - the steel furnace.
 *
 * <p>It runs on heat, not EU: whatever heat source faces its front (a heat generator, say) is
 * asked for all it can give while there is something to smelt, or while a redstone signal holds
 * it ready, and it has to reach 50000 heat before it works. Without heat coming in it cools by
 * one a tick. Hot, it turns iron (ingots, ore, dust, crushed ore) into steel and slag, taking one
 * millibucket of compressed air from its tank every tick of the 6000 the recipes ask for; the air
 * comes from cells put in its cell slot.
 */
public class TileEntityBlastFurnace extends TileEntityInventory implements IUpgradableBlock {
    public static int maxHeat = 50000;

    public int heat = 0;
    protected int progress = 0;
    protected int progressNeeded = 300;
    protected float guiProgress;
    public float guiHeat;

    public final Fluids.InternalFluidTank fluidTank;
    public final InvSlotProcessable<IRecipeInput, Collection<ItemStack>, ItemStack> inputSlot;
    public final InvSlotOutput outputSlot;
    public final InvSlotConsumableLiquid tankInputSlot;
    public final InvSlotOutput tankOutputSlot;
    public final InvSlotUpgrade upgradeSlot;

    public TileEntityBlastFurnace(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type == null ? IC2Blocks.BLAST_FURNACE_BE : type, pos, state, TeBlock.blast_furnace);
        if (Recipes.blastfurnace == null) {
            init();
        }

        this.inputSlot = new InvSlotProcessable<>(this, "input", 1, Recipes.blastfurnace);
        this.outputSlot = new InvSlotOutput(this, "output", 2);
        this.fluidTank = fluids().addTankInsert("fluid", 8000, Fluids.fluidPredicate(FluidName.air.getInstance()));
        this.tankInputSlot = new InvSlotConsumableLiquidByTank(this, "cellInput", InvSlot.Access.I, 1,
                InvSlot.InvSide.ANY, InvSlotConsumableLiquid.OpType.Drain, fluidTank);
        this.tankOutputSlot = new InvSlotOutput(this, "cellOutput", 1);
        this.upgradeSlot = new InvSlotUpgrade(this, "upgrade", 2);
    }

    public TileEntityBlastFurnace(BlockPos pos, BlockState state) {
        this(IC2Blocks.BLAST_FURNACE_BE, pos, state);
    }

    public static void init() {
        if (Recipes.blastfurnace == null) {
            Recipes.blastfurnace = new BasicMachineRecipeManager();
        }
    }

    @Override
    public void tick() {
        if (world == null || world.isClient) {
            return;
        }

        boolean dirty = false;
        heatup();

        MachineRecipeResult<IRecipeInput, Collection<ItemStack>, ItemStack> result = getOutput();
        if (result != null && isHot()) {
            setActive(true);
            NbtCompound meta = result.getRecipe().getMetaData();
            int air = meta.getInt("fluid");
            if (LiquidUtil.mb(air) <= fluidTank.getFluidAmount()) {
                progress++;
                fluidTank.drainInternal(LiquidUtil.mb(air), true);
            }

            progressNeeded = meta.getInt("duration");
            if (progress >= progressNeeded) {
                operateOnce(result, result.getOutput());
                dirty = true;
                progress = 0;
            }
        } else {
            if (result == null) {
                progress = 0;
            }
            setActive(false);
        }

        if (fluidTank.getFluidAmount() < fluidTank.getCapacity()) {
            dirty |= gainFluid();
        }

        dirty |= upgradeSlot.tickNoMark();
        guiProgress = progressNeeded == 0 ? 0.0F : (float) progress / progressNeeded;
        guiHeat = (float) heat / maxHeat;
        if (dirty) {
            markDirty();
        } else {
            // the heat changes nearly every tick and has to be saved, the comparators need no news
            world.markDirty(pos);
        }
    }

    public void operateOnce(MachineRecipeResult<IRecipeInput, Collection<ItemStack>, ItemStack> result, Collection<ItemStack> processResult) {
        // the recipe manager hands back the input unchanged, so the recipe says how much to take
        inputSlot.consume(Math.max(1, result.getRecipe().getInput().getAmount()));
        outputSlot.add(processResult);
    }

    /** The recipe for the input, when it has the steel recipe's metadata and its output fits. */
    public MachineRecipeResult<IRecipeInput, Collection<ItemStack>, ItemStack> getOutput() {
        if (inputSlot.isEmpty()) {
            return null;
        }
        MachineRecipeResult<IRecipeInput, Collection<ItemStack>, ItemStack> output = inputSlot.process();
        if (output == null || output.getRecipe().getMetaData() == null) {
            return null;
        }
        return outputSlot.canAdd(output.getOutput()) ? output : null;
    }

    /** Empties an air cell from the cell slot into the tank. */
    public boolean gainFluid() {
        return tankInputSlot.processIntoTank(fluidTank, tankOutputSlot);
    }

    /** Whether redstone holds the furnace hot, through an inverter upgrade if one is fitted. */
    public boolean hasRedstoneInput() {
        return upgradeSlot.getRedstoneInput(world.getReceivedRedstonePower(pos)) > 0;
    }

    private void heatup() {
        int heatRequested = 0;
        if ((!inputSlot.isEmpty() || progress >= 1) && heat <= maxHeat) {
            heatRequested = maxHeat - heat + 100;
        } else if (hasRedstoneInput() && heat <= maxHeat) {
            heatRequested = maxHeat - heat + 100;
        }

        if (heatRequested > 0) {
            Direction dir = getFacing();
            BlockEntity te = world.getBlockEntity(pos.offset(dir));
            int gained = 0;
            if (te instanceof IHeatSource source) {
                gained = source.drawHeat(dir.getOpposite(), heatRequested, false);
                heat += gained;
            }
            if (gained == 0) {
                heat -= Math.min(heat, 1);
            }
        } else {
            heat -= Math.min(heat, 1);
        }
    }

    /** Ticks the current operation has run. */
    public int getProgress() {
        return progress;
    }

    public boolean isHot() {
        return heat >= maxHeat;
    }

    @Override
    public double getGuiValue(String name) {
        return switch (name) {
            case "progress" -> guiProgress;
            case "heat" -> guiHeat;
            default -> super.getGuiValue(name);
        };
    }

    /** No EU in a blast furnace: upgrades that want some get none. */
    @Override
    public double getEnergy() {
        return 0.0;
    }

    @Override
    public boolean useEnergy(double amount) {
        return false;
    }

    @Override
    public Set<UpgradableProperty> getUpgradableProperties() {
        return EnumSet.of(UpgradableProperty.RedstoneSensitive, UpgradableProperty.ItemConsuming,
                UpgradableProperty.ItemProducing, UpgradableProperty.FluidConsuming);
    }

    @Override
    public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.writeNbt(nbt, registries);
        nbt.putInt("heat", heat);
        nbt.putInt("progress", progress);
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.readNbt(nbt, registries);
        heat = nbt.getInt("heat");
        progress = nbt.getInt("progress");
    }
}
