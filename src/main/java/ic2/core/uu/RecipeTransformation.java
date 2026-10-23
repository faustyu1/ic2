package ic2.core.uu;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

/**
 * Port of {@code ic2.core.uu.RecipeTransformation}: one recipe as the UU graph sees it - a fixed
 * cost plus, for every ingredient, the cheapest of its alternatives, giving the outputs.
 */
class RecipeTransformation {
    final double transformCost;
    /** One list per ingredient, holding the stacks that can fill it. */
    List<List<LeanItemStack>> inputs;
    List<LeanItemStack> outputs;

    RecipeTransformation(double transformCost, List<List<LeanItemStack>> inputs, LeanItemStack... outputs) {
        this(transformCost, inputs, Arrays.asList(outputs));
    }

    RecipeTransformation(double transformCost, List<List<LeanItemStack>> inputs, List<LeanItemStack> outputs) {
        this.transformCost = transformCost;
        this.inputs = inputs;
        this.outputs = outputs;
    }

    /**
     * Folds ingredients with the same alternatives into one with the amounts added up (a recipe
     * with eight planks needs eight of the cheapest plank, not eight times the cheapest of a
     * possibly different plank each), and outputs of the same item into one stack.
     */
    void merge() {
        List<List<LeanItemStack>> cleanInputs = new ArrayList<>();

        for (List<LeanItemStack> inputList : inputs) {
            boolean found = false;

            for (ListIterator<List<LeanItemStack>> it = cleanInputs.listIterator(); it.hasNext(); ) {
                List<LeanItemStack> merged = mergeEqualLists(inputList, it.next());
                if (merged != null) {
                    found = true;
                    it.set(merged);
                    break;
                }
            }

            if (!found) {
                cleanInputs.add(inputList);
            }
        }

        inputs = cleanInputs;
        List<LeanItemStack> cleanOutputs = new ArrayList<>();

        for (LeanItemStack output : outputs) {
            boolean found = false;

            for (ListIterator<LeanItemStack> it = cleanOutputs.listIterator(); it.hasNext(); ) {
                LeanItemStack stack = it.next();
                if (output.hasSameItem(stack)) {
                    found = true;
                    it.set(stack.copyWithSize(stack.getSize() + output.getSize()));
                    break;
                }
            }

            if (!found) {
                cleanOutputs.add(output);
            }
        }

        outputs = cleanOutputs;
    }

    @Override
    public String toString() {
        return "{ " + transformCost + " + " + inputs + " -> " + outputs + " }";
    }

    private static List<LeanItemStack> mergeEqualLists(List<LeanItemStack> listA, List<LeanItemStack> listB) {
        if (listA.size() != listB.size()) {
            return null;
        }

        List<LeanItemStack> ret = new ArrayList<>(listA.size());
        List<LeanItemStack> listBCopy = new LinkedList<>(listB);

        for (LeanItemStack a : listA) {
            boolean found = false;

            for (Iterator<LeanItemStack> it = listBCopy.iterator(); it.hasNext(); ) {
                LeanItemStack b = it.next();
                if (a.hasSameItem(b)) {
                    found = true;
                    ret.add(a.copyWithSize(a.getSize() + b.getSize()));
                    it.remove();
                    break;
                }
            }

            if (!found) {
                return null;
            }
        }

        return ret;
    }
}
