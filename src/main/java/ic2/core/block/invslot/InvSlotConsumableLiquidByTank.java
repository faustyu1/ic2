package ic2.core.block.invslot;

import ic2.core.block.IInventorySlotHolder;
import ic2.core.block.comp.Fluids;
import net.minecraft.fluid.Fluid;

/**
 * Fabric port of InvSlotConsumableLiquidByTank - a liquid slot that takes whatever its tank
 * already holds, so a half full tank cannot be spoiled with another fluid.
 */
public class InvSlotConsumableLiquidByTank extends InvSlotConsumableLiquid {
    public final Fluids.InternalFluidTank tank;

    public InvSlotConsumableLiquidByTank(IInventorySlotHolder<?> base, String name, Access access, int count,
                                         InvSide preferredSide, OpType opType, Fluids.InternalFluidTank tank) {
        super(base, name, access, count, preferredSide, opType);
        this.tank = tank;
    }

    @Override
    protected boolean acceptsLiquid(Fluid fluid) {
        return tank.acceptsFluid(fluid) && (tank.isEmpty() || tank.getFluid().getFluid().equals(fluid));
    }
}
