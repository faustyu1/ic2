package ic2.core.init;

import static org.junit.Assert.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import org.junit.Test;

/**
 * Checks the item models {@code tools/gen_item_models.py} writes: a model pointing at a texture
 * that is not there renders as the missing-texture cube, which is exactly the failure the
 * generator exists to prevent.
 */
public class ItemModelDataTest {
    private static final File ASSETS = new File("src/main/resources/assets/ic2");
    private static final File MODELS = new File(ASSETS, "models/item");

    @Test
    public void everyItemModelExists() {
        assertTrue("run tools/gen_item_models.py", MODELS.isDirectory());
        assertTrue("expected a model per flattened item", listModels().size() > 400);
    }

    @Test
    public void everyModelTextureExists() throws IOException {
        TreeSet<String> missing = new TreeSet<>();

        for (File file : listModels()) {
            JsonObject model = read(file);
            JsonObject textures = model.getAsJsonObject("textures");
            if (textures == null) {
                continue;
            }

            for (Map.Entry<String, JsonElement> texture : textures.entrySet()) {
                String value = texture.getValue().getAsString();
                if (!value.startsWith("ic2:")) {
                    continue;
                }
                if (!new File(ASSETS, "textures/" + value.substring("ic2:".length()) + ".png").isFile()) {
                    missing.add(file.getName() + ": " + value);
                }
            }
        }

        assertTrue("item models pointing at textures that do not exist: " + missing, missing.isEmpty());
    }

    @Test
    public void chargeOverridesPointAtModelsThatExist() throws IOException {
        TreeSet<String> broken = new TreeSet<>();

        for (File file : listModels()) {
            JsonObject model = read(file);
            if (!model.has("overrides")) {
                continue;
            }

            for (JsonElement element : model.getAsJsonArray("overrides")) {
                JsonObject override = element.getAsJsonObject();
                String target = override.get("model").getAsString();
                if (!target.startsWith("ic2:item/")) {
                    broken.add(file.getName() + ": " + target);
                    continue;
                }
                // the charge steps are textures used directly as models by the generator
                String path = target.substring("ic2:item/".length());
                if (!new File(ASSETS, "textures/item/" + path + ".png").isFile()
                        && !new File(MODELS, path + ".json").isFile()) {
                    broken.add(file.getName() + ": " + target);
                }
            }
        }

        assertTrue("charge overrides without a model or texture: " + broken, broken.isEmpty());
    }

    private static List<File> listModels() {
        List<File> ret = new ArrayList<>();
        collect(MODELS, ret);
        return ret;
    }

    private static void collect(File directory, List<File> out) {
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                collect(file, out);
            } else if (file.getName().endsWith(".json")) {
                out.add(file);
            }
        }
    }

    private static JsonObject read(File file) throws IOException {
        try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }
}
