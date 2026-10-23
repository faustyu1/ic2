package ic2.api.recipe;

import java.util.Map;
import net.minecraftforge.fluids.Fluid;

public interface ISemiFluidFuelManager extends ILiquidAcceptManager {
    void addFluid(String fluidName, long energyPerMb, long energyPerTick);

    void removeFluid(String fluidName);

    FuelProperty getFuelProperty(Fluid fluid);

    Map<String, FuelProperty> getFuelProperties();

    final class FuelProperty {
        public final long energyPerMb;
        public final long energyPerTick;

        public FuelProperty(long energyPerMb, long energyPerTick) {
            this.energyPerMb = energyPerMb;
            this.energyPerTick = energyPerTick;
        }
    }
}
