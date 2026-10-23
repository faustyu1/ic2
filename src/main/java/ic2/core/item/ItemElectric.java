package ic2.core.item;

import ic2.api.item.IElectricItem;
import net.minecraft.item.ItemStack;

/**
 * Fabric stub of electric item - implements IElectricItem with simple EU storage in NBT via DataComponent.
 */
public class ItemElectric extends ItemIC2 implements IElectricItem {

    private final int tier;
    private final double maxCharge;
    private final double transferLimit;

    public ItemElectric(String name, int tier, double maxCharge, double transferLimit){
        this(name, tier, maxCharge, transferLimit, new Settings().maxCount(1));
    }

    /** For an electric item that needs more than the defaults, such as a rarity of its own. */
    protected ItemElectric(String name, int tier, double maxCharge, double transferLimit, Settings settings){
        super(name, settings);
        this.tier=tier; this.maxCharge=maxCharge; this.transferLimit=transferLimit;
    }
    @Override public boolean canProvideEnergy(ItemStack s){ return true; }
    @Override public double getMaxCharge(ItemStack s){ return maxCharge; }
    @Override public int getTier(ItemStack s){ return tier; }
    @Override public double getTransferLimit(ItemStack s){ return transferLimit; }
}
