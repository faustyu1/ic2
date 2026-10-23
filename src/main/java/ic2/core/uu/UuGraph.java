package ic2.core.uu;

import ic2.api.recipe.Recipes;
import ic2.core.IC2;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Port of {@code ic2.core.uu.UuGraph}: the UU-matter cost of every item the scanner can record.
 *
 * <p>The values configured in {@link UuIndex} (the world scan values and the predefined ones) are
 * the starting points. Every recipe - crafting table, furnace, the IC2 machines, the scrap box,
 * the recycler and the reactor - then passes value on: an output costs the recipe's own cost plus
 * its cheapest ingredients, divided by the output amount, and the cheapest way to an item wins.
 * Crafting costs 1 UU, a furnace or machine run 14, so a crafted item without a value of its own
 * is still scannable as long as its ingredients have one.
 *
 * <p>In 1.12.2 the graph was built in a worker thread after the mod finished loading. Crafting and
 * smelting recipes come from the server's datapacks now, so the graph is built the first time a
 * value is asked for while a server runs, and again after a {@code /reload} or a new server. Without
 * a server (a client in multiplayer) only the IC2 machine recipes are known.
 */
public final class UuGraph {
    private static final Logger LOG = LoggerFactory.getLogger("IC2-UU");
    private static final double epsilon = 1.0E-9;

    /** Item to UU value (infinite for an item the recipes reach but no value does). */
    private static Map<Item, Double> values;
    /** The server the current values were built from, null when built without one. */
    private static MinecraftServer builtFor;

    private UuGraph() {}

    /** Rebuilds the graph whenever the server's recipes change. */
    public static void init() {
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> invalidate());
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> invalidate());
    }

    /** Throws the values away, so the next lookup rebuilds them. */
    public static synchronized void invalidate() {
        values = null;
        builtFor = null;
    }

    /** @return the UU cost of the item, or 1.0 when it has none */
    public static double getCost(Item item) {
        Double value = values().get(item);
        return value != null && value < Double.POSITIVE_INFINITY ? value : 1.0;
    }

    /** The UU cost in the scan value unit, infinite for an item without one. */
    public static double get(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return Double.POSITIVE_INFINITY;
        }
        Double value = values().get(stack.getItem());
        return value != null ? value : Double.POSITIVE_INFINITY;
    }

    /** The UU cost in buckets: a scan value unit is 10 microbuckets, as in 1.12.2. */
    public static double getInBuckets(ItemStack stack) {
        return get(stack) * 1.0E-5;
    }

    /**
     * The pattern a stack is scanned as: one plain item of its kind (charge, damage and the like
     * are not recorded), or nothing when the graph does not know the item.
     */
    public static ItemStack find(ItemStack stack) {
        return stack == null || stack.isEmpty() || !values().containsKey(stack.getItem()) ? ItemStack.EMPTY : new ItemStack(stack.getItem());
    }

    /** Raw configured values, keyed by the config item reference. */
    public static Map<String, Double> getAll() {
        return UuIndex.getInstance().getRawValues();
    }

    /** Every item with a finite UU value - the 1.12.2 {@code iterator()}. */
    public static Map<Item, Double> getValues() {
        Map<Item, Double> ret = new LinkedHashMap<>();
        values().forEach((item, value) -> {
            if (value < Double.POSITIVE_INFINITY) {
                ret.put(item, value);
            }
        });
        return ret;
    }

    private static synchronized Map<Item, Double> values() {
        MinecraftServer server = IC2.server;
        if (values == null || builtFor != server) {
            values = build(server);
            builtFor = server;
        }
        return values;
    }

    private static Map<Item, Double> build(MinecraftServer server) {
        long startTime = System.nanoTime();
        List<IRecipeResolver> resolvers = new ArrayList<>();
        resolvers.add(new VanillaSmeltingResolver(server));
        if (server != null) {
            resolvers.add(new RecipeResolver(server));
        }
        resolvers.add(new MachineRecipeResolver(Recipes.macerator));
        resolvers.add(new MachineRecipeResolver(Recipes.extractor));
        resolvers.add(new MachineRecipeResolver(Recipes.compressor));
        resolvers.add(new MachineRecipeResolver(Recipes.centrifuge));
        resolvers.add(new MachineRecipeResolver(Recipes.blockcutter));
        resolvers.add(new MachineRecipeResolver(Recipes.blastfurnace));
        resolvers.add(new MachineRecipeResolver(Recipes.metalformerExtruding));
        resolvers.add(new MachineRecipeResolver(Recipes.metalformerCutting));
        resolvers.add(new MachineRecipeResolver(Recipes.metalformerRolling));
        resolvers.add(new MachineRecipeResolver(Recipes.oreWashing));
        resolvers.add(new CannerBottleSolidResolver());
        resolvers.add(new ScrapBoxResolver());
        resolvers.add(new ManualRecipeResolver());
        List<ILateRecipeResolver> lateResolvers = Collections.singletonList(new RecyclerResolver());

        Map<Item, Double> initialValues = new LinkedHashMap<>(UuIndex.getInstance().getValues());

        Calculation calculation = new Calculation();
        List<RecipeTransformation> transformations = new ArrayList<>();
        for (IRecipeResolver resolver : resolvers) {
            try {
                transformations.addAll(resolver.getTransformations());
            } catch (RuntimeException e) {
                LOG.warn("[IC2] UU recipe resolver {} failed", resolver.getClass().getSimpleName(), e);
            }
        }
        calculation.addTransformations(transformations, initialValues.keySet(), lateResolvers);
        calculation.setInitialValues(initialValues);

        Map<Item, Double> ret = calculation.getValues();
        LOG.info("[IC2] UU graph built with {} nodes from {} recipe transformations after {} ms.",
                ret.size(), transformations.size(), (System.nanoTime() - startTime) / 1000000L);
        return ret;
    }

    /**
     * The graph itself, separate from where its recipes come from. Values only ever fall: setting
     * a value on a node passes it on to every recipe the node is an ingredient of, which may lower
     * the value of that recipe's outputs, and so on until nothing changes. 1.12.2 did this
     * recursively; a work queue gives the same values without running out of stack on long recipe
     * chains.
     */
    static final class Calculation {
        private final Map<LeanItemStack, Node> nodes = new HashMap<>();

        /**
         * Registers the recipes: the early ones, then the late ones, which get to see every item
         * the early recipes or the initial values reach.
         */
        void addTransformations(List<RecipeTransformation> transformations, Iterable<Item> valuedItems, List<ILateRecipeResolver> lateResolvers) {
            List<RecipeTransformation> all = new ArrayList<>(transformations);

            for (RecipeTransformation transform : all) {
                for (LeanItemStack output : transform.outputs) {
                    getInternal(output);
                }
            }
            for (Item item : valuedItems) {
                getInternal(new LeanItemStack(item, 1));
            }

            for (ILateRecipeResolver resolver : lateResolvers) {
                try {
                    all.addAll(resolver.getTransformations(new ArrayList<>(nodes.keySet())));
                } catch (RuntimeException e) {
                    LOG.warn("[IC2] UU recipe resolver {} failed", resolver.getClass().getSimpleName(), e);
                }
            }

            for (RecipeTransformation transform : all) {
                transform.merge();
                registerTransform(transform);
            }
        }

        void setInitialValues(Map<Item, Double> initialValues) {
            initialValues.forEach((item, value) -> setValue(getInternal(new LeanItemStack(item, 1)), value));
        }

        /** Every node with its value, infinite where no value reaches it. */
        Map<Item, Double> getValues() {
            Map<Item, Double> ret = new IdentityHashMap<>(nodes.size());
            for (Node node : nodes.values()) {
                ret.put(node.stack.getItem(), node.value);
            }
            return ret;
        }

        double get(Item item) {
            Node node = nodes.get(new LeanItemStack(item, 1));
            return node == null ? Double.POSITIVE_INFINITY : node.value;
        }

        private Node getInternal(LeanItemStack stack) {
            return nodes.computeIfAbsent(stack.copyWithSize(1), Node::new);
        }

        private void registerTransform(RecipeTransformation transform) {
            NodeTransform nt = new NodeTransform(transform);

            for (List<LeanItemStack> inputs : transform.inputs) {
                for (LeanItemStack input : inputs) {
                    getInternal(input).provides.add(nt);
                }
            }

            for (LeanItemStack output : transform.outputs) {
                nt.out.add(getInternal(output));
            }
        }

        private void setValue(Node start, double value) {
            if (!(value < start.value - epsilon)) {
                return;
            }

            start.value = value;
            Deque<Node> changed = new ArrayDeque<>();
            changed.add(start);

            while (!changed.isEmpty()) {
                Node node = changed.poll();

                for (NodeTransform nt : node.provides) {
                    for (Node out : nt.out) {
                        int outputSize = nt.getOutputSize(out.stack);
                        if (outputSize <= 0) {
                            LOG.warn("[IC2] UU update: Invalid output size {} in recipe transform {}, expected {}.", outputSize, nt.transform, out.stack);
                        } else if (out.value > node.value / outputSize) {
                            double newValue = nt.cost() / outputSize;
                            if (newValue < out.value - epsilon) {
                                out.value = newValue;
                                changed.add(out);
                            }
                        }
                    }
                }
            }
        }

        private final class NodeTransform {
            final RecipeTransformation transform;
            final Set<Node> out = new HashSet<>();

            NodeTransform(RecipeTransformation transform) {
                this.transform = transform;
            }

            int getOutputSize(LeanItemStack output) {
                for (LeanItemStack stack : transform.outputs) {
                    if (stack.hasSameItem(output)) {
                        return stack.getSize();
                    }
                }
                return 0;
            }

            /** The recipe's cost with its cheapest ingredients at the current values. */
            double cost() {
                double ret = transform.transformCost;

                for (List<LeanItemStack> inputs : transform.inputs) {
                    double minValue = Double.POSITIVE_INFINITY;

                    for (LeanItemStack input : inputs) {
                        Node node = nodes.get(input);
                        double value = (node == null ? Double.POSITIVE_INFINITY : node.value) * input.getSize();
                        if (value < minValue) {
                            minValue = value;
                        }
                    }

                    ret += minValue;
                }

                return ret;
            }
        }
    }

    private static final class Node {
        final LeanItemStack stack;
        double value = Double.POSITIVE_INFINITY;
        final Set<Calculation.NodeTransform> provides = new HashSet<>();

        Node(LeanItemStack stack) {
            this.stack = stack;
        }
    }
}
