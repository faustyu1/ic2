package ic2.core.item.armor;

import ic2.core.ref.FluidName;

/**
 * Fabric port of {@code ic2.core.item.armor.ItemArmorCFPack}: an 80 000 mB tank of construction
 * foam worn on the chest. The foam sprayer draws from it before its own tank.
 */
public class ItemArmorCFPack extends ItemArmorFluidTank {

    public ItemArmorCFPack() {
        super("cf_pack", IC2ArmorMaterials.CF_PACK, FluidName.construction_foam, 80000);
    }
}
