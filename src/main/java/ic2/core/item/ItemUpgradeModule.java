package ic2.core.item;

import ic2.api.upgrade.IFullUpgrade;
import ic2.api.upgrade.IUpgradableBlock;
import ic2.api.upgrade.UpgradableProperty;
import ic2.api.upgrade.UpgradeRegistry;
import ic2.core.item.type.UpgradeType;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

/**
 * Fabric port of ItemUpgradeModule - the machine upgrades.
 *
 * <p>Overclockers make a machine 30% faster for 60% more power per operation each, transformers
 * raise the voltage it takes by a tier, storage upgrades add 10000 EU of buffer. Ejectors push
 * the output into the inventories around the machine, pulling upgrades take input from them -
 * 1, 4, 16, 64 or 256 items a tick for a stack of 1 to 5 - on any side or, set by right clicking
 * a block face, on that side only. The redstone inverter flips the signal a machine reads, the
 * remote interface doubles the reach of a block that is used from afar.
 *
 * <p>1.12.2 had one item with the kind as metadata; each kind is its own item now,
 * {@code ic2:upgrade_<kind>}. The advanced ejector and puller only pass items matching the filter
 * stacks kept in their {@code filters} data; the hand-held screen 1.12.2 edited them with is not
 * ported, so without filters they pass everything, like the plain ones. The fluid ejector and
 * puller move 50 mB a tick, four times as much for each further upgrade in the stack.
 */
public class ItemUpgradeModule extends ItemIC2 implements IFullUpgrade {
    public final UpgradeType type;

    public ItemUpgradeModule(String name, UpgradeType type) {
        super(name);
        this.type = type;
        UpgradeRegistry.register(new ItemStack(this));
    }

    // ---------------------------------------------------------------- direction

    /** The side a directional upgrade works on, or null for every side. */
    public static Direction getDirection(ItemStack stack) {
        int dir = data(stack).getByte("dir") - 1;
        return dir < 0 || dir >= Direction.values().length ? null : Direction.byId(dir);
    }

    private static NbtCompound data(ItemStack stack) {
        return stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
    }

    /** Clicking a block face with a directional upgrade sets it to that side, again clears it. */
    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (!type.directional) {
            return ActionResult.PASS;
        }

        ItemStack stack = context.getStack();
        NbtCompound nbt = data(stack);
        byte dir = (byte) (1 + context.getSide().getId());
        nbt.putByte("dir", nbt.getByte("dir") == dir ? 0 : dir);
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));

        if (context.getWorld().isClient && context.getPlayer() != null) {
            context.getPlayer().sendMessage(Text.translatable(isEjecting() ? "tooltip.upgrade.ejector" : "tooltip.upgrade.pulling",
                    sideName(getDirection(stack))), false);
        }
        return ActionResult.SUCCESS;
    }

    private boolean isEjecting() {
        return type == UpgradeType.ejector || type == UpgradeType.advanced_ejector || type == UpgradeType.fluid_ejector;
    }

    private static Text sideName(Direction dir) {
        if (dir == null) {
            return Text.translatable("tooltip.upgrade.ejector.anyside");
        }
        return Text.translatable(switch (dir) {
            case DOWN -> "dir.bottom";
            case UP -> "dir.top";
            default -> "dir." + dir.getName();
        });
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType tooltipType) {
        int size = stack.getCount();
        switch (type) {
            case overclocker -> {
                tooltip.add(Text.translatable("tooltip.upgrade.overclocker.time", percent(Math.pow(0.7, size))));
                tooltip.add(Text.translatable("tooltip.upgrade.overclocker.power", percent(Math.pow(1.6, size))));
            }
            case transformer -> tooltip.add(Text.translatable("tooltip.upgrade.transformer", size));
            case energy_storage -> tooltip.add(Text.translatable("tooltip.upgrade.storage", 10000 * size));
            case ejector, advanced_ejector, fluid_ejector ->
                    tooltip.add(Text.translatable("tooltip.upgrade.ejector", sideName(getDirection(stack))));
            case pulling, advanced_pulling, fluid_pulling ->
                    tooltip.add(Text.translatable("tooltip.upgrade.pulling", sideName(getDirection(stack))));
            case redstone_inverter -> tooltip.add(Text.translatable("tooltip.upgrade.redstone"));
            case remote_interface -> tooltip.add(Text.translatable("tooltip.upgrade.remote_interface", size));
        }
    }

    private static String percent(double ratio) {
        return new java.text.DecimalFormat("0.##").format(100.0 * ratio);
    }

    // ---------------------------------------------------------------- what it does

    @Override
    public boolean isSuitableFor(ItemStack stack, Set<UpgradableProperty> types) {
        return switch (type) {
            case overclocker -> types.contains(UpgradableProperty.Processing) || types.contains(UpgradableProperty.Augmentable);
            case transformer -> types.contains(UpgradableProperty.Transformer);
            case energy_storage -> types.contains(UpgradableProperty.EnergyStorage);
            case ejector, advanced_ejector -> types.contains(UpgradableProperty.ItemProducing);
            case pulling, advanced_pulling -> types.contains(UpgradableProperty.ItemConsuming);
            case fluid_ejector -> types.contains(UpgradableProperty.FluidProducing);
            case fluid_pulling -> types.contains(UpgradableProperty.FluidConsuming);
            case redstone_inverter -> types.contains(UpgradableProperty.RedstoneSensitive);
            case remote_interface -> types.contains(UpgradableProperty.RemotelyAccessible);
        };
    }

    @Override public int getAugmentation(ItemStack stack, IUpgradableBlock parent) { return type == UpgradeType.overclocker ? 1 : 0; }
    @Override public int getExtraProcessTime(ItemStack stack, IUpgradableBlock parent) { return 0; }
    @Override public double getProcessTimeMultiplier(ItemStack stack, IUpgradableBlock parent) { return type == UpgradeType.overclocker ? 0.7 : 1.0; }
    @Override public int getExtraEnergyDemand(ItemStack stack, IUpgradableBlock parent) { return 0; }
    @Override public double getEnergyDemandMultiplier(ItemStack stack, IUpgradableBlock parent) { return type == UpgradeType.overclocker ? 1.6 : 1.0; }
    @Override public int getExtraEnergyStorage(ItemStack stack, IUpgradableBlock parent) { return type == UpgradeType.energy_storage ? 10000 : 0; }
    @Override public double getEnergyStorageMultiplier(ItemStack stack, IUpgradableBlock parent) { return 1.0; }
    @Override public int getExtraTier(ItemStack stack, IUpgradableBlock parent) { return type == UpgradeType.transformer ? 1 : 0; }
    @Override public boolean modifiesRedstoneInput(ItemStack stack, IUpgradableBlock parent) { return type == UpgradeType.redstone_inverter; }

    @Override
    public int getRedstoneInput(ItemStack stack, IUpgradableBlock parent, int externalInput) {
        return type == UpgradeType.redstone_inverter ? 15 - externalInput : externalInput;
    }

    @Override
    public int getRangeAmplification(ItemStack stack, IUpgradableBlock parent, int existingRange) {
        return type == UpgradeType.remote_interface ? existingRange << 1 : existingRange;
    }

    @Override
    public Collection<ItemStack> onProcessEnd(ItemStack stack, IUpgradableBlock parent, Collection<ItemStack> output) {
        return output;
    }

    /** Ejects or pulls items each tick; the other kinds act through the machine's rates. */
    @Override
    public boolean onTick(ItemStack stack, IUpgradableBlock parent) {
        if ((type == UpgradeType.fluid_ejector || type == UpgradeType.fluid_pulling)
                && parent instanceof BlockEntity te && te.getWorld() != null && !te.getWorld().isClient) {
            return moveFluid(stack, te, type == UpgradeType.fluid_ejector);
        }

        boolean eject = type == UpgradeType.ejector || type == UpgradeType.advanced_ejector;
        boolean pull = type == UpgradeType.pulling || type == UpgradeType.advanced_pulling;
        if (!(eject || pull) || !(parent instanceof BlockEntity te) || te.getWorld() == null || te.getWorld().isClient) {
            return false;
        }

        long amount = (long) Math.pow(4.0, Math.min(4, stack.getCount() - 1));
        Predicate<ItemVariant> filter = filter(stack);
        Direction only = getDirection(stack);
        World world = te.getWorld();
        boolean moved = false;

        for (Direction dir : Direction.values()) {
            if (only != null && dir != only) {
                continue;
            }

            Storage<ItemVariant> own = ItemStorage.SIDED.find(world, te.getPos(), dir);
            Storage<ItemVariant> other = ItemStorage.SIDED.find(world, te.getPos().offset(dir), dir.getOpposite());
            if (own == null || other == null) {
                continue;
            }

            long done = eject
                    ? StorageUtil.move(own, other, filter, amount, null)
                    : StorageUtil.move(other, own, filter, amount, null);
            moved |= done > 0;
        }
        return moved;
    }

    /** Pushes or pulls 50, 200, 800, 3200 or 12800 mB a tick for a stack of 1 to 5. */
    private static boolean moveFluid(ItemStack stack, BlockEntity te, boolean eject) {
        if (!ic2.core.util.LiquidUtil.isFluidTile(te, null)) {
            return false;
        }

        int amount = (int) (50.0 * Math.pow(4.0, Math.min(4, stack.getCount() - 1)));
        Direction only = getDirection(stack);
        for (Direction dir : Direction.values()) {
            if (only != null && dir != only) {
                continue;
            }
            BlockEntity other = te.getWorld().getBlockEntity(te.getPos().offset(dir));
            if (!ic2.core.util.LiquidUtil.isFluidTile(other, dir.getOpposite())) {
                continue;
            }
            if (eject) {
                ic2.core.util.LiquidUtil.transfer(te, dir, other, ic2.core.util.LiquidUtil.mb(amount));
            } else {
                ic2.core.util.LiquidUtil.transfer(other, dir.getOpposite(), te, ic2.core.util.LiquidUtil.mb(amount));
            }
        }
        return false;
    }

    /** What an advanced upgrade lets through: the items in its filter list, or anything without one. */
    private Predicate<ItemVariant> filter(ItemStack stack) {
        if (type != UpgradeType.advanced_ejector && type != UpgradeType.advanced_pulling) {
            return variant -> true;
        }

        NbtList filters = data(stack).getList("filters", NbtElement.STRING_TYPE);
        if (filters.isEmpty()) {
            return variant -> true;
        }

        Set<String> ids = new java.util.HashSet<>();
        for (int i = 0; i < filters.size(); i++) {
            ids.add(filters.getString(i));
        }
        return variant -> ids.contains(net.minecraft.registry.Registries.ITEM.getId(variant.getItem()).toString());
    }
}
