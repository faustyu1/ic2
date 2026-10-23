package ic2.core.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Maps the pre-flattening vanilla item references in the IC2 config files onto 1.21.1 item ids.
 *
 * <p>The config files are kept byte-identical to 1.12.2, so they still say
 * {@code minecraft:stonebrick@3} or {@code minecraft:reeds}. The table lives in
 * {@code assets/ic2/port/legacy_items.tsv} so the datapack recipe generator can use the very same
 * mapping.
 *
 * <p>A reference with "any metadata" ({@code @*}) resolves to the name's default entry, which is
 * the variant the 1.12.2 recipes used in practice, e.g. {@code minecraft:log@*} becomes an oak log.
 */
public final class LegacyItemNames {
    private static final Logger LOG = LoggerFactory.getLogger("IC2-Config");
    private static final String PATH = "/assets/ic2/port/legacy_items.tsv";

    private static Map<String, String> mapping;

    private LegacyItemNames() {}

    /**
     * @param name 1.12.2 item name without namespace, e.g. {@code stonebrick}
     * @param meta metadata value as written in the config, {@code null} if none or "any"
     * @return the 1.21.1 item name, or the unchanged name if the table has no entry for it
     */
    public static String resolve(String name, String meta) {
        Map<String, String> table = get();
        if (meta != null) {
            String exact = table.get(name + "@" + meta);
            if (exact != null) {
                return exact;
            }
        }
        return table.getOrDefault(name, name);
    }

    private static synchronized Map<String, String> get() {
        if (mapping == null) {
            Map<String, String> table = new HashMap<>();
            try (InputStream is = LegacyItemNames.class.getResourceAsStream(PATH)) {
                if (is == null) {
                    throw new IOException("Resource not found: " + PATH);
                }
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("#")) {
                            continue;
                        }
                        String[] parts = line.split("\t+");
                        if (parts.length != 2) {
                            LOG.warn("[IC2] malformed legacy item mapping: {}", line);
                            continue;
                        }
                        table.put(parts[0], parts[1]);
                    }
                }
            } catch (IOException e) {
                LOG.error("[IC2] Failed to read the legacy item table", e);
            }
            mapping = Collections.unmodifiableMap(table);
        }
        return mapping;
    }
}
