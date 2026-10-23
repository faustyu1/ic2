package ic2.core.block.machine.tileentity;

import ic2.api.recipe.IRecipeInput;
import ic2.api.recipe.Recipes;
import ic2.api.upgrade.UpgradableProperty;
import ic2.core.block.wiring.CableType;
import ic2.core.recipe.BasicMachineRecipeManager;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;
import ic2.core.init.IC2Blocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

/**
 * Fabric port of TileEntityMacerator: grinds ores into crushed ore and blocks into dust, 300 ticks
 * at 2 EU/t an operation, with the recipes of {@code macerator.ini}. The smoke the 1.12.2 client
 * puffed out of the top while it ran is not drawn.
 */
public class TileEntityMacerator extends TileEntityStandardMachine<IRecipeInput, Collection<ItemStack>, ItemStack> {

    public TileEntityMacerator(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 2, 300, 1, 1);
    }

    public TileEntityMacerator() {
        super(2, 300, 1, 1);
    }

    public TileEntityMacerator(BlockPos pos, BlockState state) {
        this(IC2Blocks.MACERATOR_BE, pos, state);
    }

    public static void init() {
        if (Recipes.macerator == null) {
            Recipes.macerator = new BasicMachineRecipeManager();
        }
    }

    @Override
    protected ic2.api.recipe.IBasicMachineRecipeManager getRecipeManager() {
        if (Recipes.macerator == null) init();
        return Recipes.macerator;
    }

    @Override
    public Set<UpgradableProperty> getUpgradableProperties() {
        return EnumSet.of(
            UpgradableProperty.Processing,
            UpgradableProperty.Transformer,
            UpgradableProperty.EnergyStorage,
            UpgradableProperty.ItemConsuming,
            UpgradableProperty.ItemProducing
        );
    }

    public String getStartSoundFile() { return "Machines/MaceratorOp.ogg"; }
    public String getInterruptSoundFile() { return "Machines/InterruptOne.ogg"; }
}
