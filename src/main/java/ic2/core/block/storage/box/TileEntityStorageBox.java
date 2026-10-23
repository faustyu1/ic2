package ic2.core.block.storage.box;

import ic2.core.block.TileEntityInventory;
import ic2.core.block.invslot.InvSlot;
import net.minecraft.block.BlockState;
import java.util.List;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

/**
 * Fabric port of TileEntityStorageBox - a plain box of items, sized by its material.
 *
 * <p>The single {@code inventory} slot group is what the guidef grid names, so the window follows
 * the size automatically: 27 slots become 9x3, 126 become 18x7.
 *
 * <p>Broken, the box keeps what it holds in the dropped item and puts it back when placed again,
 * the inventory written the way the block entity saves it.
 */
public abstract class TileEntityStorageBox extends TileEntityInventory {
    protected final InvSlot inventory;

    public TileEntityStorageBox(BlockEntityType<?> type, BlockPos pos, BlockState state, int inventorySize) {
        super(type, pos, state);
        this.inventory = new InvSlot(this, "inventory", InvSlot.Access.IO, inventorySize, InvSlot.InvSide.ANY);
    }

    public InvSlot getInventory() {
        return inventory;
    }

    @Override
    public boolean keepsContentsInDrop() {
        return true;
    }

    @Override
    protected ItemStack adjustDrop(ItemStack drop, boolean wrench) {
        drop = super.adjustDrop(drop, wrench);
        if (drop != null && !inventory.isEmpty() && world != null) {
            editDropData(drop, nbt -> inventory.writeToNbt(nbt, world.getRegistryManager()));
        }
        return drop;
    }

    @Override
    public void onPlaced(ItemStack stack, LivingEntity placer) {
        inventory.readFromNbt(dropData(stack), world.getRegistryManager());
    }

    @Override
    public void addInformation(ItemStack stack, List<Text> tooltip) {
        tooltip.add(Text.literal("Stores items even when broken"));
        tooltip.add(Text.literal("Inventory size: " + inventory.size()));
    }
}
