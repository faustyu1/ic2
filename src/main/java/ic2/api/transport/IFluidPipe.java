package ic2.api.transport;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;

public interface IFluidPipe extends IPipe {
    int getTransferRate();
    SingleVariantStorage<FluidVariant> getTank();
    int getCurrentInnerCapacity();
    int getMaxInnerCapacity();
}
