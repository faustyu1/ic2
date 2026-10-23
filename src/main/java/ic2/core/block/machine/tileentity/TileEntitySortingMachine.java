package ic2.core.block.machine.tileentity;

import ic2.api.upgrade.IUpgradableBlock;
import ic2.api.upgrade.UpgradableProperty;
import ic2.core.block.invslot.InvSlot;
import ic2.core.block.invslot.InvSlotDischarge;
import ic2.core.block.invslot.InvSlotUpgrade;
import ic2.core.init.IC2Blocks;
import ic2.core.util.AdjacentInventories;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * Fabric port of TileEntitySortingMachine - sends the items of its 11 slot buffer to the
 * inventories around it by a white list per side.
 *
 * <p>Each side has seven filter slots. A filter is a ghost stack whose size is the batch: the
 * machine only sends a stack of that size, and only when the whole batch fits next door, costing
 * 20 EU an item. Items no filter names leave through the default route (down unless set in the
 * window), one item per buffer slot and tick, also for 20 EU. Below 20 EU it does nothing. It
 * stores 15000 EU at tier 2, more with transformer upgrades.
 */
public class TileEntitySortingMachine extends TileEntityElectricMachine implements IUpgradableBlock, InvSlotUpgrade.Listener {
    public static final int DEFAULT_TIER = 2;
    /** EU per item sorted. */
    public static final int ENERGY_PER_ITEM = 20;

    public final InvSlotUpgrade upgradeSlot;
    public final InvSlot buffer;
    public final InvSlotDischarge dischargeSlot;
    /** Seven filters for each side, by direction id (down, up, north, south, west, east). */
    private final ItemStack[][] filters = new ItemStack[6][7];
    public Direction defaultRoute = Direction.DOWN;

    public TileEntitySortingMachine(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type == null ? IC2Blocks.SORTING_BE : type, pos, state, 15000, DEFAULT_TIER);
        this.upgradeSlot = new InvSlotUpgrade(this, "upgrade", 3);
        this.buffer = new InvSlot(this, "Buffer", InvSlot.Access.IO, 11);
        this.dischargeSlot = new InvSlotDischarge(this, "discharge", DEFAULT_TIER);
        for (ItemStack[] side : filters) {
            Arrays.fill(side, ItemStack.EMPTY);
        }
    }

    public TileEntitySortingMachine(BlockPos pos, BlockState state) {
        this(null, pos, state);
    }

    @Override
    public void tick() {
        if (world == null || world.isClient) {
            return;
        }
        drainDischargeSlot(dischargeSlot);

        nextSlot:
        for (int index = 0; index < buffer.size(); index++) {
            if (energyStored < ENERGY_PER_ITEM) {
                return;
            }

            ItemStack stack = buffer.get(index);
            if (stack.isEmpty()) {
                continue;
            }

            for (AdjacentInventories.AdjacentInv inv : AdjacentInventories.getAll(this)) {
                if (inv.dir() != defaultRoute) {
                    // the first filter of the side that matches decides, whether its batch fits or not
                    for (ItemStack filter : getFilterSlots(inv.dir())) {
                        if (filter.isEmpty()) {
                            continue;
                        }
                        int filterSize = filter.getCount();
                        if (stack.getCount() >= filterSize && ItemStack.areItemsAndComponentsEqual(filter, stack)
                                && energyStored >= filterSize * ENERGY_PER_ITEM) {
                            ItemStack transfer = stack.copyWithCount(filterSize);
                            if (AdjacentInventories.put(inv, transfer, true) == filterSize) {
                                int amount = AdjacentInventories.put(inv, transfer, false);
                                stack = stack.copyWithCount(stack.getCount() - amount);
                                buffer.put(index, stack);
                                useEnergy(amount * ENERGY_PER_ITEM);
                                if (stack.isEmpty()) {
                                    continue nextSlot;
                                }
                            }
                            break;
                        }
                    }
                } else if (!inAnyFilter(stack)) {
                    // the default route takes one item of what no side asks for
                    int amount = AdjacentInventories.put(inv, stack.copyWithCount(1), false);
                    if (amount > 0) {
                        stack = stack.copyWithCount(stack.getCount() - amount);
                        buffer.put(index, stack);
                        useEnergy(ENERGY_PER_ITEM);
                    }
                    continue nextSlot;
                }
            }
        }
    }

    private boolean inAnyFilter(ItemStack stack) {
        for (ItemStack[] side : filters) {
            for (ItemStack filter : side) {
                if (!filter.isEmpty() && ItemStack.areItemsAndComponentsEqual(filter, stack)) {
                    return true;
                }
            }
        }
        return false;
    }

    public ItemStack[] getFilterSlots(Direction side) {
        return filters[side.getId()];
    }

    @Override
    public ItemStack[] getHologramSlots(String name) {
        if (name.startsWith("filter_")) {
            int side = Integer.parseInt(name.substring(7));
            return side >= 0 && side < filters.length ? filters[side] : null;
        }
        return super.getHologramSlots(name);
    }

    /** {@code route:<direction id>} sets the default route. */
    @Override
    public void onGuiEvent(PlayerEntity player, String event) {
        if (event.startsWith("route:")) {
            try {
                int id = Integer.parseInt(event.substring(6));
                if (id >= 0 && id <= 5) {
                    defaultRoute = Direction.byId(id);
                    markDirty();
                }
            } catch (NumberFormatException e) {
                // a malformed event changes nothing
            }
        }
    }

    /** {@code route_<id>}: that side is the default route; {@code adj_<id>}: an inventory is there. */
    @Override
    public boolean getGuiCondition(String name) {
        if (name.startsWith("route_")) {
            return defaultRoute.getId() == Integer.parseInt(name.substring(6));
        }
        if (name.startsWith("adj_")) {
            return AdjacentInventories.get(this, Direction.byId(Integer.parseInt(name.substring(4)))) != null;
        }
        return super.getGuiCondition(name);
    }

    @Override
    public void onUpgradesChanged() {
        setTier(upgradeSlot.getTier(DEFAULT_TIER));
    }

    @Override
    public Set<UpgradableProperty> getUpgradableProperties() {
        return EnumSet.of(UpgradableProperty.Transformer);
    }

    @Override
    public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.writeNbt(nbt, registries);
        NbtList filtersTag = new NbtList();
        for (int i = 0; i < 42; i++) {
            ItemStack stack = filters[i / 7][i % 7];
            if (!stack.isEmpty()) {
                NbtCompound contentTag = (NbtCompound) stack.encode(registries);
                contentTag.putByte("index", (byte) i);
                filtersTag.add(contentTag);
            }
        }
        nbt.put("filters", filtersTag);
        nbt.putByte("defaultroute", (byte) defaultRoute.getId());
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.readNbt(nbt, registries);
        for (ItemStack[] side : filters) {
            Arrays.fill(side, ItemStack.EMPTY);
        }
        NbtList filtersTag = nbt.getList("filters", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < filtersTag.size(); i++) {
            NbtCompound filterTag = filtersTag.getCompound(i);
            int index = filterTag.getByte("index") & 255;
            if (index < 42) {
                filters[index / 7][index % 7] = ItemStack.fromNbt(registries, filterTag).orElse(ItemStack.EMPTY);
            }
        }
        int route = nbt.getByte("defaultroute");
        if (route >= 0 && route < 6) {
            defaultRoute = Direction.byId(route);
        }
    }
}
