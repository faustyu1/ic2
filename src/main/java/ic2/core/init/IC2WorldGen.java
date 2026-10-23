package ic2.core.init;

import ic2.core.world.Ic2WorldDecorator;

/**
 * Entry point for all IC2 world generation registration.
 * Called from {@link ic2.core.IC2Mod#onInitialize()}.
 */
public final class IC2WorldGen {
    private IC2WorldGen() {}

    public static void register() {
        Ic2WorldDecorator.register();
    }
}
