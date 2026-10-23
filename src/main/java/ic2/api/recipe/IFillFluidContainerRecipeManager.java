package ic2.api.recipe;

import ic2.api.util.FluidContainerOutputMode;
import java.util.Collection;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.ResourceAmount;
import net.minecraft.item.ItemStack;

public interface IFillFluidContainerRecipeManager extends IMachineRecipeManager<Void, Collection<ItemStack>, IFillFluidContainerRecipeManager.Input> {
    MachineRecipeResult<Void, Collection<ItemStack>, Input> apply(Input input, FluidContainerOutputMode outputMode, boolean adjustInput);

    class Input {
        public final ItemStack container;
        /** In droplets. */
        public final ResourceAmount<FluidVariant> fluid;

        public Input(ItemStack container, ResourceAmount<FluidVariant> fluid) {
            this.container = container;
            this.fluid = fluid;
        }
    }
}
