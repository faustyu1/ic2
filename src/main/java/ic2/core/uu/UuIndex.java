package ic2.core.uu;

import ic2.core.init.MainConfig;
import ic2.core.util.Config;
import ic2.core.util.ConfigUtil;
import java.text.ParseException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The configured UU-matter values the scanner and the replicator start from.
 *
 * <p>Values come from {@code uu_scan_values.ini} plus the {@code balance/uu-values/predefined}
 * section of {@code general.ini}, exactly as in 1.12.2. These are the starting values only:
 * {@link UuGraph} passes them on through the recipes, and that is what the machines ask.
 */
public final class UuIndex {
    private static final Logger LOG = LoggerFactory.getLogger("IC2-UU");
    private static final String PREDEFINED_SECTION = "balance/uu-values/predefined/";

    private static final UuIndex INSTANCE = new UuIndex();

    /** Resolved values, in config order. */
    private final Map<Item, Double> values = new LinkedHashMap<>();
    /** Raw config keys, kept for items that are not registered (yet). */
    private final Map<String, Double> rawValues = new LinkedHashMap<>();

    private UuIndex() {}

    public static UuIndex getInstance() {
        return INSTANCE;
    }

    /**
     * Fills the index from the scan value config and the predefined values in general.ini.
     *
     * @param scanValues raw {@code item reference -> UU value} pairs from uu_scan_values.ini
     */
    public void load(Map<String, Double> scanValues) {
        values.clear();
        rawValues.clear();

        scanValues.forEach(this::put);

        Config config = MainConfig.get();
        for (String path : config.paths()) {
            if (path.startsWith(PREDEFINED_SECTION)) {
                String reference = path.substring(PREDEFINED_SECTION.length());
                put(reference, config.getDouble(path, 0.0));
            }
        }

        LOG.info("[IC2] Indexed {} UU values ({} resolved to items)", rawValues.size(), values.size());
        UuGraph.invalidate();
    }

    private void put(String reference, double value) {
        rawValues.put(reference, value);

        // the parser the recipe configs use: the namespace may be spelled "IC2", "#variant" picks
        // a flattened item and a vanilla "@meta" maps to its flattened name
        Item item;
        try {
            ItemStack stack = ConfigUtil.asStack(reference.trim());
            item = stack == null || stack.isEmpty() ? null : stack.getItem();
        } catch (ParseException | RuntimeException e) {
            item = null;
        }

        if (item != null) {
            // an item listed twice keeps its lower value, as the graph's initial values did
            values.merge(item, value, Math::min);
        } else {
            LOG.debug("[IC2] No item for UU value {}", reference);
        }
    }

    /** @return the configured UU-matter cost of the item, or null when it has none */
    public Double getValue(Item item) {
        return values.get(item);
    }

    /** @return the configured UU-matter cost of the stack, or null when it has none */
    public Double getValue(ItemStack stack) {
        return stack == null || stack.isEmpty() ? null : values.get(stack.getItem());
    }

    /** The configured value of every item that has one. */
    public Map<Item, Double> getValues() {
        return Collections.unmodifiableMap(values);
    }

    /** Raw config entries, including the ones whose item is not registered. */
    public Map<String, Double> getRawValues() {
        return Collections.unmodifiableMap(rawValues);
    }

    public int size() {
        return values.size();
    }
}
