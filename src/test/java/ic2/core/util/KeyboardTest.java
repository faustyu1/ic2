package ic2.core.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.EnumSet;
import java.util.Set;

import ic2.core.util.Keyboard.Key;
import org.junit.Test;

/**
 * The packed key state the client sends to the server. The bit per key is the enum ordinal, the
 * same layout as 1.12.2, so a mismatch here would silently give equipment the wrong key.
 */
public class KeyboardTest {

    @Test
    public void eachKeyOwnsItsOwnBit() {
        assertEquals(1, Key.toInt(EnumSet.of(Key.alt)));
        assertEquals(2, Key.toInt(EnumSet.of(Key.boost)));
        assertEquals(4, Key.toInt(EnumSet.of(Key.forward)));
        assertEquals(8, Key.toInt(EnumSet.of(Key.modeSwitch)));
        assertEquals(16, Key.toInt(EnumSet.of(Key.jump)));
        assertEquals(32, Key.toInt(EnumSet.of(Key.sideInventory)));
        assertEquals(64, Key.toInt(EnumSet.of(Key.hubMode)));
    }

    @Test
    public void aStateSurvivesTheRoundTrip() {
        Set<Key> held = EnumSet.of(Key.alt, Key.forward, Key.hubMode);

        Set<Key> back = Key.fromInt(Key.toInt(held));

        assertEquals(held, back);
    }

    @Test
    public void noKeysIsZero() {
        assertEquals(0, Key.toInt(EnumSet.noneOf(Key.class)));
        assertTrue(Key.fromInt(0).isEmpty());
    }

    @Test
    public void bitsAboveTheKnownKeysAreIgnored() {
        // a newer client sending a key this build does not know must not shift the rest
        assertEquals(EnumSet.of(Key.alt), Key.fromInt(1 | (1 << 20)));
    }
}
