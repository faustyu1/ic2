package ic2.integration.modmenu;

import ic2.core.init.MainConfig;
import ic2.core.util.Config;
import ic2.core.util.IniEditor;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The settings of {@code general.ini} on a Cloth Config screen: a tab per top section, a
 * collapsible group per sub section, and the file's comments as tooltips. The kind of field
 * follows the shipped value - a toggle for true/false, number fields for numbers, text for the
 * rest. Saving writes the changed values into {@code config/IC2.ini}; they apply on the next
 * start.
 *
 * <p>Only loaded with Cloth Config present, see {@link Ic2ModMenu}.
 */
final class Ic2ConfigScreen {
    private static final Logger LOG = LoggerFactory.getLogger("IC2-Config");

    private Ic2ConfigScreen() {
    }

    /** @return the screen, or null when the config cannot be read */
    static Screen create(Screen parent) {
        List<IniEditor.Entry> entries;
        Config current;
        try {
            entries = MainConfig.entries();
            current = MainConfig.read();
        } catch (IOException e) {
            LOG.error("[IC2] Failed to read the config for the settings screen", e);
            return null;
        }

        Map<String, String> changed = new LinkedHashMap<>();
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.translatable("ic2.config.title"))
                .setSavingRunnable(() -> save(changed));
        ConfigEntryBuilder eb = builder.entryBuilder();

        Map<String, SubCategoryBuilder> groups = new LinkedHashMap<>();
        for (IniEditor.Entry entry : entries) {
            int slash = entry.section().indexOf('/');
            String top = slash < 0 ? entry.section() : entry.section().substring(0, slash);
            AbstractConfigListEntry<?> field = field(eb, entry, current.getString(entry.path(), entry.defaultValue()), changed);
            if (slash < 0) {
                category(builder, top).addEntry(field);
            } else {
                groups.computeIfAbsent(entry.section(), section -> eb.startSubCategory(
                        Text.literal(section.substring(slash + 1).replace("/", " / "))).setExpanded(true)).add(field);
            }
        }
        groups.forEach((section, group) -> category(builder, section.substring(0, section.indexOf('/'))).addEntry(group.build()));
        return builder.build();
    }

    private static ConfigCategory category(ConfigBuilder builder, String section) {
        return builder.getOrCreateCategory(Text.literal(section.isEmpty() ? "general" : section));
    }

    private static AbstractConfigListEntry<?> field(ConfigEntryBuilder eb, IniEditor.Entry entry, String value, Map<String, String> changed) {
        Text name = Text.literal(entry.key());
        Text[] tooltip = entry.comment().stream().map(Text::literal).toArray(Text[]::new);
        String def = entry.defaultValue();
        Consumer<Object> save = newValue -> {
            if (!same(String.valueOf(newValue), value)) {
                changed.put(entry.path(), String.valueOf(newValue));
            }
        };

        if (def.equalsIgnoreCase("true") || def.equalsIgnoreCase("false")) {
            return eb.startBooleanToggle(name, Boolean.parseBoolean(value))
                    .setDefaultValue(Boolean.parseBoolean(def))
                    .setTooltip(tooltip).setSaveConsumer(save::accept).requireRestart().build();
        }
        Integer defInt = parseInt(def);
        if (defInt != null) {
            Integer current = parseInt(value);
            return eb.startIntField(name, current != null ? current : defInt)
                    .setDefaultValue(defInt)
                    .setTooltip(tooltip).setSaveConsumer(save::accept).requireRestart().build();
        }
        Double defDouble = parseDouble(def);
        if (defDouble != null) {
            Double current = parseDouble(value);
            return eb.startDoubleField(name, current != null ? current : defDouble)
                    .setDefaultValue(defDouble)
                    .setTooltip(tooltip).setSaveConsumer(save::accept).requireRestart().build();
        }
        return eb.startStrField(name, value)
                .setDefaultValue(def)
                .setTooltip(tooltip).setSaveConsumer(save::accept).requireRestart().build();
    }

    /** Whether two values mean the same, so that 1 and 1.0 do not count as a change. */
    private static boolean same(String a, String b) {
        if (a.equals(b.trim())) {
            return true;
        }
        Double x = parseDouble(a);
        Double y = parseDouble(b);
        return x != null && x.equals(y);
    }

    private static void save(Map<String, String> changed) {
        if (changed.isEmpty()) {
            return;
        }
        try {
            MainConfig.save(changed);
        } catch (IOException e) {
            LOG.error("[IC2] Failed to save the config", e);
        }
        changed.clear();
    }

    private static Integer parseInt(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Double parseDouble(String s) {
        try {
            return Double.parseDouble(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
