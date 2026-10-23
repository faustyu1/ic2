package ic2.api.recipe;

import java.util.Map;
import net.minecraft.fluid.Fluid;

public interface IFluidHeatManager extends ILiquidAcceptManager {
    void addFluid(String fluidName, int amount, int heat);

    BurnProperty getBurnProperty(Fluid fluid);

    Map<String, BurnProperty> getBurnProperties();

    class BurnProperty {
        public final int amount;
        public final int heat;

        public BurnProperty(int amount, int heat) {
            this.amount = amount;
            this.heat = heat;
        }
    }
}
