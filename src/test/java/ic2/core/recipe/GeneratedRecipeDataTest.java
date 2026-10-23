package ic2.core.recipe;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.gson.JsonArray;
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
import java.util.TreeSet;
import org.junit.Test;

/**
 * Checks the datapack files produced by {@code tools/gen_crafting_recipes.py} and
 * {@code tools/gen_oredict_tags.py}, so a regeneration that breaks them fails the build rather
 * than silently dropping recipes at load time.
 */
public class GeneratedRecipeDataTest {
    private static final File RESOURCES = new File("src/main/resources");
    private static final File RECIPES = new File(RESOURCES, "data/ic2/recipe");

    @Test
    public void generatedRecipesExist() {
        assertTrue("run tools/gen_crafting_recipes.py", RECIPES.isDirectory());
        assertTrue("expected the full IC2 recipe set", listRecipes().size() > 400);
    }

    @Test
    public void everyShapedRecipeIsWellFormed() throws IOException {
        List<String> problems = new ArrayList<>();

        for (File file : listRecipes()) {
            JsonObject recipe = read(file);
            String type = recipe.get("type").getAsString();
            if (type.equals("ic2:jetpack_attachment")) {
                continue; // a special recipe: no pattern, no fixed result
            }

            if (type.equals("minecraft:crafting_shaped")) {
                JsonArray pattern = recipe.getAsJsonArray("pattern");
                JsonObject keys = recipe.getAsJsonObject("key");

                int width = pattern.get(0).getAsString().length();
                if (width < 1 || width > 3 || pattern.size() > 3) {
                    problems.add(file.getName() + ": pattern is " + pattern.size() + "x" + width);
                }

                for (JsonElement row : pattern) {
                    String text = row.getAsString();
                    if (text.length() != width) {
                        problems.add(file.getName() + ": ragged pattern row '" + text + "'");
                    }
                    for (char c : text.toCharArray()) {
                        if (c != ' ' && !keys.has(String.valueOf(c))) {
                            problems.add(file.getName() + ": no key for '" + c + "'");
                        }
                    }
                }
            } else if (!type.equals("minecraft:crafting_shapeless")) {
                problems.add(file.getName() + ": unexpected type " + type);
            } else {
                JsonArray ingredients = recipe.getAsJsonArray("ingredients");
                if (ingredients.isEmpty() || ingredients.size() > 9) {
                    problems.add(file.getName() + ": " + ingredients.size() + " ingredients");
                }
            }

            JsonObject result = recipe.getAsJsonObject("result");
            if (!result.has("id") || result.get("id").getAsString().isEmpty()) {
                problems.add(file.getName() + ": no result id");
            }
        }

        assertTrue(String.valueOf(problems), problems.isEmpty());
    }

    @Test
    public void everyReferencedIc2TagHasAFile() throws IOException {
        TreeSet<String> missing = new TreeSet<>();

        for (File file : listRecipes()) {
            for (String tag : collectTags(read(file))) {
                String namespace = tag.substring(0, tag.indexOf(':'));
                String path = tag.substring(tag.indexOf(':') + 1);
                if (namespace.equals("minecraft")) {
                    continue;
                }
                File tagFile = new File(RESOURCES, "data/" + namespace + "/tags/item/" + path + ".json");
                if (!tagFile.isFile()) {
                    missing.add(tag + " (" + file.getName() + ")");
                }
            }
        }

        assertTrue("recipes reference tags without a tag file: " + missing, missing.isEmpty());
    }

    @Test
    public void ingredientsUseItemOrTagEntries() throws IOException {
        List<String> problems = new ArrayList<>();

        for (File file : listRecipes()) {
            for (JsonObject entry : collectEntries(read(file))) {
                boolean hasItem = entry.has("item");
                boolean hasTag = entry.has("tag");
                if (hasItem == hasTag) {
                    problems.add(file.getName() + ": " + entry);
                }
            }
        }

        assertTrue(String.valueOf(problems), problems.isEmpty());
    }

    @Test
    public void resultCountIsOmittedWhenSingle() throws IOException {
        JsonObject recipe = read(new File(RECIPES, "reactor_vessel.json"));
        JsonObject result = recipe.getAsJsonObject("result");

        assertEquals("ic2:reactor_vessel", result.get("id").getAsString());
        assertEquals(4, result.get("count").getAsInt());

        JsonObject single = read(new File(RECIPES, "batbox.json"));
        assertFalse(single.getAsJsonObject("result").has("count"));
    }

    private static List<File> listRecipes() {
        File[] files = RECIPES.listFiles((dir, name) -> name.endsWith(".json"));
        return files == null ? List.of() : List.of(files);
    }

    private static JsonObject read(File file) throws IOException {
        try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    /** All ingredient entries of a recipe, flattening the "one of several" lists. */
    private static List<JsonObject> collectEntries(JsonObject recipe) {
        List<JsonObject> ret = new ArrayList<>();
        List<JsonElement> raw = new ArrayList<>();

        if (recipe.has("key")) {
            recipe.getAsJsonObject("key").entrySet().forEach(e -> raw.add(e.getValue()));
        }
        if (recipe.has("ingredients")) {
            recipe.getAsJsonArray("ingredients").forEach(raw::add);
        }

        for (JsonElement element : raw) {
            if (element.isJsonArray()) {
                element.getAsJsonArray().forEach(inner -> ret.add(inner.getAsJsonObject()));
            } else {
                ret.add(element.getAsJsonObject());
            }
        }
        return ret;
    }

    private static List<String> collectTags(JsonObject recipe) {
        List<String> ret = new ArrayList<>();
        for (JsonObject entry : collectEntries(recipe)) {
            if (entry.has("tag")) {
                ret.add(entry.get("tag").getAsString());
            }
        }
        return ret;
    }
}
