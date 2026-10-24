package ic2.core.recipe.data;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.input.SingleStackRecipeInput;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.world.World;

/**
 * A machine recipe from a datapack, one of the lines the 1.12.2 machine configs held:
 *
 * <pre>
 * {"type": "ic2:thermal_centrifuge", "ingredient": {"tag": "ic2:crushed/iron"}, "count": 1,
 *  "results": [{"id": "ic2:dust_small_gold"}, {"id": "ic2:dust_iron"}], "metadata": {"minHeat": 1500}}
 * </pre>
 *
 * <p>{@code count} is how many items the machine takes at once, and {@code metadata} the recipe
 * attributes under the names the machines read them by: {@code minHeat}, {@code amount} (ore
 * washing water), {@code fluid} and {@code duration} (blast furnace), {@code hardness} (block
 * cutter) and {@code ignoreSameInputOutput}. The machines work from their recipe managers, which
 * {@link MachineRecipeSync} fills from these.
 */
public class IC2MachineRecipe implements Recipe<SingleStackRecipeInput> {
    public final MachineRecipeKind kind;
    public final Ingredient ingredient;
    public final int count;
    public final List<ItemStack> results;
    public final NbtCompound metadata;

    public IC2MachineRecipe(MachineRecipeKind kind, Ingredient ingredient, int count, List<ItemStack> results, NbtCompound metadata) {
        this.kind = kind;
        this.ingredient = ingredient;
        this.count = count;
        this.results = results;
        this.metadata = metadata;
    }

    @Override
    public boolean matches(SingleStackRecipeInput input, World world) {
        ItemStack stack = input.item();
        return ingredient.test(stack) && stack.getCount() >= count;
    }

    @Override
    public ItemStack craft(SingleStackRecipeInput input, RegistryWrapper.WrapperLookup lookup) {
        return results.get(0).copy();
    }

    @Override
    public boolean fits(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResult(RegistryWrapper.WrapperLookup lookup) {
        return results.get(0);
    }

    @Override
    public DefaultedList<Ingredient> getIngredients() {
        return DefaultedList.copyOf(Ingredient.EMPTY, ingredient);
    }

    /** The machines are no crafting table: the recipe book leaves these out. */
    @Override
    public boolean isIgnoredInRecipeBook() {
        return true;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return kind.serializer();
    }

    @Override
    public RecipeType<?> getType() {
        return kind.type();
    }

    public static final class Serializer implements RecipeSerializer<IC2MachineRecipe> {
        private final MapCodec<IC2MachineRecipe> codec;
        private final PacketCodec<RegistryByteBuf, IC2MachineRecipe> packetCodec;

        Serializer(MachineRecipeKind kind) {
            this.codec = RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Ingredient.DISALLOW_EMPTY_CODEC.fieldOf("ingredient").forGetter(r -> r.ingredient),
                    Codecs.POSITIVE_INT.optionalFieldOf("count", 1).forGetter(r -> r.count),
                    ItemStack.VALIDATED_CODEC.listOf(1, Integer.MAX_VALUE).fieldOf("results").forGetter(r -> r.results),
                    NbtCompound.CODEC.optionalFieldOf("metadata", new NbtCompound()).forGetter(r -> r.metadata)
            ).apply(instance, (ingredient, count, results, metadata) -> new IC2MachineRecipe(kind, ingredient, count, results, metadata)));
            this.packetCodec = PacketCodec.tuple(
                    Ingredient.PACKET_CODEC, r -> r.ingredient,
                    PacketCodecs.VAR_INT, r -> r.count,
                    ItemStack.PACKET_CODEC.collect(PacketCodecs.toList()), r -> r.results,
                    PacketCodecs.NBT_COMPOUND, r -> r.metadata,
                    (ingredient, count, results, metadata) -> new IC2MachineRecipe(kind, ingredient, count, results, metadata));
        }

        @Override
        public MapCodec<IC2MachineRecipe> codec() {
            return codec;
        }

        @Override
        public PacketCodec<RegistryByteBuf, IC2MachineRecipe> packetCodec() {
            return packetCodec;
        }
    }
}
