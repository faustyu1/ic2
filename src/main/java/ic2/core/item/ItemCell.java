package ic2.core.item;

import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

public class ItemCell extends ItemIC2 {
    private Fluid fluid;
    public ItemCell(String name){ super(name); }
    public ItemCell(String name, Fluid fluid){ super(name); this.fluid=fluid; }
    public FluidStack getFluidStack(){ return fluid==null?null:new FluidStack(fluid,1000); }
}
