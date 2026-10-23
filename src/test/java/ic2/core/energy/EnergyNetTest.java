package ic2.core.energy;

import ic2.core.block.wiring.CableType;
import ic2.core.energy.grid.EnergyNetGlobal;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Pure Java unit test for EnergyNet statics.
 * Does not require Minecraft runtime — tests getPowerFromTier / getTierFromPower
 * and CableType conductor loss via reflection instantiation of EnergyNetGlobal.
 */
public class EnergyNetTest {

    private EnergyNetGlobal newEnergyNet() {
        try {
            java.lang.reflect.Constructor<EnergyNetGlobal> c =
                    EnergyNetGlobal.class.getDeclaredConstructor();
            c.setAccessible(true);
            return c.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate EnergyNetGlobal", e);
        }
    }

    @Test
    public void testPowerFromTier() {
        EnergyNetGlobal enet = newEnergyNet();

        // tier < 14: 8 << tier*2  i.e. 8 * 4^tier
        assertEquals(8.0, enet.getPowerFromTier(0), 0.0);
        assertEquals(32.0, enet.getPowerFromTier(1), 0.0);
        assertEquals(128.0, enet.getPowerFromTier(2), 0.0);
        assertEquals(512.0, enet.getPowerFromTier(3), 0.0);
        assertEquals(2048.0, enet.getPowerFromTier(4), 0.0);
        assertEquals(8192.0, enet.getPowerFromTier(5), 0.0);
        assertEquals(32768.0, enet.getPowerFromTier(6), 0.0);

        // boundary tier 13 -> 8 << 26
        assertEquals((double) (8 << 13 * 2), enet.getPowerFromTier(13), 0.0);
        // tier 14 uses Math.pow branch
        assertEquals(8.0 * Math.pow(4.0, 14), enet.getPowerFromTier(14), 0.0);
        assertEquals(8.0 * Math.pow(4.0, 29), enet.getPowerFromTier(29), 0.0);
        // tier >=30 -> cap 9.223372E18F
        assertEquals((double) 9.223372E18F, enet.getPowerFromTier(30), 0.0);
        assertEquals((double) 9.223372E18F, enet.getPowerFromTier(31), 0.0);
    }

    @Test
    public void testLossInCable() {
        // CableType.copper loss is defined as 0.2 in CableType.java:9
        assertEquals(0.2, CableType.copper.loss, 0.0);
        // sanity check other types not equal to copper loss (glass 0.025)
        assertEquals(0.025, CableType.glass.loss, 0.0);
    }

    @Test
    public void testTierFromPower() {
        EnergyNetGlobal enet = newEnergyNet();

        // power <=0 -> 0
        assertEquals(0, enet.getTierFromPower(0.0));
        assertEquals(0, enet.getTierFromPower(-1.0));

        // exact tier powers: ceil(log(power/8)/log(4))
        assertEquals(0, enet.getTierFromPower(8.0));
        assertEquals(1, enet.getTierFromPower(32.0));
        assertEquals(2, enet.getTierFromPower(128.0));
        assertEquals(3, enet.getTierFromPower(512.0));
        assertEquals(4, enet.getTierFromPower(2048.0));

        // non-exact powers round up
        assertEquals(1, enet.getTierFromPower(9.0));
        assertEquals(2, enet.getTierFromPower(33.0));
        assertEquals(2, enet.getTierFromPower(127.0));
        assertEquals(3, enet.getTierFromPower(129.0));

        // round-trip: tier -> power -> tier
        for (int tier = 0; tier <= 6; tier++) {
            double power = enet.getPowerFromTier(tier);
            assertEquals(tier, enet.getTierFromPower(power));
        }
    }
}
