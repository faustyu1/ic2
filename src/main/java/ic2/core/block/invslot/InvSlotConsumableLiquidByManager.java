package ic2.core.block.invslot;

import ic2.api.recipe.ILiquidAcceptManager;
import ic2.core.block.IInventorySlotHolder;
import net.minecraft.fluid.Fluid;

/**
 * Fabric port of InvSlotConsumableLiquidByManager - a liquid slot that takes whatever a recipe
 * manager knows a recipe for, rather than what the machine's tank happens to hold.
 */
public class InvSlotConsumableLiquidByManager extends InvSlotConsumableLiquid {
    private final ILiquidAcceptManager manager;

    public InvSlotConsumableLiquidByManager(IInventorySlotHolder<?> base, String name, Access access, int count,
                                            InvSide preferredSide, OpType opType, ILiquidAcceptManager manager) {
        super(base, name, access, count, preferredSide, opType);
        this.manager = manager;
    }

    @Override
    protected boolean acceptsLiquid(Fluid fluid) {
        return manager != null && manager.acceptsFluid(fluid);
    }
}
