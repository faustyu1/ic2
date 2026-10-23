package ic2.core.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads an IC2 ini file with the comments {@link Config} skips, and writes changed values back
 * into a file without touching anything else in it. The settings screen is built from the first
 * and saves through the second.
 */
public final class IniEditor {
    private IniEditor() {
    }

    /**
     * A setting as the shipped file has it.
     *
     * @param section the normalized section, as {@link Config} paths have it
     * @param comment the comment lines right above the key, without their {@code ;}
     */
    public record Entry(String section, String key, String defaultValue, List<String> comment) {
        public String path() {
            return section.isEmpty() ? key : section + "/" + key;
        }
    }

    /** Every key of the file, in file order. */
    public static List<Entry> readEntries(InputStream is) throws IOException {
        List<Entry> entries = new ArrayList<>();
        List<String> comment = new ArrayList<>();
        String section = "";

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    // a blank line ends a comment block: it belonged to no key
                    comment.clear();
                    continue;
                }
                if (trimmed.startsWith(";")) {
                    String text = trimmed.substring(1).trim();
                    if (!text.equals("---")) {
                        comment.add(text);
                    }
                    continue;
                }
                String content = stripComment(trimmed);
                if (content.startsWith("[") && content.endsWith("]")) {
                    section = Config.normalizeSection(content.substring(1, content.length() - 1));
                    comment.clear();
                    continue;
                }
                int sep = content.indexOf('=');
                if (sep < 0) {
                    comment.clear();
                    continue;
                }
                entries.add(new Entry(section, content.substring(0, sep).trim(), content.substring(sep + 1).trim(), List.copyOf(comment)));
                comment.clear();
            }
        }
        return entries;
    }

    /**
     * Writes the values, by path, into the file: a key already there gets the new value in place,
     * with its line's comment kept; a key that is not there is added under its section at the end.
     */
    public static void write(Path file, Map<String, String> values) throws IOException {
        Map<String, String> pending = new LinkedHashMap<>(values);
        List<String> lines = Files.exists(file) ? Files.readAllLines(file, StandardCharsets.UTF_8) : new ArrayList<>();
        String section = "";

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            String content = stripComment(line).trim();
            if (content.startsWith("[") && content.endsWith("]")) {
                section = Config.normalizeSection(content.substring(1, content.length() - 1));
                continue;
            }
            int sep = content.indexOf('=');
            if (sep < 0) {
                continue;
            }
            String key = content.substring(0, sep).trim();
            String value = pending.remove(section.isEmpty() ? key : section + "/" + key);
            if (value != null) {
                int lineSep = line.indexOf('=');
                int comment = line.indexOf(';', lineSep);
                String tail = comment >= 0 ? " " + line.substring(comment) : "";
                lines.set(i, (line.substring(0, lineSep + 1) + " " + value).stripTrailing() + tail);
            }
        }

        String lastSection = null;
        for (Map.Entry<String, String> entry : pending.entrySet()) {
            int slash = entry.getKey().lastIndexOf('/');
            String entrySection = slash < 0 ? "" : entry.getKey().substring(0, slash);
            if (!entrySection.equals(lastSection)) {
                lines.add("");
                if (!entrySection.isEmpty()) {
                    lines.add("[" + entrySection + "]");
                }
                lastSection = entrySection;
            }
            lines.add(entry.getKey().substring(slash + 1) + " = " + entry.getValue());
        }

        Files.createDirectories(file.getParent());
        Files.write(file, lines, StandardCharsets.UTF_8);
    }

    private static String stripComment(String line) {
        int comment = line.indexOf(';');
        return comment >= 0 ? line.substring(0, comment) : line;
    }
}
