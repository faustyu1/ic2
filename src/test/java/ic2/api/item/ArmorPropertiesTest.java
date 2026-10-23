package ic2.api.item;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import ic2.api.item.ISpecialArmor.ArmorProperties;
import org.junit.Test;

/**
 * The share-out Forge's {@code StandardizeList} did, which decides how much of a hit each IC2
 * armour piece pays for. Only the arithmetic is covered here - applying it to an entity needs a
 * running game.
 */
public class ArmorPropertiesTest {

    private static final double EPSILON = 1.0e-9;

    @Test
    public void aFullSuitOfEnergyArmourNeverAbsorbsMoreThanTheWholeHit() {
        // the nano suit: 0.15 / 0.4 / 0.3 / 0.15 of the slot times 0.9, so 0.9 in total
        ArmorProperties[] suit = {
                new ArmorProperties(0, 0.15 * 0.9, Integer.MAX_VALUE),
                new ArmorProperties(0, 0.40 * 0.9, Integer.MAX_VALUE),
                new ArmorProperties(0, 0.30 * 0.9, Integer.MAX_VALUE),
                new ArmorProperties(0, 0.15 * 0.9, Integer.MAX_VALUE),
        };

        ISpecialArmor.standardize(suit, 10.0);

        assertEquals(0.9, totalRatio(suit), EPSILON);
    }

    @Test
    public void ratiosOverOneAreScaledDownToOne() {
        ArmorProperties[] overfull = {
                new ArmorProperties(0, 1.0, Integer.MAX_VALUE),
                new ArmorProperties(0, 1.0, Integer.MAX_VALUE),
        };

        ISpecialArmor.standardize(overfull, 10.0);

        assertEquals(1.0, totalRatio(overfull), EPSILON);
        assertEquals(0.5, overfull[0].absorbRatio, EPSILON);
        assertEquals(0.5, overfull[1].absorbRatio, EPSILON);
    }

    @Test
    public void aPieceNeverAbsorbsMoreDamageThanItsChargeCanPayFor() {
        // 25 * charge / energyPerDamage: a nearly empty suit can only pay for two points of damage
        ArmorProperties[] nearlyEmpty = {
                new ArmorProperties(0, 1.0, 2),
        };

        ISpecialArmor.standardize(nearlyEmpty, 10.0);

        assertEquals(0.2, nearlyEmpty[0].absorbRatio, EPSILON);
        assertTrue(nearlyEmpty[0].absorbRatio * 10.0 <= nearlyEmpty[0].absorbMax);
    }

    @Test
    public void theHigherPriorityGroupIsServedFirst() {
        // the quantum boots take a fall at priority 10, the rest of the suit at 8
        ArmorProperties boots = new ArmorProperties(10, 1.0, Integer.MAX_VALUE);
        ArmorProperties chest = new ArmorProperties(8, 0.48, Integer.MAX_VALUE);
        ArmorProperties[] suit = {chest, boots};

        ISpecialArmor.standardize(suit, 10.0);

        assertEquals("the boots sort ahead of the chestplate", 10, suit[0].priority);
        assertEquals("the chestplate sorts behind them", 8, suit[1].priority);
        assertTrue("and the boots keep the larger share", suit[0].absorbRatio > suit[1].absorbRatio);
    }

    @Test
    public void sortingPutsTheToughestPieceOfAGroupFirst() {
        ArmorProperties weak = new ArmorProperties(0, 0.5, 10);
        ArmorProperties strong = new ArmorProperties(0, 0.5, 1000);

        assertTrue("a higher absorb cap sorts first", strong.compareTo(weak) < 0);
    }

    private static double totalRatio(ArmorProperties[] props) {
        double total = 0.0;
        for (ArmorProperties prop : props) total += prop.absorbRatio;
        return total;
    }
}
