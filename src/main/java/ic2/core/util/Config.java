package ic2.core.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fabric port of ic2.core.util.Config - the reader behind the IC2 ini files.
 *
 * <p>Values are addressed by path, joining the section header with the key:
 * {@code [balance / energy / generator]} plus {@code solar} becomes
 * {@code balance/energy/generator/solar}. Whitespace around the section separators is dropped, so
 * the inconsistent spacing in the shipped {@code general.ini} does not matter.
 */
public class Config {
    private static final Logger LOG = LoggerFactory.getLogger("IC2-Config");

    public final String name;
    private final Map<String, String> values = new LinkedHashMap<>();

    public Config(String name) {
        this.name = name;
    }

    /** Reads an ini file, replacing anything loaded before. */
    public void load(InputStream is) throws IOException {
        values.clear();
        merge(is);
    }

    /** Reads an ini file over what is loaded: its values win, the others stay. */
    public void merge(InputStream is) throws IOException {
        String section = "";

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                int comment = line.indexOf(';');
                if (comment >= 0) {
                    line = line.substring(0, comment);
                }
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                if (line.startsWith("[") && line.endsWith("]")) {
                    section = normalizeSection(line.substring(1, line.length() - 1));
                    continue;
                }

                int sep = line.indexOf('=');
                if (sep < 0) {
                    LOG.warn("[IC2] {}: no '=' in line: {}", name, line);
                    continue;
                }

                String key = line.substring(0, sep).trim();
                String value = line.substring(sep + 1).trim();
                values.put(section.isEmpty() ? key : section + "/" + key, value);
            }
        }
    }

    private static String normalizeSection(String header) {
        StringBuilder ret = new StringBuilder();
        for (String part : header.split("/")) {
            part = part.trim();
            if (part.isEmpty()) {
                continue;
            }
            if (ret.length() > 0) {
                ret.append('/');
            }
            ret.append(part);
        }
        return ret.toString();
    }

    /** @return the raw value, or null if the path is not present */
    public String get(String path) {
        return values.get(path);
    }

    public String getString(String path, String fallback) {
        String value = get(path);
        return value == null ? fallback : value;
    }

    public boolean getBool(String path, boolean fallback) {
        String value = get(path);
        return value == null ? fallback : Boolean.parseBoolean(value);
    }

    public int getInt(String path, int fallback) {
        String value = get(path);
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            LOG.warn("[IC2] {}: {} is not an integer: {}", name, path, value);
            return fallback;
        }
    }

    public double getDouble(String path, double fallback) {
        String value = get(path);
        if (value == null) {
            return fallback;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            LOG.warn("[IC2] {}: {} is not a number: {}", name, path, value);
            return fallback;
        }
    }

    /** Comma separated value, e.g. the recycler black/white lists. */
    public List<String> getList(String path) {
        String value = get(path);
        return value == null ? Collections.emptyList() : ConfigUtil.asList(value);
    }

    /** Paths of every loaded value, in file order. */
    public List<String> paths() {
        return new ArrayList<>(values.keySet());
    }

    public int size() {
        return values.size();
    }
}
