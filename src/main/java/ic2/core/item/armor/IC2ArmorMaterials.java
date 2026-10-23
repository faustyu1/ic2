package ic2.core.item.armor;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import ic2.core.init.IC2ItemVariants;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;

/**
 * The armour materials IC2 added in 1.12.2 with
 * {@code EnumHelper.addArmorMaterial(name, texture, durability, reduction, enchantability, sound, toughness)},
 * plus the materials the suits that absorb with energy need.
 *
 * <p>1.12.2 passed the reduction as {@code int[]{feet, legs, chest, head}}; 1.21.1 wants a map keyed
 * by {@link ArmorItem.Type}, and the durability multiplier is applied per type instead of being a
 * single number, so the numbers below are the same values rearranged.
 *
 * <p>The suits that run on energy ({@code nano}, {@code quantum}, the packs, hazmat) get zero
 * vanilla protection on purpose: they absorb through {@link ic2.api.item.ISpecialArmor}, and giving
 * them armour points as well would count their protection twice.
 */
public final class IC2ArmorMaterials {
    /** {@code IC2_BRONZE}: iron's protection, 15x durability, enchantability 9. */
    public static RegistryEntry<ArmorMaterial> BRONZE;
    /** {@code IC2_ALLOY}: better than diamond, 50x durability, toughness 2. */
    public static RegistryEntry<ArmorMaterial> ALLOY;
    /** Hazmat suit and rubber boots - protection comes from {@code ItemArmorHazmat}. */
    public static RegistryEntry<ArmorMaterial> HAZMAT;
    public static RegistryEntry<ArmorMaterial> RUBBER;
    /** Energy suits - protection comes from {@code ItemArmorElectric}. */
    public static RegistryEntry<ArmorMaterial> NANO;
    public static RegistryEntry<ArmorMaterial> QUANTUM;
    /** Chest-slot packs and the utility head/feet pieces. */
    public static RegistryEntry<ArmorMaterial> BATPACK;
    public static RegistryEntry<ArmorMaterial> ADVANCED_BATPACK;
    public static RegistryEntry<ArmorMaterial> ENERGYPACK;
    public static RegistryEntry<ArmorMaterial> LAPPACK;
    public static RegistryEntry<ArmorMaterial> JETPACK;
    public static RegistryEntry<ArmorMaterial> CF_PACK;
    public static RegistryEntry<ArmorMaterial> SOLAR;
    public static RegistryEntry<ArmorMaterial> NIGHTVISION;

    private static boolean registered;

    private IC2ArmorMaterials() {}

    public static synchronized void registerAll() {
        if (registered) return;
        registered = true;

        BRONZE = register("bronze", protection(2, 5, 6, 2), 9, 0.0f, 0.0f,
                () -> ingredient("ingot", "bronze"));
        ALLOY = register("alloy", protection(4, 7, 9, 4), 12, 2.0f, 0.0f,
                () -> ingredient("crafting", "alloy"));

        HAZMAT = register("hazmat", protection(0, 0, 0, 0), 0, 0.0f, 0.0f, Ingredient::empty);
        RUBBER = register("rubber", protection(0, 0, 0, 0), 0, 0.0f, 0.0f, Ingredient::empty);
        // the suits wear vanilla armour points on top of their EU absorption: the nano suit those
        // of netherite, the quantum suit more again
        NANO = register("nano", protection(3, 6, 8, 3), 0, 3.0f, 0.1f, Ingredient::empty);
        QUANTUM = register("quantum", protection(4, 8, 10, 4), 0, 4.0f, 0.15f, Ingredient::empty);
        BATPACK = register("batpack", protection(0, 0, 0, 0), 0, 0.0f, 0.0f, Ingredient::empty);
        ADVANCED_BATPACK = register("advbatpack", protection(0, 0, 0, 0), 0, 0.0f, 0.0f, Ingredient::empty);
        ENERGYPACK = register("energypack", protection(0, 0, 0, 0), 0, 0.0f, 0.0f, Ingredient::empty);
        LAPPACK = register("lappack", protection(0, 0, 0, 0), 0, 0.0f, 0.0f, Ingredient::empty);
        JETPACK = register("jetpack", protection(0, 0, 0, 0), 0, 0.0f, 0.0f, Ingredient::empty);
        // the CF pack wore the batpack texture in 1.12.2
        CF_PACK = register("cfpack", "batpack", protection(0, 0, 0, 0), 0, 0.0f, 0.0f, Ingredient::empty);
        SOLAR = register("solar", protection(0, 0, 0, 0), 0, 0.0f, 0.0f, Ingredient::empty);
        NIGHTVISION = register("nightvision", protection(0, 0, 0, 0), 0, 0.0f, 0.0f, Ingredient::empty);
    }

    private static Map<ArmorItem.Type, Integer> protection(int feet, int legs, int chest, int head) {
        EnumMap<ArmorItem.Type, Integer> map = new EnumMap<>(ArmorItem.Type.class);
        map.put(ArmorItem.Type.BOOTS, feet);
        map.put(ArmorItem.Type.LEGGINGS, legs);
        map.put(ArmorItem.Type.CHESTPLATE, chest);
        map.put(ArmorItem.Type.HELMET, head);
        map.put(ArmorItem.Type.BODY, chest);
        return map;
    }

    private static RegistryEntry<ArmorMaterial> register(String name, Map<ArmorItem.Type, Integer> defense,
                                                         int enchantability, float toughness, float knockbackResistance,
                                                         Supplier<Ingredient> repair) {
        return register(name, name, defense, enchantability, toughness, knockbackResistance, repair);
    }

    private static RegistryEntry<ArmorMaterial> register(String name, String texture, Map<ArmorItem.Type, Integer> defense,
                                                         int enchantability, float toughness, float knockbackResistance,
                                                         Supplier<Ingredient> repair) {
        Identifier id = Identifier.of("ic2", name);
        Identifier textureId = Identifier.of("ic2", texture);
        RegistryEntry<SoundEvent> equipSound = SoundEvents.ITEM_ARMOR_EQUIP_IRON;
        // the layer texture, assets/ic2/textures/models/armor/<name>_layer_{1,2}.png
        List<ArmorMaterial.Layer> layers = List.of(new ArmorMaterial.Layer(textureId));

        ArmorMaterial material = new ArmorMaterial(defense, enchantability, equipSound, repair, layers,
                toughness, knockbackResistance);
        Registry.register(Registries.ARMOR_MATERIAL, id, material);
        return Registries.ARMOR_MATERIAL.getEntry(material);
    }

    private static Ingredient ingredient(String group, String name) {
        Item item = IC2ItemVariants.get(group, name);
        return item == null ? Ingredient.empty() : Ingredient.ofStacks(new ItemStack(item));
    }
}
