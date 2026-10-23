package ic2.api.recipe;

import ic2.api.util.FluidContainerOutputMode;
import java.util.Collection;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.ResourceAmount;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.ItemStack;

public interface IEmptyFluidContainerRecipeManager extends IMachineRecipeManager<Void, IEmptyFluidContainerRecipeManager.Output, ItemStack> {
    MachineRecipeResult<Void, Output, ItemStack> apply(ItemStack input, Fluid fluid, FluidContainerOutputMode outputMode, boolean adjustInput);

    class Output {
        public final Collection<ItemStack> container;
        /** In droplets. */
        public final ResourceAmount<FluidVariant> fluid;

        public Output(Collection<ItemStack> container, ResourceAmount<FluidVariant> fluid) {
            this.container = container;
            this.fluid = fluid;
        }
    }
}
