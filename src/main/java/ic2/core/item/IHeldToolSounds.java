package ic2.core.item;

import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvent;
import org.jetbrains.annotations.Nullable;

/**
 * The sounds of a tool held in the main hand, which the client plays: a loop while it is held, a
 * sound when it is taken in hand and one when it is put away. 1.12.2's ItemElectricTool asked the
 * same of its subclasses through getIdleSound, getStartSound and getStopSound.
 */
public interface IHeldToolSounds {
    /** The loop while the tool is held, or null for none - also the test for whether it counts as running. */
    @Nullable SoundEvent getIdleSound(ItemStack stack);

    default @Nullable SoundEvent getStartSound(ItemStack stack) {
        return null;
    }

    default @Nullable SoundEvent getStopSound(ItemStack stack) {
        return null;
    }
}
