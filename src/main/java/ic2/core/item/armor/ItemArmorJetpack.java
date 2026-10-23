package ic2.core.item.armor;

import ic2.core.item.armor.jetpack.IJetpack;
import ic2.core.item.armor.jetpack.JetpackLogic;
import ic2.core.ref.FluidName;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;

/**
 * Fabric port of {@code ic2.core.item.armor.ItemArmorJetpack}: the fuel jetpack, a 30 000 mB tank
 * of biogas worn on the chest. It flies on {@link JetpackLogic} like the electric one but harder -
 * full power, twice the hover speed, up to the build limit - and burns 1 mB a tick hovering, 2 mB
 * climbing. The last fifth of the tank gives less and less thrust.
 */
public class ItemArmorJetpack extends ItemArmorFluidTank implements IJetpack {

    public ItemArmorJetpack() {
        super("jetpack", IC2ArmorMaterials.JETPACK, FluidName.biogas, 30000);
    }

    @Override
    public void onArmorTick(World world, PlayerEntity player, ItemStack stack) {
        JetpackLogic.onArmorTick(world, player, stack, this);
    }

    @Override
    public boolean drainEnergy(ItemStack pack, int amount) {
        if (isEmpty(pack)) {
            return false;
        }

        FluidStack drained = drain(pack, amount, false);
        if (drained == null || drained.amount < amount) {
            return false;
        }
        drain(pack, amount, true);
        return true;
    }

    @Override
    public float getPower(ItemStack stack) {
        return 1.0F;
    }

    @Override
    public float getDropPercentage(ItemStack stack) {
        return 0.2F;
    }

    @Override
    public boolean isJetpackActive(ItemStack stack) {
        return true;
    }

    @Override
    public double getChargeLevel(ItemStack stack) {
        return getCharge(stack) / getMaxCharge(stack);
    }

    @Override
    public float getHoverMultiplier(ItemStack stack, boolean upwards) {
        return 0.2F;
    }

    @Override
    public float getWorldHeightDivisor(ItemStack stack) {
        return 1.0F;
    }
}
