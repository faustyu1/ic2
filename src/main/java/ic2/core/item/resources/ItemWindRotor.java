package ic2.core.item.resources;

import ic2.api.item.IKineticRotor;
import ic2.core.item.ItemGradualInt;
import java.util.List;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Fabric port of ItemWindRotor - the rotor a wind or water kinetic generator turns.
 *
 * <p>The rotor decides how big the generator is (its diameter), how much of the wind it turns into
 * kinetic energy and between which wind strengths it works; above the top one it wears four times
 * as fast. Only the wooden rotor is too weak for water.
 */
public class ItemWindRotor extends ItemGradualInt implements IKineticRotor {
    private final int radius;
    private final float efficiency;
    private final int minWindStrength;
    private final int maxWindStrength;
    private final Identifier renderTexture;
    private final boolean water;

    public ItemWindRotor(String name, int radius, int durability, float efficiency, int minWindStrength, int maxWindStrength) {
        super(name, durability, new Item.Settings().maxCount(1));
        this.radius = radius;
        this.efficiency = efficiency;
        this.minWindStrength = minWindStrength;
        this.maxWindStrength = maxWindStrength;
        this.renderTexture = Identifier.of("ic2", "textures/item/rotor/" + name.substring("rotor_".length()) + "_rotor_model.png");
        this.water = !name.equals("rotor_wood");
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("itemrotor.wind.info", minWindStrength, maxWindStrength));
    }

    @Override public int getDiameter(ItemStack stack) { return radius; }
    @Override public Identifier getRotorRenderTexture(ItemStack stack) { return renderTexture; }
    @Override public float getEfficiency(ItemStack stack) { return efficiency; }
    @Override public int getMinWindStrength(ItemStack stack) { return minWindStrength; }
    @Override public int getMaxWindStrength(ItemStack stack) { return maxWindStrength; }

    @Override
    public boolean isAcceptedType(ItemStack stack, GearboxType type) {
        return type == GearboxType.WIND || water;
    }
}
