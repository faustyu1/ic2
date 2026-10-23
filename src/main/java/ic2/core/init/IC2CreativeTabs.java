package ic2.core.init;

import ic2.api.item.ElectricItem;
import ic2.api.item.IElectricItem;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class IC2CreativeTabs {
    public static ItemGroup IC2;

    private IC2CreativeTabs(){}

    public static void register(){
        IC2 = FabricItemGroup.builder()
            .icon(() -> new ItemStack(IC2Items.WRENCH))
            .displayName(Text.translatable("itemGroup.ic2"))
            .entries((ctx, entries) -> {
                // All ic2 items (including BlockItems) - Fabric 1.21.1
                for (var item : Registries.ITEM) {
                    Identifier id = Registries.ITEM.getId(item);
                    if (id != null && "ic2".equals(id.getNamespace())) {
                        // a bare seed bag holds nothing; the tab lists one bag per crop instead
                        if (item instanceof ic2.core.item.ItemCropSeed) {
                            ic2.core.item.ItemCropSeed.creativeStacks().forEach(entries::add);
                            continue;
                        }
                        entries.add(item);
                        // 1.12.2 listed every electric item twice, empty and full; without the
                        // full one there is no way to get a charged tool in creative
                        ItemStack charged = chargedCopy(item);
                        if (charged != null) {
                            entries.add(charged);
                        }
                        // the same for the items with a tank of their own, empty and full
                        if (item instanceof ic2.core.item.IItemFluidTank tank && tank.getCreativeFluid() != null) {
                            entries.add(tank.filledCopy(new ItemStack(item), tank.getCreativeFluid()));
                        }
                    }
                }
            })
            .build();
        Registry.register(Registries.ITEM_GROUP, Identifier.of("ic2", "tab"), IC2);
    }

    /** A full stack of {@code item}, or null when the item does not hold EU. */
    private static ItemStack chargedCopy(net.minecraft.item.Item item) {
        if (!(item instanceof IElectricItem electric) || ElectricItem.manager == null) {
            return null;
        }

        ItemStack stack = new ItemStack(item);
        double maxCharge = electric.getMaxCharge(stack);
        if (maxCharge <= 0.0) {
            return null;
        }

        ElectricItem.manager.charge(stack, maxCharge, Integer.MAX_VALUE, true, false);
        return stack;
    }
}
