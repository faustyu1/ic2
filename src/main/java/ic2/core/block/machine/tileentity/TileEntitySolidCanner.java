package ic2.core.block.machine.tileentity;

import ic2.api.recipe.IBasicMachineRecipeManager;
import ic2.api.recipe.MachineRecipe;
import ic2.api.recipe.MachineRecipeResult;
import ic2.api.upgrade.UpgradableProperty;
import ic2.core.block.invslot.InvSlotConsumable;
import ic2.core.init.IC2Blocks;
import ic2.core.ref.TeBlock;
import java.util.EnumSet;
import java.util.Set;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

/**
 * Fabric port of TileEntitySolidCanner: puts things into containers - food into tin cans, fuel
 * into fuel rods and the like - by the canner's bottling recipes, 200 ticks at 2 EU a tick.
 */
public class TileEntitySolidCanner extends TileEntityStandardMachine<Void, TileEntityCanner.BottleRecipe, Void> {
    public final InvSlotConsumable canInputSlot;

    public TileEntitySolidCanner(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type == null ? IC2Blocks.SOLID_CANNER_BE : type, pos, state, 2, 200, 1);
        this.canInputSlot = new InvSlotConsumable(this, "canInput", 1) {
            @Override
            public boolean accepts(ItemStack stack) {
                for (TileEntityCanner.BottleRecipe recipe : TileEntityCanner.BOTTLE_RECIPES) {
                    if (recipe.container().test(stack)) {
                        return true;
                    }
                }
                return false;
            }
        };
    }

    public TileEntitySolidCanner(BlockPos pos, BlockState state) {
        this(null, pos, state);
    }

    @Override
    public TeBlock getTeBlock() {
        return TeBlock.solid_canner;
    }

    @Override
    protected IBasicMachineRecipeManager getRecipeManager() {
        return null;
    }

    @Override
    protected boolean acceptsInput(ItemStack stack) {
        for (TileEntityCanner.BottleRecipe recipe : TileEntityCanner.BOTTLE_RECIPES) {
            if (recipe.fill().test(stack)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected MachineRecipeResult<Void, TileEntityCanner.BottleRecipe, Void> getOutput() {
        TileEntityCanner.BottleRecipe recipe = TileEntityCanner.findBottleRecipe(canInputSlot.get(), getInput(), true);
        if (recipe == null || !outputSlot.canAdd(recipe.output())) {
            return null;
        }
        return new MachineRecipeResult<>(new MachineRecipe<>(null, recipe), null);
    }

    @Override
    protected void operate(MachineRecipeResult<Void, TileEntityCanner.BottleRecipe, Void> result) {
        if (result == null) {
            return;
        }
        TileEntityCanner.BottleRecipe recipe = result.getOutput();
        canInputSlot.consume(recipe.containerAmount());
        inputSlot.consume(recipe.fillAmount());
        outputSlot.add(recipe.output().copy());
        markDirty();
    }

    @Override
    public Set<UpgradableProperty> getUpgradableProperties() {
        return EnumSet.of(UpgradableProperty.Processing, UpgradableProperty.Transformer, UpgradableProperty.EnergyStorage,
                UpgradableProperty.ItemConsuming, UpgradableProperty.ItemProducing);
    }
}
