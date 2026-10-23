package ic2.core.item;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import ic2.core.item.type.IRadioactiveItemType;
import ic2.core.item.type.NuclearResourceType;
import org.junit.Test;

/**
 * The radiation numbers a carried nuclear stack hands out, and the rate the effect fires at.
 * Both are the 1.12.2 formulas, integer division and all.
 */
public class RadiationTest {

    @Test
    public void everyNuclearVariantKeepsItsRadiationNumbers() {
        assertEquals(60, NuclearResourceType.uranium.getRadiationDuration());
        assertEquals(100, NuclearResourceType.uranium.getRadiationAmplifier());
        assertEquals(300, NuclearResourceType.mox.getRadiationDuration());
        assertEquals(2, NuclearResourceType.rtg_pellet.getRadiationDuration());
        assertEquals(90, NuclearResourceType.rtg_pellet.getRadiationAmplifier());
    }

    @Test
    public void theDurationIsSecondsAndReachesTheEffectAsTicks() {
        IRadioactiveItemType type = NuclearResourceType.uranium_235;
        assertEquals(150 * 20, type.getRadiationDuration() * 20);
    }

    @Test
    public void theDamagePerHitIsHalfAHeartPlusOnePerHundredAmplifier() {
        assertEquals(0.5f, damageFor(90), 1.0e-6);
        assertEquals(1.5f, damageFor(100), 1.0e-6);
    }

    @Test
    public void aHighAmplifierFiresEveryTick() {
        // 25 >> amplifier uses only the low five bits of the shift, so 100 shifts by 4
        assertTrue(fires(7, 100));
        assertTrue(fires(8, 100));
    }

    @Test
    public void aZeroAmplifierFiresEveryTwentyFifthTick() {
        assertTrue(fires(50, 0));
        assertFalse(fires(51, 0));
    }

    /** {@code IC2Potion.performEffect} in 1.12.2. */
    private static float damageFor(int amplifier) {
        return (float) (amplifier / 100) + 0.5f;
    }

    /** {@code IC2Potion.isReady} in 1.12.2. */
    private static boolean fires(int duration, int amplifier) {
        int rate = 25 >> amplifier;
        return rate > 0 ? duration % rate == 0 : true;
    }
}
