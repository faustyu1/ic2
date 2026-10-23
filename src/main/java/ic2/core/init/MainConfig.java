package ic2.core.init;

import ic2.core.util.Config;
import ic2.core.util.IniEditor;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fabric port of ic2.core.init.MainConfig - {@code assets/ic2/config/general.ini}, overridden by the
 * player's {@code config/IC2.ini}.
 *
 * <p>As in 1.12.2 the user file is created from the shipped one, comments and all, when it does not
 * exist yet, and its values are read over the shipped defaults; a key the user file lacks, such as
 * one added by a later version, keeps its default. Unlike the original the user file is not
 * rewritten afterwards, so what the player wrote stays as written.
 */
public class MainConfig {
    private static final Logger LOG = LoggerFactory.getLogger("IC2-Config");
    private static final String PATH = "/assets/ic2/config/general.ini";
    private static final String USER_FILE = "IC2.ini";

    private static Config instance;

    /** Loads the config on first use. Never returns null - a failed read yields an empty config. */
    public static synchronized Config get() {
        if (instance == null) {
            instance = new Config("general");
            try (InputStream is = MainConfig.class.getResourceAsStream(PATH)) {
                if (is == null) {
                    throw new IOException("Resource not found: " + PATH);
                }
                instance.load(is);
                LOG.info("[IC2] Loaded {} settings from general.ini", instance.size());
            } catch (IOException e) {
                LOG.error("[IC2] Failed to read general.ini, falling back to built-in defaults", e);
            }
            loadUserFile(instance);
        }
        return instance;
    }

    /** The settings the shipped file has, with their comments, for the settings screen. */
    public static List<IniEditor.Entry> entries() throws IOException {
        try (InputStream is = MainConfig.class.getResourceAsStream(PATH)) {
            if (is == null) {
                throw new IOException("Resource not found: " + PATH);
            }
            return IniEditor.readEntries(is);
        }
    }

    /**
     * What the files hold now, read afresh: unlike {@link #get} this sees edits made to the user
     * file since the game started.
     */
    public static Config read() throws IOException {
        Config config = new Config("general");
        try (InputStream is = MainConfig.class.getResourceAsStream(PATH)) {
            if (is == null) {
                throw new IOException("Resource not found: " + PATH);
            }
            config.load(is);
        }
        Path file = userFile();
        if (Files.exists(file)) {
            try (InputStream is = Files.newInputStream(file)) {
                config.merge(is);
            }
        }
        return config;
    }

    /**
     * Writes values, by path, into the user file. The game reads them on its next start: the
     * settings loaded now stay as they are.
     */
    public static void save(Map<String, String> values) throws IOException {
        Path file = userFile();
        if (!Files.exists(file)) {
            Files.createDirectories(file.getParent());
            try (InputStream is = MainConfig.class.getResourceAsStream(PATH)) {
                if (is != null) {
                    Files.copy(is, file);
                }
            }
        }
        IniEditor.write(file, values);
        LOG.info("[IC2] Saved {} settings to {}", values.size(), file);
    }

    private static Path userFile() {
        return net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve(USER_FILE);
    }

    private static void loadUserFile(Config config) {
        Path file;
        try {
            file = userFile();
        } catch (Throwable t) {
            // unit tests run without a Fabric loader context, and so without a config folder
            return;
        }

        try {
            if (!Files.exists(file)) {
                Files.createDirectories(file.getParent());
                try (InputStream is = MainConfig.class.getResourceAsStream(PATH)) {
                    if (is != null) {
                        Files.copy(is, file);
                        LOG.info("[IC2] Wrote the default config to {}", file);
                    }
                }
                return;
            }
            try (InputStream is = Files.newInputStream(file)) {
                config.merge(is);
            }
            LOG.info("[IC2] Read the user config {}", file);
        } catch (IOException e) {
            LOG.error("[IC2] Failed to read or write the user config {}, using the defaults", file, e);
        }
    }
}
