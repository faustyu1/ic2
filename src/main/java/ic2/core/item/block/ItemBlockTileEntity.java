package ic2.core.item.block;

import ic2.core.block.BlockTileEntity;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

public class ItemBlockTileEntity extends BlockItem {
    public final Identifier identifier;
    public ItemBlockTileEntity(Block block, Identifier id){
        super(block, new Settings());
        this.identifier=id;
    }
    @Override public String getTranslationKey(ItemStack stack){ return super.getTranslationKey(stack)+"."+identifier.getPath(); }
}
