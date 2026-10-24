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
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;

/**
 * Fabric port of TileEntityMetalFormer: one machine with three recipe sets - extruding (ingots into
 * cables), rolling (ingots into plates, plates into casings) and cutting (plates into item casings
 * and the like). The button in its window cycles the mode, and the input slot takes what the
 * current mode can work.
 */
public class TileEntityMetalFormer extends TileEntityStandardMachine<IRecipeInput, Collection<ItemStack>, ItemStack> {
    /** 0 extruding, 1 rolling, 2 cutting, the order the 1.12.2 button cycled them in. */
    private int mode;

    public TileEntityMetalFormer(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 10, 200, 1, 1);
    }
    public TileEntityMetalFormer() { super(10, 200, 1, 1); }
    public TileEntityMetalFormer(BlockPos pos, BlockState state) { this(IC2Blocks.METAL_FORMER_BE, pos, state); }
    public static void init() {
        if (Recipes.metalformerExtruding == null) Recipes.metalformerExtruding = new BasicMachineRecipeManager();
        if (Recipes.metalformerCutting == null) Recipes.metalformerCutting = new BasicMachineRecipeManager();
        if (Recipes.metalformerRolling == null) Recipes.metalformerRolling = new BasicMachineRecipeManager();
    }
    @Override protected ic2.api.recipe.IBasicMachineRecipeManager getRecipeManager() {
        if (Recipes.metalformerExtruding == null) init();
        return switch (mode) {
            case 1 -> Recipes.metalformerRolling;
            case 2 -> Recipes.metalformerCutting;
            default -> Recipes.metalformerExtruding;
        };
    }

    public int getMode() {
        return mode;
    }

    @Override
    public void onGuiEvent(net.minecraft.entity.player.PlayerEntity player, String event) {
        if ("mode".equals(event)) {
            mode = (mode + 1) % 3;
            // the half-done work belonged to the old mode's recipe
            progress = 0;
            markDirty();
        } else {
            super.onGuiEvent(player, event);
        }
    }

    /** {@code mode0} to {@code mode2}: which button the window shows. */
    @Override
    public boolean getGuiCondition(String name) {
        if (name.startsWith("mode")) {
            return name.equals("mode" + mode);
        }
        return super.getGuiCondition(name);
    }

    @Override
    public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.writeNbt(nbt, registries);
        nbt.putInt("mode", mode);
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.readNbt(nbt, registries);
        mode = Math.floorMod(nbt.getInt("mode"), 3);
    }
    @Override public Set<UpgradableProperty> getUpgradableProperties() {
        return EnumSet.of(UpgradableProperty.Processing, UpgradableProperty.Transformer, UpgradableProperty.EnergyStorage, UpgradableProperty.ItemConsuming, UpgradableProperty.ItemProducing);
    }
}
