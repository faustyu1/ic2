package ic2.core.item;

import ic2.core.init.IC2ItemVariants;
import ic2.core.util.FluidContainers;
import ic2.core.util.LiquidUtil;
import java.util.Map;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.base.EmptyItemFluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.base.FullItemFluidStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.ResourceAmount;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantItemStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;

/**
 * The IC2 fluid containers as Fabric item fluid storage ({@link FluidStorage#ITEM}), so other
 * mods' tanks and pipes can fill and empty them - Forge reached them through the fluid handler
 * item capability.
 *
 * <p>A cell holds one bucket and swaps between the empty cell and the cell of its fluid, like a
 * bucket does. An item with a tank of its own ({@link IItemFluidTank}: the jetpack, the CF pack,
 * the foam sprayer) is filled and emptied bit by bit.
 */
public final class ItemFluidStorages {
    private ItemFluidStorages() {
    }

    /** Runs once the items are registered. */
    public static void register() {
        Item emptyCell = IC2ItemVariants.get("fluid_cell", null);
        for (Map.Entry<String, Item> entry : IC2ItemVariants.getAll().entrySet()) {
            if (emptyCell == null || !entry.getKey().startsWith("fluid_cell#")) {
                continue;
            }
            Fluid fluid = LiquidUtil.getFluid(entry.getKey().substring("fluid_cell#".length()));
            if (fluid == null) {
                continue;
            }
            Item fullCell = entry.getValue();
            FluidStorage.combinedItemApiProvider(emptyCell).register(context ->
                    new EmptyItemFluidStorage(context, fullCell, fluid, FluidContainers.CELL));
            FluidStorage.ITEM.registerForItems((stack, context) ->
                    new FullItemFluidStorage(context, emptyCell, FluidVariant.of(fluid), FluidContainers.CELL), fullCell);
        }

        for (Item item : Registries.ITEM) {
            if (item instanceof IItemFluidTank tank) {
                FluidStorage.ITEM.registerForItems((stack, context) -> new TankItemStorage(context, tank), item);
            }
        }
    }

    /** The tank of an {@link IItemFluidTank}, kept in the item's data. */
    private static final class TankItemStorage extends SingleVariantItemStorage<FluidVariant> {
        private final ContainerItemContext context;
        private final IItemFluidTank tank;

        TankItemStorage(ContainerItemContext context, IItemFluidTank tank) {
            super(context);
            this.context = context;
            this.tank = tank;
        }

        @Override
        protected FluidVariant getBlankResource() {
            return FluidVariant.blank();
        }

        @Override
        protected FluidVariant getResource(ItemVariant item) {
            ResourceAmount<FluidVariant> content = tank.getFluid(item.toStack());
            return content == null ? FluidVariant.blank() : content.resource();
        }

        @Override
        protected long getAmount(ItemVariant item) {
            return tank.getAmount(item.toStack());
        }

        @Override
        protected long getCapacity(FluidVariant fluid) {
            return tank.getCapacity(context.getItemVariant().toStack());
        }

        @Override
        protected ItemVariant getUpdatedVariant(ItemVariant item, FluidVariant fluid, long amount) {
            ItemStack stack = item.toStack();
            tank.setFluid(stack, fluid, amount);
            return ItemVariant.of(stack);
        }

        @Override
        public long insert(FluidVariant fluid, long maxAmount, TransactionContext transaction) {
            return tank.canHold(context.getItemVariant().toStack(), fluid.getFluid()) ? super.insert(fluid, maxAmount, transaction) : 0;
        }
    }
}
