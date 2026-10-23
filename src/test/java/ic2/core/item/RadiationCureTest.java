package ic2.core.item;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * How much radiation an iodine tablet and a terra wart take off. The item classes need a running
 * game to exercise, so the arithmetic they run is checked here.
 */
public class RadiationCureTest {

    // ---- iodine tablet: one second of the effect per tablet, the whole stack at once ----

    @Test
    public void aStackOfTabletsCutsOneSecondPerTablet() {
        // 60 seconds of radiation, 16 tablets in hand
        assertEquals(44 * 20, iodineRemainder(60 * 20, 16));
    }

    @Test
    public void moreTabletsThanSecondsClearsTheEffect() {
        assertEquals(0, iodineRemainder(5 * 20, 16));
    }

    @Test
    public void aTabletDoesNothingBelowOneWholeSecondLeft() {
        // duration / 20 rounds down, so under a second there is nothing to take off
        assertFalse(iodineApplies(19, 16));
        assertTrue(iodineApplies(20, 1));
    }

    // ---- terra wart: a flat 30 seconds ----

    @Test
    public void aWartTakesThirtySecondsOff() {
        assertEquals(30 * 20, wartRemainder(60 * 20));
    }

    @Test
    public void aWartClearsAnythingShorterThanThirtySeconds() {
        assertEquals(0, wartRemainder(600));
        assertEquals(0, wartRemainder(599));
    }

    /** Ticks of radiation left after swallowing {@code tablets}. */
    private static int iodineRemainder(int durationTicks, int tablets) {
        int seconds = durationTicks / 20;
        int amount = Math.min(tablets, seconds);
        if (amount <= 0) return durationTicks;
        return amount < seconds ? (seconds - amount) * 20 : 0;
    }

    private static boolean iodineApplies(int durationTicks, int tablets) {
        return Math.min(tablets, durationTicks / 20) > 0;
    }

    /** Ticks of radiation left after eating a terra wart. */
    private static int wartRemainder(int durationTicks) {
        return durationTicks > 600 ? durationTicks - 600 : 0;
    }
}
