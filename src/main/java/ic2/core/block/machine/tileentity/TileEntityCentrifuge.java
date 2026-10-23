package ic2.core.block.machine.tileentity;

import ic2.api.recipe.IRecipeInput;
import ic2.api.recipe.Recipes;
import ic2.api.upgrade.UpgradableProperty;
import ic2.core.recipe.BasicMachineRecipeManager;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;
import ic2.core.init.IC2Blocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

public class TileEntityCentrifuge extends TileEntityStandardMachine<IRecipeInput, Collection<ItemStack>, ItemStack> {
    public TileEntityCentrifuge(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 48, 500, 3, 2);
    }
    public TileEntityCentrifuge() { super(48, 500, 3, 2); }
    public TileEntityCentrifuge(BlockPos pos, BlockState state) { this(IC2Blocks.CENTRIFUGE_BE, pos, state); }
    public static void init() {
        if (Recipes.centrifuge == null) Recipes.centrifuge = new BasicMachineRecipeManager();
    }
    @Override protected ic2.api.recipe.IBasicMachineRecipeManager getRecipeManager() {
        if (Recipes.centrifuge == null) init();
        return Recipes.centrifuge;
    }
    @Override public Set<UpgradableProperty> getUpgradableProperties() {
        return EnumSet.of(UpgradableProperty.Processing, UpgradableProperty.Transformer, UpgradableProperty.EnergyStorage, UpgradableProperty.ItemConsuming, UpgradableProperty.ItemProducing);
    }
}
