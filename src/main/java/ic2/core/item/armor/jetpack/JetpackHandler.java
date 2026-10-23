package ic2.core.item.armor.jetpack;

import ic2.api.item.ElectricItem;
import ic2.api.item.IBackupElectricItemManager;
import ic2.api.item.IElectricItem;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Equipment;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

/**
 * Port of {@code ic2.core.item.armor.jetpack.JetpackHandler}: an electric jetpack crafted onto a
 * chestplate (see {@link JetpackAttachmentRecipe}).
 *
 * <p>The chestplate flies like the electric jetpack, with the jetpack's 30k EU, tier and transfer
 * limit. A chestplate that is no electric item itself keeps that charge in its own data, which this
 * class manages as the backup electric item manager; an electric one (the nano chestplate) flies on
 * its own charge. When the chestplate breaks while worn, the jetpack drops back into the chest slot
 * with the charge it had.
 */
public final class JetpackHandler implements IBackupElectricItemManager {
    private static final String ATTACHED = "hasIC2Jetpack";
    private static final JetpackHandler INSTANCE = new JetpackHandler();
    /** The worn attached chestplate of every player, as seen at the end of the last tick. */
    private static final Map<PlayerEntity, Worn> lastWorn = new WeakHashMap<>();

    private JetpackHandler() {}

    public static void init() {
        ElectricItem.registerBackupManager(INSTANCE);
    }

    public static JetpackHandler getInstance() {
        return INSTANCE;
    }

    /** The electric jetpack, whose numbers an attached jetpack flies with. */
    public static ItemStack jetpack() {
        return new ItemStack(jetpackItem());
    }

    private static Item jetpackItem() {
        return Registries.ITEM.get(Identifier.of("ic2", "jetpack_electric"));
    }

    public static boolean isChestArmor(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getItem() instanceof Equipment equipment
                && equipment.getSlotType() == EquipmentSlot.CHEST;
    }

    public static void setJetpackAttached(ItemStack stack, boolean value) {
        if (stack == null || stack.isEmpty()) {
            return;
        }

        NbtCompound data = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
        if (!value) {
            data.remove(ATTACHED);
        } else if (isChestArmor(stack)) {
            data.putBoolean(ATTACHED, true);
        } else {
            return;
        }
        setData(stack, data);
    }

    public static boolean hasJetpackAttached(ItemStack stack) {
        return isChestArmor(stack) && stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt().getBoolean(ATTACHED);
    }

    /** Whether the stack flies: a jetpack itself or a chestplate with one attached. */
    public static boolean hasJetpack(ItemStack stack) {
        return stack != null && (hasJetpackAttached(stack) || stack.getItem() instanceof IJetpack);
    }

    public static IJetpack getJetpack(ItemStack stack) {
        return stack.getItem() instanceof IJetpack jetpack ? jetpack : (IJetpack) jetpackItem();
    }

    public static double getTransferLimit() {
        ItemStack jetpack = jetpack();
        return ((IElectricItem) jetpack.getItem()).getTransferLimit(jetpack);
    }

    /**
     * The per-tick part of the 1.12.2 player tick handler, run from the armor tick: flies an
     * attached jetpack (a jetpack item flies from its own armor tick), and gives the jetpack back
     * when its chestplate broke.
     */
    public static void tick(PlayerEntity player) {
        ItemStack stack = player.getEquippedStack(EquipmentSlot.CHEST);
        if (hasJetpackAttached(stack) && !(stack.getItem() instanceof IJetpack)) {
            JetpackLogic.onArmorTick(player.getWorld(), player, stack, getJetpack(stack));
        }

        if (player.getWorld().isClient) {
            return;
        }

        // a broken stack is emptied in place; one taken off keeps its count
        Worn worn = lastWorn.remove(player);
        if (worn != null && worn.stack.isEmpty() && stack.isEmpty()) {
            ItemStack newJetpack = jetpack();
            ElectricItem.manager.charge(newJetpack, worn.charge, Integer.MAX_VALUE, true, false);
            player.equipStack(EquipmentSlot.CHEST, newJetpack);
            stack = newJetpack;
        }

        if (hasJetpackAttached(stack)) {
            lastWorn.put(player, new Worn(stack, ElectricItem.manager.getCharge(stack)));
        }
    }

    private record Worn(ItemStack stack, double charge) {}

    // ---- the charge of a chestplate that is no electric item itself ----

    @Override
    public boolean handles(ItemStack stack) {
        return hasJetpackAttached(stack) && !(stack.getItem() instanceof IElectricItem);
    }

    @Override
    public double charge(ItemStack stack, double amount, int tier, boolean ignoreTransferLimit, boolean simulate) {
        if (getTier(stack) > tier) {
            return 0.0;
        }

        if (!ignoreTransferLimit) {
            amount = Math.min(amount, getTransferLimit());
        }

        NbtCompound data = getData(stack);
        double charge = data.getDouble("charge");
        amount = Math.max(0.0, Math.min(amount, getMaxCharge(stack) - charge));
        if (!simulate && amount > 0.0) {
            data.putDouble("charge", charge + amount);
            setData(stack, data);
        }

        return amount;
    }

    @Override
    public double discharge(ItemStack stack, double amount, int tier, boolean ignoreTransferLimit, boolean externally, boolean simulate) {
        if (externally || getTier(stack) > tier) {
            return 0.0;
        }

        if (!ignoreTransferLimit) {
            amount = Math.min(amount, getTransferLimit());
        }

        NbtCompound data = getData(stack);
        double charge = data.getDouble("charge");
        amount = Math.max(0.0, Math.min(amount, charge));
        if (!simulate && amount > 0.0) {
            charge -= amount;
            if (charge == 0.0) {
                data.remove("charge");
            } else {
                data.putDouble("charge", charge);
            }
            setData(stack, data);
        }

        return amount;
    }

    @Override
    public double getCharge(ItemStack stack) {
        return discharge(stack, Double.MAX_VALUE, Integer.MAX_VALUE, true, false, true);
    }

    @Override
    public double getMaxCharge(ItemStack stack) {
        ItemStack jetpack = jetpack();
        return ((IElectricItem) jetpack.getItem()).getMaxCharge(jetpack);
    }

    @Override
    public boolean canUse(ItemStack stack, double amount) {
        return getCharge(stack) >= amount;
    }

    @Override
    public boolean use(ItemStack stack, double amount, LivingEntity entity) {
        if (!canUse(stack, amount)) {
            return false;
        }
        discharge(stack, amount, Integer.MAX_VALUE, true, false, false);
        return true;
    }

    @Override
    public void chargeFromArmor(ItemStack stack, LivingEntity entity) {
    }

    @Override
    public String getToolTip(ItemStack stack) {
        return Math.round(getCharge(stack)) + "/" + Math.round(getMaxCharge(stack)) + " EU";
    }

    @Override
    public int getTier(ItemStack stack) {
        ItemStack jetpack = jetpack();
        return ((IElectricItem) jetpack.getItem()).getTier(jetpack);
    }

    private static NbtCompound getData(ItemStack stack) {
        return stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
    }

    private static void setData(ItemStack stack, NbtCompound data) {
        if (data.isEmpty()) {
            stack.remove(DataComponentTypes.CUSTOM_DATA);
        } else {
            stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(data));
        }
    }
}
