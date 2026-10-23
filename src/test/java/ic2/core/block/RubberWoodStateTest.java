package ic2.core.block;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import ic2.core.block.BlockRubWood.RubberWoodState;
import net.minecraft.util.math.Direction;
import org.junit.Test;

/**
 * The eleven forms one rubber log can take. A wrong wet/dry pairing would either make a tapped
 * log refill instantly or never refill at all, and neither shows up in a build.
 */
public class RubberWoodStateTest {

    @Test
    public void tappingAWetSpotLeavesItDryOnTheSameSide() {
        for (Direction facing : Direction.Type.HORIZONTAL) {
            RubberWoodState wet = RubberWoodState.wet(facing);
            RubberWoodState dry = wet.getDry();

            assertTrue(wet.wet);
            assertFalse(dry.wet);
            assertEquals(facing, dry.facing);
            assertEquals(wet, dry.getWet());
        }
    }

    @Test
    public void onlyADrySpotFillsUpAgain() {
        assertTrue(RubberWoodState.dry_north.canRegenerate());
        assertFalse(RubberWoodState.wet_north.canRegenerate());
        assertFalse(RubberWoodState.plain_y.canRegenerate());
    }

    @Test
    public void aPlainLogHasAnAxisAndNoSpot() {
        assertTrue(RubberWoodState.plain_x.isPlain());
        assertTrue(RubberWoodState.plain_y.isPlain());
        assertTrue(RubberWoodState.plain_z.isPlain());

        assertEquals(Direction.Axis.X, RubberWoodState.plain(Direction.Axis.X).axis);
        assertEquals(Direction.Axis.Y, RubberWoodState.plain(Direction.Axis.Y).axis);
        assertEquals(Direction.Axis.Z, RubberWoodState.plain(Direction.Axis.Z).axis);

        // a plain log has nothing to dry out or fill up, so both stay the same log
        assertEquals(RubberWoodState.plain_y, RubberWoodState.plain_y.getWet());
        assertEquals(RubberWoodState.plain_y, RubberWoodState.plain_y.getDry());
    }

    @Test
    public void everySpotIsNamedAfterItsSide() {
        assertEquals("wet_east", RubberWoodState.wet(Direction.EAST).asString());
        assertEquals("dry_west", RubberWoodState.dry(Direction.WEST).asString());
    }
}
