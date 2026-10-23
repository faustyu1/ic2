package ic2.integration.rei;

import ic2.core.util.LiquidUtil;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.fluid.Fluid;

/**
 * IC2 fluids as REI entries. Every IC2 fluid is a Minecraft fluid, so they are REI's own fluid
 * entries, drawn and looked up like any other mod's fluids.
 */
final class ReiFluids {
    private ReiFluids() {
    }

    /** @param name the fluid's 1.12.2 name, {@code water} or {@code ic2coolant} */
    static EntryIngredient of(String name, int mb) {
        return of(LiquidUtil.getFluid(name), mb);
    }

    static EntryIngredient of(Fluid fluid, int mb) {
        return fluid == null ? EntryIngredient.empty() : EntryIngredient.of(EntryStacks.of(fluid, LiquidUtil.mb(mb)));
    }
}
