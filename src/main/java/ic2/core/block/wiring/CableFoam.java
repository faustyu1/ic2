package ic2.core.block.wiring;

import net.minecraft.util.StringIdentifiable;

/**
 * How far a cable is buried in construction foam: not at all, under soft foam that still has to
 * set, or inside a hardened foam wall.
 *
 * <p>1.12.2 kept this in the cable's tile entity and networked it; here it is the {@code foam}
 * property of the cable's block state, which is what the shape, the light and the model of the
 * cable depend on.
 */
public enum CableFoam implements StringIdentifiable {
    none,
    soft,
    hardened;

    @Override
    public String asString() {
        return name();
    }
}
