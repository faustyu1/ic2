package ic2.core.gui.dynamic;

import static org.junit.Assert.assertEquals;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

/** Covers the slot grid geometry both the container and the screen build from. */
public class SlotGridLayoutTest {

    @Test
    public void colsOnlyGrowsRows() {
        // the upgrade grid of every machine: <slotgrid name="upgrade" cols="1"/> with four slots
        SlotGridLayout layout = SlotGridLayout.of(element("cols", "1"), 4);

        assertEquals(1, layout.cols());
        assertEquals(4, layout.rows());
        assertEquals(4, layout.count(4));
        assertEquals(0, layout.columnOf(3));
        assertEquals(3, layout.rowOf(3));
    }

    @Test
    public void rowsOnlyGrowsCols() {
        // the tank and storage box inventories are written as <slotgrid rows="2"/>
        SlotGridLayout layout = SlotGridLayout.of(element("rows", "2"), 6);

        assertEquals(2, layout.rows());
        assertEquals(3, layout.cols());
        assertEquals(1, layout.rowOf(3));
        assertEquals(0, layout.columnOf(3));
    }

    @Test
    public void verticalFillsColumnsFirst() {
        SlotGridLayout layout = SlotGridLayout.of(element("rows", "2", "vertical", "true"), 4);

        assertEquals(2, layout.rows());
        assertEquals(2, layout.cols());
        assertEquals(1, layout.rowOf(1));
        assertEquals(0, layout.columnOf(1));
        assertEquals(0, layout.rowOf(2));
        assertEquals(1, layout.columnOf(2));
    }

    @Test
    public void offsetSkipsLeadingSlots() {
        SlotGridLayout layout = SlotGridLayout.of(element("cols", "2", "offset", "1"), 5);

        assertEquals(4, layout.count(5));
        assertEquals(1, layout.slotIndex(0));
        assertEquals(4, layout.slotIndex(3));
    }

    @Test
    public void styleAndSpacingDriveTheStep() {
        SlotGridLayout layout = SlotGridLayout.of(element("cols", "2", "style", "large", "spacing", "2"), 4);

        assertEquals(SlotStyle.LARGE, layout.style());
        assertEquals(28, layout.stepX());
        assertEquals(28, layout.stepY());
        assertEquals(5, layout.style().insetX());
    }

    @Test
    public void theReactorGridFollowsItsChambers() {
        // guidef/nuclear_reactor.xml states the six rows; the columns come from the built chambers
        assertEquals(3, SlotGridLayout.of(element("rows", "6"), 3 * 6).cols());
        assertEquals(6, SlotGridLayout.of(element("rows", "6"), 6 * 6).cols());
        assertEquals(9, SlotGridLayout.of(element("rows", "6"), 9 * 6).cols());
        assertEquals(54, SlotGridLayout.of(element("rows", "6"), 9 * 6).count(9 * 6));
    }

    @Test
    public void emptyGroupHasNoCells() {
        SlotGridLayout layout = SlotGridLayout.of(element("cols", "1"), 0);

        assertEquals(0, layout.count(0));
    }

    @Test
    public void withoutRowsOrColsTheGridIsSquarish() {
        SlotGridLayout layout = SlotGridLayout.of(element(), 9);

        assertEquals(3, layout.rows());
        assertEquals(3, layout.cols());
    }

    private static GuiElementNode element(String... attributes) {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < attributes.length; i += 2) {
            map.put(attributes[i], attributes[i + 1]);
        }
        return new GuiElementNode("slotgrid", map, List.of(), "");
    }
}
