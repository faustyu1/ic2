package ic2.core.item.reactor;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * The pulse arithmetic of a fuel rod, which is what decides how much heat a reactor design makes.
 *
 * <p>A rod with n pulses produces the n-th triangular number times four heat per cell, so a single
 * rod on its own makes 4 heat and a rod between two reflectors makes 40.
 */
public class ReactorMathTest {

    @Test
    public void heatIsTheTriangularNumberOfThePulses() {
        assertEquals(1, ItemReactorUranium.triangularNumber(1));
        assertEquals(3, ItemReactorUranium.triangularNumber(2));
        assertEquals(6, ItemReactorUranium.triangularNumber(3));
        assertEquals(10, ItemReactorUranium.triangularNumber(4));
        assertEquals(15, ItemReactorUranium.triangularNumber(5));
    }

    @Test
    public void aLoneRodMakesFourHeat() {
        // one cell, base pulses = 1 + 1/2 = 1, no neighbours
        assertEquals(4, ItemReactorUranium.triangularNumber(1) * 4);
    }

    @Test
    public void aQuadRodBetweenFourNeighboursMakesTheOriginalHeat() {
        // four cells, base pulses = 1 + 4/2 = 3, plus one per pulsing neighbour, per cell
        int pulses = 3 + 4;
        int heatPerCell = ItemReactorUranium.triangularNumber(pulses) * 4;

        assertEquals(112, heatPerCell);
        assertEquals(448, heatPerCell * 4);
    }
}
