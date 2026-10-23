package ic2.core.ref;

import net.minecraft.util.Identifier;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;

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
    private Fluid instance;

    FluidName(){ this(true); }
    FluidName(boolean hasFlow){ this.hasFlowTexture=hasFlow; }

    public String getName(){ return "ic2"+name(); }
    public Identifier getTextureLocation(boolean flowing){
        if(name().startsWith("molten_")) return Identifier.of("ic2","block/fluid/molten_metal");
        String type = flowing && hasFlowTexture ? "flow" : "still";
        return Identifier.of("ic2","block/fluid/"+name()+"_"+type);
    }
    public boolean hasInstance(){ return instance!=null; }
    public Fluid getInstance(){
        if(instance==null){
            Fluid f = FluidRegistry.getFluid(getName());
            if(f!=null) instance=f;
            else {
                instance=new Fluid(getName())
                        .setColor(getColor())
                        .setTemperature(getTemperature())
                        .setStillTexture(getTextureLocation(false));
                FluidRegistry.registerFluid(instance);
            }
        }
        return instance;
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

    /** Registers every IC2 fluid plus the vanilla ones IC2 handles, on both sides alike. */
    public static void registerAll(){
        FluidRegistry.registerDefaults();
        for(FluidName name : values()) name.getInstance();
    }
    public void setInstance(Fluid f){ if(instance!=null) throw new IllegalStateException("conflicting"); instance=f; }
}
