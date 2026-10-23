package ic2.core.item;

import ic2.api.item.IElectricItem;
import ic2.api.item.IElectricItemManager;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

/**
 * Fabric port for 1.21.1 — EU storage via {@link DataComponentTypes#CUSTOM_DATA} ({@link NbtComponent}).
 * Charge stored as double {@code "charge"} in CUSTOM_DATA.
 */
public class ElectricItemManager implements IElectricItemManager {

    public static final ElectricItemManager INSTANCE = new ElectricItemManager();

    public ElectricItemManager() {}

    // ---- NBT helpers (Fabric 1.21.1: DataComponentTypes.CUSTOM_DATA) ----

    private static NbtCompound getNbt(ItemStack stack) {
        NbtComponent comp = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
        return comp.copyNbt();
    }

    private static void setNbt(ItemStack stack, NbtCompound nbt) {
        if (nbt == null || nbt.isEmpty()) {
            stack.remove(DataComponentTypes.CUSTOM_DATA);
        } else {
            stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
        }
    }

    private static double getChargeNbt(ItemStack stack) {
        NbtCompound nbt = getNbt(stack);
        return nbt.contains("charge") ? nbt.getDouble("charge") : 0.0D;
    }

    private static void setChargeNbt(ItemStack stack, double charge) {
        NbtCompound nbt = getNbt(stack);
        if (charge > 0.0D) {
            nbt.putDouble("charge", charge);
        } else {
            nbt.remove("charge");
        }
        setNbt(stack, nbt);
    }

    private static int mapChargeLevelToDamage(double charge, double maxCharge, int maxDamage) {
        if (maxDamage < 2) return 0;
        maxDamage--;
        if (maxCharge <= 0) return maxDamage;
        double mapped = (charge / maxCharge) * (double) maxDamage;
        return maxDamage - (int) mapped;
    }

    @Override
    public double charge(ItemStack stack, double amount, int tier, boolean ignoreTransferLimit, boolean simulate) {
        if (stack == null || stack.isEmpty()) return 0.0D;
        if (!(stack.getItem() instanceof IElectricItem item)) return 0.0D;
        if (amount < 0.0D) return 0.0D;
        if (stack.getCount() > 1) return 0.0D;
        if (item.getTier(stack) > tier) return 0.0D;
        if (item.getMaxCharge(stack) <= 0.0D) return 0.0D;

        if (!ignoreTransferLimit && amount > item.getTransferLimit(stack)) {
            amount = item.getTransferLimit(stack);
        }

        double current = getChargeNbt(stack);
        double maxCharge = item.getMaxCharge(stack);
        amount = Math.min(amount, maxCharge - current);
        if (amount <= 0.0D) return 0.0D;

        if (!simulate) {
            double newCharge = current + amount;
            setChargeNbt(stack, newCharge);
            // update damage bar
            if (stack.getItem() instanceof IElectricItem) {
                IElectricItem it = (IElectricItem) stack.getItem();
                int maxDamage = DamageHandler.getMaxDamage(stack);
                DamageHandler.setDamage(stack, mapChargeLevelToDamage(newCharge, it.getMaxCharge(stack), maxDamage), true);
            } else {
                DamageHandler.setDamage(stack, 0, true);
            }
        }
        return amount;
    }

    @Override
    public double discharge(ItemStack stack, double amount, int tier, boolean ignoreTransferLimit, boolean externally, boolean simulate) {
        if (stack == null || stack.isEmpty()) return 0.0D;
        if (!(stack.getItem() instanceof IElectricItem item)) return 0.0D;
        if (amount < 0.0D) return 0.0D;
        if (stack.getCount() > 1) return 0.0D;
        if (item.getTier(stack) > tier) return 0.0D;
        if (externally && !item.canProvideEnergy(stack)) return 0.0D;

        if (!ignoreTransferLimit && amount > item.getTransferLimit(stack)) {
            amount = item.getTransferLimit(stack);
        }

        double current = getChargeNbt(stack);
        amount = Math.min(amount, current);
        if (amount <= 0.0D) return 0.0D;

        if (!simulate) {
            double newCharge = current - amount;
            setChargeNbt(stack, newCharge);
            if (stack.getItem() instanceof IElectricItem) {
                IElectricItem it = (IElectricItem) stack.getItem();
                int maxDamage = DamageHandler.getMaxDamage(stack);
                DamageHandler.setDamage(stack, mapChargeLevelToDamage(newCharge, it.getMaxCharge(stack), maxDamage), true);
            } else {
                DamageHandler.setDamage(stack, 0, true);
            }
        }
        return amount;
    }

    @Override
    public double getCharge(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0.0D;
        if (!(stack.getItem() instanceof IElectricItem)) return 0.0D;
        // simulate discharge to get current charge without mutation
        return getChargeNbt(stack);
    }

    @Override
    public double getMaxCharge(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0.0D;
        if (stack.getItem() instanceof IElectricItem item) {
            return item.getMaxCharge(stack);
        }
        return 0.0D;
    }

    @Override
    public boolean canUse(ItemStack stack, double amount) {
        return getCharge(stack) >= amount;
    }

    @Override
    public boolean use(ItemStack stack, double amount, LivingEntity entity) {
        if (entity != null) {
            chargeFromArmor(stack, entity);
        }
        double transfer = discharge(stack, amount, Integer.MAX_VALUE, true, false, true);
        // isSimilar with epsilon
        if (Math.abs(transfer - amount) < 1e-9) {
            discharge(stack, amount, Integer.MAX_VALUE, true, false, false);
            if (entity != null) {
                chargeFromArmor(stack, entity);
            }
            return true;
        }
        return false;
    }

    @Override
    public void chargeFromArmor(ItemStack target, LivingEntity entity) {
        if (target == null || target.isEmpty() || entity == null) return;
        if (!(target.getItem() instanceof IElectricItem)) return;

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (!slot.isArmorSlot()) continue;
            ItemStack source = entity.getEquippedStack(slot);
            if (source == null || source.isEmpty()) continue;
            if (source == target) continue;

            int tier;
            if (source.getItem() instanceof IElectricItem ie) {
                tier = ie.getTier(source);
            } else {
                tier = Integer.MAX_VALUE;
            }

            double transfer = discharge(source, Double.POSITIVE_INFINITY, Integer.MAX_VALUE, true, true, true);
            if (transfer <= 0.0D) continue;
            transfer = charge(target, transfer, tier, true, false);
            if (transfer <= 0.0D) continue;
            discharge(source, transfer, Integer.MAX_VALUE, true, true, false);
        }
    }

    @Override
    public String getToolTip(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        if (!(stack.getItem() instanceof IElectricItem)) return null;
        double charge = getCharge(stack);
        double max = getMaxCharge(stack);
        return toSiString(charge, 3) + "/" + toSiString(max, 3) + " EU";
    }

    private static String toSiString(double value, int digits) {
        if (value >= 1_000_000) return String.format("%." + digits + "fM", value / 1_000_000);
        if (value >= 1_000) return String.format("%." + digits + "fk", value / 1_000);
        return String.format("%." + digits + "f", value);
    }

    @Override
    public int getTier(ItemStack stack) {
        if (stack != null && !stack.isEmpty() && stack.getItem() instanceof IElectricItem item) {
            return item.getTier(stack);
        }
        return 0;
    }
}
