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

public class TileEntityCompressor extends TileEntityStandardMachine<IRecipeInput, Collection<ItemStack>, ItemStack> {
    public TileEntityCompressor(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 2, 300, 1, 1);
    }
    public TileEntityCompressor() {
        super(2, 300, 1, 1);
    }
    public TileEntityCompressor(BlockPos pos, BlockState state) {
        this(IC2Blocks.COMPRESSOR_BE, pos, state);
    }
    public static void init() {
        if (Recipes.compressor == null) Recipes.compressor = new BasicMachineRecipeManager();
    }
    @Override protected ic2.api.recipe.IBasicMachineRecipeManager getRecipeManager() {
        if (Recipes.compressor == null) init();
        return Recipes.compressor;
    }
    @Override public Set<UpgradableProperty> getUpgradableProperties() {
        return EnumSet.of(UpgradableProperty.Processing, UpgradableProperty.Transformer, UpgradableProperty.EnergyStorage, UpgradableProperty.ItemConsuming, UpgradableProperty.ItemProducing);
    }
}
