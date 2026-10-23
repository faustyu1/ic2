package ic2.core.block.machine.tileentity;

import ic2.api.recipe.IRecipeInput;
import ic2.api.recipe.Recipes;
import ic2.api.upgrade.UpgradableProperty;
import ic2.core.recipe.SmeltingRecipeManager;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;
import ic2.core.init.IC2Blocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

public class TileEntityElectricFurnace extends TileEntityStandardMachine<IRecipeInput, Collection<ItemStack>, ItemStack> {
    public TileEntityElectricFurnace(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 3, 100, 1, 1);
    }
    public TileEntityElectricFurnace() {
        super(3, 100, 1, 1);
    }
    public TileEntityElectricFurnace(BlockPos pos, BlockState state) {
        this(IC2Blocks.ELECTRIC_FURNACE_BE, pos, state);
    }
    public static void init() {
        if (Recipes.furnace == null) Recipes.furnace = new SmeltingRecipeManager();
    }
    @Override protected ic2.api.recipe.IBasicMachineRecipeManager getRecipeManager() {
        if (Recipes.furnace == null) init();
        return SmeltingRecipeManager.SmeltingBridge.INSTANCE;
    }
    @Override public Set<UpgradableProperty> getUpgradableProperties() {
        return EnumSet.of(UpgradableProperty.Processing, UpgradableProperty.Transformer, UpgradableProperty.EnergyStorage, UpgradableProperty.ItemConsuming, UpgradableProperty.ItemProducing);
    }
}
