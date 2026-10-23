package ic2.core.block.machine.tileentity;

import ic2.api.item.IBlockCuttingBlade;
import ic2.api.recipe.IRecipeInput;
import ic2.api.recipe.MachineRecipeResult;
import ic2.api.recipe.Recipes;
import ic2.api.upgrade.UpgradableProperty;
import ic2.core.block.invslot.InvSlotConsumable;
import ic2.core.init.IC2Blocks;
import ic2.core.recipe.BasicMachineRecipeManager;
import ic2.core.ref.TeBlock;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

/**
 * Fabric port of TileEntityBlockCutter: cuts blocks into plates and the like in 450 ticks at 4 EU
 * a tick. It needs a cutting blade at least as hard as the recipe's {@code hardness}; otherwise -
 * or without a blade - it waits and the window says the blade is too weak.
 */
public class TileEntityBlockCutter extends TileEntityStandardMachine<IRecipeInput, Collection<ItemStack>, ItemStack> {
    private boolean bladeTooWeak;
    public final InvSlotConsumable cutterSlot;

    public TileEntityBlockCutter(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type == null ? IC2Blocks.BLOCK_CUTTER_BE : type, pos, state, 4, 450, 1, 1);
        this.cutterSlot = new InvSlotConsumable(this, "cutterInputSlot", 1) {
            @Override
            public boolean accepts(ItemStack stack) {
                return stack.getItem() instanceof IBlockCuttingBlade;
            }
        };
    }

    public TileEntityBlockCutter(BlockPos pos, BlockState state) {
        this(null, pos, state);
    }

    public static void init() {
        if (Recipes.blockcutter == null) {
            Recipes.blockcutter = new BasicMachineRecipeManager();
        }
    }

    @Override
    protected ic2.api.recipe.IBasicMachineRecipeManager getRecipeManager() {
        if (Recipes.blockcutter == null) {
            init();
        }
        return Recipes.blockcutter;
    }

    @Override
    protected MachineRecipeResult<IRecipeInput, Collection<ItemStack>, ItemStack> getOutput() {
        if (cutterSlot.isEmpty()) {
            bladeTooWeak = true;
            return null;
        }
        bladeTooWeak = false;

        MachineRecipeResult<IRecipeInput, Collection<ItemStack>, ItemStack> ret = super.getOutput();
        if (ret == null || ret.getRecipe().getMetaData() == null) {
            return null;
        }
        ItemStack blade = cutterSlot.get();
        if (ret.getRecipe().getMetaData().getInt("hardness") > ((IBlockCuttingBlade) blade.getItem()).getHardness(blade)) {
            bladeTooWeak = true;
            return null;
        }
        return ret;
    }

    public boolean isBladeTooWeak() {
        return bladeTooWeak;
    }

    @Override
    public boolean getGuiCondition(String name) {
        return "isBladeTooWeak".equals(name) ? bladeTooWeak : super.getGuiCondition(name);
    }

    @Override
    public Set<UpgradableProperty> getUpgradableProperties() {
        return EnumSet.of(UpgradableProperty.Processing, UpgradableProperty.Transformer, UpgradableProperty.EnergyStorage,
                UpgradableProperty.ItemConsuming, UpgradableProperty.ItemProducing);
    }
}
