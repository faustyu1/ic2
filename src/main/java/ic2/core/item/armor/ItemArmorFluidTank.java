package ic2.core.item.armor;

import ic2.core.item.IItemFluidTank;
import ic2.core.ref.FluidName;
import ic2.core.util.LiquidUtil;
import java.util.List;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.world.World;

/**
 * Fabric port of {@code ic2.core.item.armor.ItemArmorFluidTank}: a chest piece carrying a tank of
 * one fluid - biogas for the jetpack, construction foam for the CF pack. It gives no protection.
 *
 * <p>1.12.2 showed the fill through the item's damage (27 points) and kept the fluid behind a Forge
 * capability; here the bar is drawn from the fluid itself and the fluid lives in the stack's data,
 * see {@link IItemFluidTank}.
 */
public abstract class ItemArmorFluidTank extends ItemArmorUtility implements IItemFluidTank {
    protected final int capacity;
    protected final FluidName allowFluid;

    protected ItemArmorFluidTank(String itemName, RegistryEntry<ArmorMaterial> material, FluidName allowFluid, int capacity) {
        super(itemName, material, Type.CHESTPLATE);
        this.capacity = capacity;
        this.allowFluid = allowFluid;
    }

    @Override
    public long getCapacity(ItemStack stack) {
        return LiquidUtil.mb(capacity);
    }

    @Override
    public boolean canHold(ItemStack stack, Fluid fluid) {
        return fluid != null && fluid.equals(allowFluid.getInstance());
    }

    @Override
    public Fluid getCreativeFluid() {
        return allowFluid.getInstance();
    }

    /** Puts a full tank in the stack, {@code filltank} in 1.12.2. */
    public void fillTank(ItemStack stack) {
        setFluid(stack, allowFluid.getVariant(), getCapacity(stack));
    }

    public double getCharge(ItemStack stack) {
        return getAmount(stack);
    }

    public double getMaxCharge(ItemStack stack) {
        return getCapacity(stack);
    }

    @Override
    public boolean isItemBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getItemBarStep(ItemStack stack) {
        return fluidBarStep(stack);
    }

    @Override
    public int getItemBarColor(ItemStack stack) {
        return fluidBarColor(stack);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);
        appendFluidTooltip(stack, tooltip);
    }

    @Override
    public void onArmorTick(World world, PlayerEntity player, ItemStack stack) {
    }
}
