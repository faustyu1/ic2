package ic2.core.ref;

import ic2.core.fluid.Ic2Fluid;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributeHandler;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.minecraft.fluid.Fluid;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * The IC2 fluids. Each is registered as a still and a flowing Minecraft fluid,
 * {@code ic2:<name>} and {@code ic2:flowing_<name>}, with its temperature and gas flag as Fabric
 * fluid variant attributes.
 */
public enum FluidName {
    air(false),
    biogas(false),
    biomass,
    construction_foam,
    coolant,
    distilled_water,
    hot_coolant,
    hot_water,
    pahoehoe_lava(false),
    steam(false),
    superheated_steam(false),
    uu_matter,
    weed_ex(false),
    oxygen(false),
    hydrogen(false),
    heavy_water,
    deuterium(false),
    creosote,
    molten_brass(false),
    molten_bronze(false),
    molten_copper(false),
    molten_gold(false),
    molten_iron(false),
    molten_lead(false),
    molten_silver(false),
    molten_steel(false),
    molten_tin(false),
    molten_zinc(false),
    milk;

    private final boolean hasFlowTexture;
    private Ic2Fluid still;
    private Ic2Fluid flowing;

    FluidName(){ this(true); }
    FluidName(boolean hasFlow){ this.hasFlowTexture=hasFlow; }

    /** The name 1.12.2 registered the fluid under, which the config files and cell variants still use. */
    public String getName(){ return "ic2"+name(); }
    public Identifier getId(){ return Identifier.of("ic2", name()); }
    public Identifier getTextureLocation(boolean flowing){
        if(name().startsWith("molten_")) return Identifier.of("ic2","block/fluid/molten_metal");
        String type = flowing && hasFlowTexture ? "flow" : "still";
        return Identifier.of("ic2","block/fluid/"+name()+"_"+type);
    }
    /** The still fluid, the one tanks and cells hold. */
    public Fluid getInstance(){
        if(still==null) throw new IllegalStateException("IC2 fluids are not registered yet");
        return still;
    }
    public Fluid getFlowing(){
        if(flowing==null) throw new IllegalStateException("IC2 fluids are not registered yet");
        return flowing;
    }
    public FluidVariant getVariant(){ return FluidVariant.of(getInstance()); }

    /** Gases rise in the world and in a tank's rendering; Fabric reads that off this flag. */
    public boolean isGaseous(){
        return switch(this){
            case air, biogas, steam, superheated_steam, oxygen, hydrogen, deuterium -> true;
            default -> false;
        };
    }

    /**
     * The colour the fluid is drawn with, as the 1.12.2 registration gave it. Everything the
     * original did not colour explicitly - the molten metals and the gases added later - falls
     * back to plain water blue.
     */
    public int getColor(){
        return switch(this){
            case uu_matter -> 0x3B0BF3;
            case construction_foam -> 0x202020;
            case coolant -> 0x1459AA;
            case creosote -> 0x3D390A;
            case hot_coolant -> 0xB52034;
            case pahoehoe_lava -> 0x7B762C;
            case biomass -> 0x376C65;
            case biogas -> 0xA791EC;
            case distilled_water -> 0x4353F5;
            case superheated_steam -> 0xCAD3D1;
            case steam -> 0xBCC0BC;
            case hot_water -> 0x46E3FF;
            case weed_ex -> 0x074E94;
            case air, hydrogen, oxygen -> 0xDCDCDC;
            case heavy_water -> 0x4353F5;
            case milk -> 0xFCFCFC;
            default -> 0x3F76E4;
        };
    }

    /**
     * Temperature in kelvin, as the 1.12.2 registration gave it. The heat exchanger tells a
     * heat-up recipe from a cool-down one by comparing these two numbers, so a wrong value here
     * would silently turn a recipe into an error at startup.
     */
    public int getTemperature(){
        return switch(this){
            case hot_coolant, pahoehoe_lava -> 1200;
            case superheated_steam -> 600;
            case steam -> 420;
            case hot_water -> 350;
            default -> 300;
        };
    }

    /** Registers every IC2 fluid, on both sides alike, before the blocks and items that use them. */
    public static void registerAll(){
        for(FluidName name : values()){
            if(name.still!=null) continue;
            name.still=Registry.register(Registries.FLUID, name.getId(), new Ic2Fluid.Still(name));
            name.flowing=Registry.register(Registries.FLUID, Identifier.of("ic2", "flowing_"+name.name()), new Ic2Fluid.Flowing(name));
            FluidVariantAttributeHandler attributes = new FluidVariantAttributeHandler(){
                @Override
                public Text getName(FluidVariant variant){ return Text.translatable(name.name()); }
                @Override
                public int getTemperature(FluidVariant variant){ return name.getTemperature(); }
                @Override
                public boolean isLighterThanAir(FluidVariant variant){ return name.isGaseous(); }
            };
            FluidVariantAttributes.register(name.still, attributes);
            FluidVariantAttributes.register(name.flowing, attributes);
        }
    }

    /** @return the IC2 fluid that Minecraft fluid is, still or flowing, or null for any other */
    public static FluidName of(Fluid fluid){
        return fluid instanceof Ic2Fluid ic2 ? ic2.getFluidName() : null;
    }
}
