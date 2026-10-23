package ic2.core.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * The SI formatting the EU reader and the machine screens print their numbers with, ported from
 * {@code ic2.core.util.Util.toSiString}.
 */
public class ToSiStringTest {

    @Test
    public void zeroAndNotANumberKeepTheirSpecialSpelling() {
        assertEquals("0 ", Util.toSiString(0.0, 4));
        assertEquals("NaN ", Util.toSiString(Double.NaN, 4));
        assertEquals("∞ ", Util.toSiString(Double.POSITIVE_INFINITY, 4));
        assertEquals("-∞ ", Util.toSiString(Double.NEGATIVE_INFINITY, 4));
    }

    @Test
    public void numbersUnderAThousandCarryNoPrefix() {
        assertEquals("512", Util.toSiString(512.0, 3));
        assertEquals("8", Util.toSiString(8.0, 1));
    }

    @Test
    public void thousandsAndMillionsGetTheirPrefix() {
        assertEquals("2.05k", Util.toSiString(2048.0, 3));
        // a fraction of exactly zero is left off, as in 1.12.2
        assertEquals("1M", Util.toSiString(1_000_000.0, 3));
        assertEquals("10M", Util.toSiString(10_000_000.0, 3));
    }

    @Test
    public void fractionsGetTheSmallPrefixes() {
        assertEquals("30m", Util.toSiString(0.03, 3));
        assertEquals("500µ", Util.toSiString(0.0005, 3));
    }

    @Test
    public void theSignStaysInFront() {
        assertEquals("-2.05k", Util.toSiString(-2048.0, 3));
    }

    @Test
    public void theDigitCountIsSignificantDigitsNotDecimals() {
        // six significant digits of 2048: four after the leading "2"
        assertEquals("2.04800k", Util.toSiString(2048.0, 6));
    }
}
