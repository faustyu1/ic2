package ic2.api.recipe;

import java.util.Map;
import net.minecraft.fluid.Fluid;

public interface ILiquidHeatExchangerManager extends ILiquidAcceptManager {
    void addFluid(String inputFluidName, String outputFluidName, int huPerMB);

    HeatExchangeProperty getHeatExchangeProperty(Fluid fluid);

    Map<String, HeatExchangeProperty> getHeatExchangeProperties();

    ILiquidAcceptManager getSingleDirectionLiquidManager();

    class HeatExchangeProperty {
        public final Fluid outputFluid;
        public final int huPerMB;

        public HeatExchangeProperty(Fluid outputFluid, int huPerMB) {
            this.outputFluid = outputFluid;
            this.huPerMB = huPerMB;
        }
    }
}
