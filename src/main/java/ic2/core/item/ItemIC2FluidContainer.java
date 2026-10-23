package ic2.core.item;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

public class ItemIC2FluidContainer extends ItemIC2 {
    private final Fluid fluid;
    public ItemIC2FluidContainer(String name, Fluid fluid){ super(name); this.fluid=fluid; }
    public Fluid getFluid(){ return fluid; }
    public FluidStack getFluidStack(ItemStack stack){ return fluid==null?null:new FluidStack(fluid,1000); }
}
