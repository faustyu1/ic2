package ic2.core;

import net.minecraft.world.World;

/**
 * Port of ic2.core.Platform for 1.21.1 Fabric.
 * Legacy 1.12 used side-check via FMLCommonHandler / isRendering().
 * On 1.21.1 the logical side is stored in {@link World#isClient}.
 */
public class Platform {

    /**
     * Server/simulating side check.
     * Requirement: isSimulating() -> !world.isClient
     */
    public boolean isSimulating(World world) {
        return world != null && !world.isClient;
    }

    /**
     * Client/rendering side check.
     * Requirement: isRendering() -> world.isClient
     */
    public boolean isRendering(World world) {
        return world != null && world.isClient;
    }

    // Legacy no-arg overloads kept for old call-sites (e.g. EnergyNetLocal)
    public boolean isSimulating() {
        return true;
    }

    public boolean isRendering() {
        return false;
    }
}
