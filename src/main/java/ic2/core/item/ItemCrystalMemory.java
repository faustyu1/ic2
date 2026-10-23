package ic2.core.item;

import ic2.core.uu.UuGraph;
import java.util.List;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Fabric port of ItemCrystalMemory: holds one UU pattern, written by the scanner or the pattern
 * storage. The pattern is kept as the item id under {@code Pattern} in the stack's data - the item
 * kind is all a pattern stands for once metadata is gone.
 */
public class ItemCrystalMemory extends ItemIC2 {
    public ItemCrystalMemory() {
        super("crystal_memory", new Item.Settings().maxCount(1));
    }

    public ItemStack readItemStack(ItemStack stack) {
        NbtCompound nbt = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
        String id = nbt.getCompound("Pattern").getString("id");
        if (id.isEmpty()) {
            return ItemStack.EMPTY;
        }
        Identifier identifier = Identifier.tryParse(id);
        return identifier == null || !Registries.ITEM.containsId(identifier) ? ItemStack.EMPTY : new ItemStack(Registries.ITEM.get(identifier));
    }

    public void writecontentsTag(ItemStack stack, ItemStack recorded) {
        NbtCompound nbt = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
        NbtCompound pattern = new NbtCompound();
        pattern.putString("id", Registries.ITEM.getId(recorded.getItem()).toString());
        nbt.put("Pattern", pattern);
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        ItemStack recorded = readItemStack(stack);
        if (recorded.isEmpty()) {
            tooltip.add(Text.translatable("item.CrystalMemory.tooltip.Empty"));
            return;
        }
        tooltip.add(Text.translatable("item.CrystalMemory.tooltip.Item").append(" ").append(recorded.getName()));
        tooltip.add(Text.translatable("item.CrystalMemory.tooltip.UU-Matter")
                .append(" " + ic2.core.util.Util.toSiString(UuGraph.getInBuckets(recorded), 4) + "B"));
    }
}
