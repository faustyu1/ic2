package ic2.core.block.machine.tileentity;

import ic2.api.recipe.MachineRecipeResult;
import ic2.api.recipe.Recipes;
import ic2.api.upgrade.IUpgradableBlock;
import ic2.api.upgrade.UpgradableProperty;
import ic2.core.block.invslot.InvSlotDischarge;
import ic2.core.block.invslot.InvSlotOutput;
import ic2.core.block.invslot.InvSlotProcessable;
import ic2.core.block.invslot.InvSlotUpgrade;
import ic2.core.init.IC2Blocks;
import ic2.core.ref.TeBlock;
import java.util.EnumSet;
import java.util.Set;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;

/**
 * Fabric port of TileEntityInduction - the induction furnace.
 *
 * <p>Two smelting slots worked at once. It has to heat up first: each tick it has something to
 * smelt (or a redstone signal) it spends 1 EU and gains 1 heat, up to 10 000, and loses 4 a tick
 * otherwise. While smelting it spends 15 EU a tick more and progresses heat/30 a tick, finishing at
 * 4000 - from 133 seconds cold down to 12 ticks at full heat. 10 000 EU, tier 2. Comparators read
 * the heat.
 */
public class TileEntityInduction extends TileEntityElectricMachine implements IUpgradableBlock {
    private static final short MAX_HEAT = 10000;
    private static final short PROGRESS_NEEDED = 4000;

    public final InvSlotProcessable<ItemStack, ItemStack, ItemStack> inputSlotA;
    public final InvSlotProcessable<ItemStack, ItemStack, ItemStack> inputSlotB;
    public final InvSlotUpgrade upgradeSlot;
    public final InvSlotOutput outputSlotA;
    public final InvSlotOutput outputSlotB;
    public final InvSlotDischarge dischargeSlot;
    public short heat;
    public short progress;

    public TileEntityInduction(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type == null ? IC2Blocks.INDUCTION_BE : type, pos, state, TeBlock.induction_furnace, 10000, 2);
        this.inputSlotA = new InvSlotProcessable<>(this, "inputA", 1, Recipes.furnace);
        this.inputSlotB = new InvSlotProcessable<>(this, "inputB", 1, Recipes.furnace);
        this.outputSlotA = new InvSlotOutput(this, "outputA", 1);
        this.outputSlotB = new InvSlotOutput(this, "outputB", 1);
        this.upgradeSlot = new InvSlotUpgrade(this, "upgrade", 2);
        this.dischargeSlot = new InvSlotDischarge(this, "discharge", 2);
    }

    public TileEntityInduction(BlockPos pos, BlockState state) {
        this(null, pos, state);
    }

    private boolean redstonePowered() {
        return upgradeSlot.getRedstoneInput(world.getReceivedRedstonePower(pos)) > 0;
    }

    @Override
    public void tick() {
        if (world == null || world.isClient) {
            return;
        }

        drainDischargeSlot(dischargeSlot);
        boolean needsInvUpdate = false;
        boolean newActive = getActive();
        if (heat == 0) {
            newActive = false;
        }

        if (progress >= PROGRESS_NEEDED) {
            operate();
            needsInvUpdate = true;
            progress = 0;
            newActive = false;
        }

        boolean canOperate = canOperate();
        if ((canOperate || redstonePowered()) && useEnergy(1.0)) {
            if (heat < MAX_HEAT) {
                heat++;
            }
            newActive = true;
        } else {
            heat = (short) (heat - Math.min(heat, 4));
        }

        if (newActive && progress != 0) {
            if (!canOperate || getEnergy() < 15.0) {
                if (!canOperate) {
                    progress = 0;
                }
                newActive = false;
            }
        } else if (canOperate) {
            if (getEnergy() >= 15.0) {
                newActive = true;
            }
        } else {
            progress = 0;
        }

        if (newActive && canOperate) {
            progress = (short) (progress + heat / 30);
            useEnergy(15.0);
        }

        needsInvUpdate |= upgradeSlot.tickNoMark();
        markDirty();
        setActive(newActive);
    }

    public void operate() {
        operate(inputSlotA, outputSlotA);
        operate(inputSlotB, outputSlotB);
    }

    private void operate(InvSlotProcessable<ItemStack, ItemStack, ItemStack> inputSlot, InvSlotOutput outputSlot) {
        if (canOperate(inputSlot, outputSlot)) {
            MachineRecipeResult<ItemStack, ItemStack, ItemStack> result = inputSlot.process();
            outputSlot.add(result.getOutput().copy());
            inputSlot.consume(1);
        }
    }

    public boolean canOperate() {
        return canOperate(inputSlotA, outputSlotA) || canOperate(inputSlotB, outputSlotB);
    }

    private static boolean canOperate(InvSlotProcessable<ItemStack, ItemStack, ItemStack> inputSlot, InvSlotOutput outputSlot) {
        if (inputSlot.isEmpty()) {
            return false;
        }
        MachineRecipeResult<ItemStack, ItemStack, ItemStack> result = inputSlot.process();
        return result != null && outputSlot.canAdd(result.getOutput());
    }

    @Override
    public int getComparatorOutput() {
        return heat * 15 / MAX_HEAT;
    }

    @Override
    public double getGuiValue(String name) {
        return "progress".equals(name) ? (double) progress / PROGRESS_NEEDED : super.getGuiValue(name);
    }

    @Override
    public double getGuiAmount(String name) {
        return "heat".equals(name) ? heat * 100 / MAX_HEAT : super.getGuiAmount(name);
    }

    @Override
    public Set<UpgradableProperty> getUpgradableProperties() {
        return EnumSet.of(UpgradableProperty.RedstoneSensitive, UpgradableProperty.ItemConsuming, UpgradableProperty.ItemProducing);
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.readNbt(nbt, lookup);
        heat = nbt.getShort("heat");
        progress = nbt.getShort("progress");
    }

    @Override
    public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.writeNbt(nbt, lookup);
        nbt.putShort("heat", heat);
        nbt.putShort("progress", progress);
    }
}
