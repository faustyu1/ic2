package net.minecraftforge.fluids;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.util.Identifier;

/**
 * Minimal shim for Forge's FluidRegistry.
 * Delegates to a simple string-keyed map. On Fabric, fluids are registered via Registries.FLUID
 * but IC2 recipe managers use string names, so we keep a lightweight registry here.
 */
public final class FluidRegistry {
    private static final Map<String, Fluid> registry = new HashMap<>();

    private FluidRegistry() {}

    public static Fluid getFluid(String fluidName) {
        if (fluidName == null) return null;
        Fluid fluid = registry.get(fluidName);
        if (fluid != null) return fluid;
        // Try to resolve from Minecraft registry via Identifier
        try {
            net.minecraft.util.Identifier id = net.minecraft.util.Identifier.of(fluidName);
            if (net.minecraft.registry.Registries.FLUID.containsId(id)) {
                Fluid newFluid = new Fluid(fluidName);
                registry.put(fluidName, newFluid);
                return newFluid;
            }
        } catch (Throwable ignored) {
            // the registry is not up during early init and does not exist at all in unit tests
        }
        return null;
    }

    public static String getFluidName(Fluid fluid) {
        return fluid == null ? null : fluid.getName();
    }

    public static boolean isFluidRegistered(String fluidName) {
        return getFluid(fluidName) != null;
    }

    public static void registerFluid(Fluid fluid) {
        if (fluid != null) {
            registry.put(fluid.getName(), fluid);
        }
    }

    public static Map<String, Fluid> getRegisteredFluids() {
        return Collections.unmodifiableMap(registry);
    }

    /**
     * A number that names a fluid across the network.
     *
     * <p>The GUI has to tell the client which fluid a tank holds, and an index into this registry
     * would depend on the order things were registered in. The hash of the name does not, and a
     * collision between two of the roughly thirty fluid names would have to be constructed.
     */
    public static int getFluidId(Fluid fluid) {
        return fluid == null ? 0 : fluid.getName().hashCode();
    }

    /** @return the fluid that {@link #getFluidId} produced this number for, or null */
    public static Fluid byId(int id) {
        if (id == 0) {
            return null;
        }
        for (Fluid fluid : registry.values()) {
            if (fluid.getName().hashCode() == id) {
                return fluid;
            }
        }
        return null;
    }

    /**
     * Registers the vanilla fluids IC2 deals with by hand. Forge had them in its own registry;
     * here they only need a name, a colour and the texture a tank draws them with.
     */
    public static void registerDefaults() {
        registerFluid(new Fluid("water")
                .setColor(0x3F76E4)
                .setTemperature(300)
                .setStillTexture(Identifier.of("minecraft", "block/water_still")));
        registerFluid(new Fluid("lava")
                .setColor(0xD45A12)
                .setTemperature(1300)
                .setDensity(3000)
                .setViscosity(6000)
                .setLuminosity(15)
                .setStillTexture(Identifier.of("minecraft", "block/lava_still")));
        registerFluid(new Fluid("ic2milk")
                .setColor(0xFCFCFC)
                .setStillTexture(Identifier.of("ic2", "block/fluid/milk_still")));
    }
}
