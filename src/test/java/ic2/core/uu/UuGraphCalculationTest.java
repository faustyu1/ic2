package ic2.core.uu;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.item.Item;
import org.junit.Test;

/**
 * The value propagation of the UU graph on hand-made recipes. The items are bare instances that
 * only serve as identities, since the item registry is not bootstrapped in unit tests.
 */
public class UuGraphCalculationTest {
    private static final double DELTA = 1.0E-6;

    private final Item a = item();
    private final Item b = item();
    private final Item c = item();
    private final Item d = item();

    @Test
    public void craftedItemCostsItsIngredientsPlusTheRecipe() {
        // 2 A + 1 B -> 3 C at 1 UU: (1 + 2 * 10 + 4) / 3
        Map<Item, Double> values = run(values(a, 10.0, b, 4.0),
                transform(1.0, List.of(slot(a, 1), slot(a, 1), slot(b, 1)), out(c, 3)));

        assertEquals(25.0 / 3.0, values.get(c), DELTA);
        assertEquals(10.0, values.get(a), DELTA);
    }

    @Test
    public void cheapestAlternativeOfAnIngredientCounts() {
        // a slot that takes A or B uses B; two such slots merge into one needing two
        Map<Item, Double> values = run(values(a, 10.0, b, 4.0),
                transform(1.0, List.of(slot(a, 1, b, 1), slot(b, 1, a, 1)), out(c, 1)));

        assertEquals(1.0 + 2 * 4.0, values.get(c), DELTA);
    }

    @Test
    public void cheaperRecipeWinsAndValuesOnlyFall() {
        // C from A at 14 (smelting) or from B at 1 (crafting); a configured value above that drops
        Map<Item, Double> values = run(values(a, 10.0, b, 4.0, c, 100.0),
                transform(14.0, List.of(slot(a, 1)), out(c, 1)),
                transform(1.0, List.of(slot(b, 1)), out(c, 1)));

        assertEquals(5.0, values.get(c), DELTA);
    }

    @Test
    public void cyclesConverge() {
        // C = (1 + 2 A + B) / 3 and A = C for free: C = (5 + 2 C) / 3, so C = 5
        Map<Item, Double> values = run(values(a, 10.0, b, 4.0),
                transform(1.0, List.of(slot(a, 2), slot(b, 1)), out(c, 3)),
                transform(0.0, List.of(slot(c, 1)), out(a, 1)));

        assertEquals(5.0, values.get(c), 1.0E-5);
        assertEquals(5.0, values.get(a), 1.0E-5);
    }

    @Test
    public void unreachableItemStaysInfinite() {
        // D needs C, which nothing makes
        Map<Item, Double> values = run(values(a, 1.0),
                transform(1.0, List.of(slot(a, 1), slot(c, 1)), out(d, 1)));

        assertTrue(Double.isInfinite(values.get(d)));
        assertTrue(Double.isInfinite(values.get(c)));
    }

    @Test
    public void outputsOfTheSameItemAddUp() {
        // two outputs of one C each make 2 C: (1 + 4) / 2
        Map<Item, Double> values = run(values(b, 4.0),
                transform(1.0, List.of(slot(b, 1)), out(c, 1), out(c, 1)));

        assertEquals(2.5, values.get(c), DELTA);
    }

    @Test
    public void lateResolverSeesEveryObtainableItem() {
        // a recycler-like recipe: 8 of any obtainable item plus 11.77 make one D
        List<Item> seen = new ArrayList<>();
        ILateRecipeResolver late = obtainable -> {
            List<LeanItemStack> input = new ArrayList<>();
            for (LeanItemStack stack : obtainable) {
                seen.add(stack.getItem());
                input.add(stack.copyWithSize(8));
            }
            return Collections.singletonList(transform(11.77, Collections.singletonList(input), out(d, 1)));
        };

        UuGraph.Calculation calculation = new UuGraph.Calculation();
        calculation.addTransformations(List.of(transform(1.0, List.of(slot(a, 1)), out(c, 1))),
                List.of(a, b), List.of(late));
        calculation.setInitialValues(values(a, 10.0, b, 0.5));

        assertTrue(seen.containsAll(List.of(a, b, c)));
        assertEquals(11.77 + 8 * 0.5, calculation.get(d), DELTA);
    }

    private static Map<Item, Double> run(Map<Item, Double> initial, RecipeTransformation... transforms) {
        UuGraph.Calculation calculation = new UuGraph.Calculation();
        calculation.addTransformations(new ArrayList<>(Arrays.asList(transforms)), initial.keySet(), List.of());
        calculation.setInitialValues(initial);
        return calculation.getValues();
    }

    private static Map<Item, Double> values(Object... pairs) {
        Map<Item, Double> ret = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            ret.put((Item) pairs[i], (Double) pairs[i + 1]);
        }
        return ret;
    }

    private static RecipeTransformation transform(double cost, List<List<LeanItemStack>> inputs, LeanItemStack... outputs) {
        return new RecipeTransformation(cost, new ArrayList<>(inputs), new ArrayList<>(Arrays.asList(outputs)));
    }

    /** One ingredient slot: item, amount, item, amount... */
    private static List<LeanItemStack> slot(Object... alternatives) {
        List<LeanItemStack> ret = new ArrayList<>();
        for (int i = 0; i < alternatives.length; i += 2) {
            ret.add(new LeanItemStack((Item) alternatives[i], (Integer) alternatives[i + 1]));
        }
        return ret;
    }

    private static LeanItemStack out(Item item, int amount) {
        return new LeanItemStack(item, amount);
    }

    /** An item instance without running its constructor, which needs the registries. */
    private static Item item() {
        try {
            Field field = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");
            field.setAccessible(true);
            Object unsafe = field.get(null);
            return (Item) unsafe.getClass().getMethod("allocateInstance", Class.class).invoke(unsafe, Item.class);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }
}
