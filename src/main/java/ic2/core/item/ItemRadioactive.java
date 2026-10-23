package ic2.core.item;

import ic2.api.item.IHazmatLike;
import ic2.core.IC2Potion;
import ic2.core.item.type.IRadioactiveItemType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * Fabric port of {@code ItemHandlers.radioactiveUpdate}: carrying bare nuclear material irradiates
 * whoever carries it, unless they wear a complete hazmat suit.
 *
 * <p>1.12.2 hung this on {@code ItemMulti.setUpdateHandler}, with the metadata picking the
 * {@link IRadioactiveItemType}. Flattening gave every variant its own item, so the type is fixed
 * per item and the hook is the vanilla {@code inventoryTick}.
 */
public class ItemRadioactive extends ItemIC2 {
    private final IRadioactiveItemType type;

    public ItemRadioactive(String name, IRadioactiveItemType type) {
        super(name);
        this.type = type;
    }

    public IRadioactiveItemType getRadiationType() {
        return type;
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);
        irradiate(world, entity, type.getRadiationDuration() * 20, type.getRadiationAmplifier());
    }

    /**
     * Irradiate whoever is carrying the stack, unless they wear a complete hazmat suit. Shared
     * with the fuel rods, which did the same from their own {@code onUpdate} in 1.12.2.
     *
     * @param duration in ticks
     */
    public static void irradiate(World world, Entity entity, int duration, int amplifier) {
        if (world.isClient || !(entity instanceof LivingEntity living)) return;
        if (IHazmatLike.hasCompleteHazmat(living)) return;

        IC2Potion.applyTo(living, duration, amplifier);
    }
}
