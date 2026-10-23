package ic2.api.recipe;

import java.util.Set;
import net.minecraft.fluid.Fluid;

public interface ILiquidAcceptManager {
    boolean acceptsFluid(Fluid fluid);

    Set<Fluid> getAcceptedFluids();
}
