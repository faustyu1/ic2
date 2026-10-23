package net.minecraftforge.fluids;

import java.util.Locale;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

/**
 * Minimal shim for Forge's Fluid to keep IC2 API 1:1 compatible on Fabric.
 * Wraps a string name and optionally resolves to a Minecraft Fluid via registry.
 *
 * <p>The colour and the still texture are the ones the 1.12.2 registration gave the fluid; the
 * GUI needs them to draw a tank, which is the only place the port renders a fluid so far.
 */
public class Fluid {
    private final String name;
    private int color = 0xFFFFFF;
    private Identifier stillTexture;
    private boolean gaseous;
    private int temperature = 300;
    private int density = 1000;
    private int viscosity = 1000;
    private int luminosity = 0;

    public Fluid(String name) {
        this.name = name;
    }

    public Fluid(String name, net.minecraft.fluid.Fluid mcFluid) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    /** Tint the fluid is drawn with, as 0xRRGGBB. */
    public int getColor() {
        return color;
    }

    public Fluid setColor(int color) {
        this.color = color;
        return this;
    }

    /**
     * Temperature in kelvin, as the 1.12.2 registration gave it. The heat exchanger managers
     * compare two fluids by it to refuse a recipe that would heat a hot fluid with a cold one.
     */
    public int getTemperature() {
        return temperature;
    }

    public Fluid setTemperature(int temperature) {
        this.temperature = temperature;
        return this;
    }

    public int getDensity() {
        return density;
    }

    public Fluid setDensity(int density) {
        this.density = density;
        return this;
    }

    public int getViscosity() {
        return viscosity;
    }

    public Fluid setViscosity(int viscosity) {
        this.viscosity = viscosity;
        return this;
    }

    public int getLuminosity() {
        return luminosity;
    }

    public Fluid setLuminosity(int luminosity) {
        this.luminosity = luminosity;
        return this;
    }

    public boolean isGaseous() {
        return gaseous;
    }

    public Fluid setGaseous(boolean gaseous) {
        this.gaseous = gaseous;
        return this;
    }

    /**
     * Texture of the standing fluid. IC2 fluids carry their own; for a vanilla fluid the block
     * texture of the same name is the right one.
     */
    public Identifier getStillTexture() {
        if (stillTexture != null) {
            return stillTexture;
        }

        String plain = name.startsWith("ic2") ? name.substring("ic2".length()) : name;
        return Identifier.of("ic2", "block/fluid/" + plain.toLowerCase(Locale.ROOT) + "_still");
    }

    public Fluid setStillTexture(Identifier stillTexture) {
        this.stillTexture = stillTexture;
        return this;
    }

    /**
     * Resolve to Minecraft fluid if registered under the same identifier.
     * Returns null if not found.
     */
    public net.minecraft.fluid.Fluid getMinecraftFluid() {
        try {
            Identifier id = Identifier.of(name);
            if (Registries.FLUID.containsId(id)) {
                return Registries.FLUID.get(id);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    @Override
    public String toString() {
        return "Fluid[" + name + "]";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Fluid other)) return false;
        return name.equals(other.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
