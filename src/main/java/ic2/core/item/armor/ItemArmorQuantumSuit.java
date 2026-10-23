package ic2.core.item.armor;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import ic2.api.item.ElectricItem;
import ic2.api.item.HudMode;
import ic2.api.item.IHazmatLike;
import ic2.api.item.IItemHudProvider;
import ic2.api.item.ISpecialArmor;
import ic2.core.IC2;
import ic2.core.IC2Potion;
import ic2.core.init.MainConfig;
import ic2.core.item.armor.jetpack.IJetpack;
import ic2.core.item.armor.jetpack.JetpackLogic;
import ic2.core.util.ConfigUtil;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Rarity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Fabric port of {@code ic2.core.item.armor.ItemArmorQuantumSuit}: 10M EU, 20000 EU per damage
 * point, and absorption at priority 8 so it is served before the nano suit and the hazmat suit.
 *
 * <p>Ported: absorption, the fall the boots and leggings take, the helmet refilling air, feeding
 * tin cans and buying off poison, radiation and wither and toggling night vision and the HUD, the chestplate putting
 * the fire out, the leggings' sprint boost and the boots' high jump.
 *
 * <p>The chestplate is also a jetpack, as in 1.12.2 - the flight runs through
 * {@link JetpackLogic}. Not ported: eating from a filled tin can, which needs
 * {@code ItemTinCan}.
 */
public class ItemArmorQuantumSuit extends ItemArmorElectric implements IJetpack, IHazmatLike, IItemHudProvider {

    /** Client-side only: how much of the boots' stored jump is left, as in 1.12.2. */
    private float jumpCharge;

    /** EU the helmet pays to take an effect off, {@code potionRemovalCost} in 1.12.2. */
    private static final Map<StatusEffect, Integer> POTION_REMOVAL_COST = new IdentityHashMap<>();

    public ItemArmorQuantumSuit(String itemName, Type type) {
        super(itemName, IC2ArmorMaterials.QUANTUM, type, 1.0E7, 12000.0, 4, Rarity.RARE);
    }

    private static Map<StatusEffect, Integer> removalCosts() {
        if (POTION_REMOVAL_COST.isEmpty()) {
            POTION_REMOVAL_COST.put(StatusEffects.POISON.value(), 10000);
            POTION_REMOVAL_COST.put(StatusEffects.WITHER.value(), 25000);
            if (IC2Potion.radiation != null) {
                POTION_REMOVAL_COST.put(IC2Potion.radiation, 10000);
            }
        }
        return POTION_REMOVAL_COST;
    }

    @Override
    public boolean addsProtection(LivingEntity entity, EquipmentSlot slot, ItemStack stack) {
        return ElectricItem.manager.getCharge(stack) > 0.0;
    }

    @Override
    public ISpecialArmor.ArmorProperties getProperties(LivingEntity entity, ItemStack armor, DamageSource source,
                                                       double damage, EquipmentSlot slot) {
        int damageLimit = damageLimit(armor);

        if (source.isOf(DamageTypes.FALL)) {
            if (getType() == Type.BOOTS) {
                return new ISpecialArmor.ArmorProperties(10, 1.0, damageLimit);
            }
            if (getType() == Type.LEGGINGS) {
                return new ISpecialArmor.ArmorProperties(9, 0.8, damageLimit);
            }
        }

        return new ISpecialArmor.ArmorProperties(8, getBaseAbsorptionRatio() * getDamageAbsorptionRatio(), damageLimit);
    }

    @Override
    public boolean absorbFallDamage(LivingEntity entity, ItemStack stack, float fallDistance) {
        if (getType() != Type.BOOTS) return false;

        int fallDamage = Math.max((int) fallDistance - 10, 0);
        double energyCost = (double) getEnergyPerDamage() * fallDamage;
        if (energyCost > ElectricItem.manager.getCharge(stack)) return false;

        ElectricItem.manager.discharge(stack, energyCost, Integer.MAX_VALUE, true, false, false);
        return true;
    }

    @Override
    public double getDamageAbsorptionRatio() {
        return getType() == Type.CHESTPLATE ? 1.2 : 1.0;
    }

    @Override
    public int getEnergyPerDamage() {
        return 20000;
    }

    @Override
    public void onArmorTick(World world, PlayerEntity player, ItemStack stack) {
        switch (getType()) {
            case HELMET -> {
                if (world.isClient) return;
                tickHelmet(player, stack);
                HelmetToggles.tick(world, player, stack);
            }
            case CHESTPLATE -> {
                // the flight runs on both sides; only the fire goes out on the server
                JetpackLogic.onArmorTick(world, player, stack, this);
                if (!world.isClient) {
                    player.extinguish();
                }
            }
            // the leggings and the boots move the player, so they run on both sides: the client
            // applies the motion, the server charges for it, as in 1.12.2
            case LEGGINGS -> tickLeggings(world, player, stack);
            case BOOTS -> tickBoots(world, player, stack);
            default -> {
            }
        }
    }

    /** Air and the effects the helmet pays to remove. */
    private void tickHelmet(PlayerEntity player, ItemStack stack) {
        int air = player.getAir();
        if (air < 100 && ElectricItem.manager.canUse(stack, 1000.0)) {
            player.setAir(air + 200);
            ElectricItem.manager.use(stack, 1000.0, null);
        }

        // a hungry wearer is fed the first filled tin can in the inventory
        if (ElectricItem.manager.canUse(stack, 1000.0) && player.getHungerManager().isNotFull()) {
            for (ItemStack can : player.getInventory().main) {
                if (can.getItem() instanceof ic2.core.item.ItemTinCan) {
                    if (ic2.core.item.ItemTinCan.onEaten(player, can)) {
                        ElectricItem.manager.use(stack, 1000.0, null);
                    }
                    break;
                }
            }
        }

        for (StatusEffectInstance effect : new ArrayList<>(player.getStatusEffects())) {
            Integer cost = removalCosts().get(effect.getEffectType().value());
            if (cost == null) continue;

            int total = cost * (effect.getAmplifier() + 1);
            if (ElectricItem.manager.canUse(stack, total)) {
                ElectricItem.manager.use(stack, total, null);
                player.removeStatusEffect(effect.getEffectType());
            }
        }
    }

    /**
     * The sprint boost: 1000 EU every 10 ticks of running forward on the ground or in water, and
     * an extra push upwards while swimming. Which key arms it is the
     * {@code misc/quantumSpeedOnSprint} setting - sprinting, or the boost key.
     */
    private void tickLeggings(World world, PlayerEntity player, ItemStack stack) {
        boolean onSprint = ConfigUtil.getBool(MainConfig.get(), "misc/quantumSpeedOnSprint");
        boolean armed = onSprint ? player.isSprinting() : IC2.keyboard.isBoostKeyDown(player);

        if (!ElectricItem.manager.canUse(stack, 1000.0)
                || !(player.isOnGround() || player.isTouchingWater())
                || !IC2.keyboard.isForwardKeyDown(player)
                || !armed) {
            return;
        }

        NbtCompound data = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
        byte speedTicker = (byte) (data.getByte("speedTicker") + 1);
        if (speedTicker >= 10) {
            speedTicker = 0;
            ElectricItem.manager.use(stack, 1000.0, null);
        }
        data.putByte("speedTicker", speedTicker);
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(data));

        float speed = 0.22f;
        if (player.isTouchingWater()) {
            speed = 0.1f;
            if (IC2.keyboard.isJumpKeyDown(player)) {
                player.setVelocity(player.getVelocity().add(0.0, 0.1, 0.0));
            }
        }

        player.updateVelocity(speed, new Vec3d(0.0, 0.0, 1.0));
    }

    /**
     * The high jump: jump plus boost in mid-air costs 4000 EU on the server, and on the client
     * pushes the player up while the charge from the ground lasts.
     */
    private void tickBoots(World world, PlayerEntity player, ItemStack stack) {
        if (!world.isClient) {
            NbtCompound data = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
            boolean wasOnGround = !data.contains("wasOnGround") || data.getBoolean("wasOnGround");

            if (wasOnGround && !player.isOnGround()
                    && IC2.keyboard.isJumpKeyDown(player) && IC2.keyboard.isBoostKeyDown(player)) {
                ElectricItem.manager.use(stack, 4000.0, null);
            }

            if (player.isOnGround() != wasOnGround) {
                data.putBoolean("wasOnGround", player.isOnGround());
                stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(data));
            }
            return;
        }

        if (ElectricItem.manager.canUse(stack, 4000.0) && player.isOnGround()) {
            jumpCharge = 1.0f;
        }

        if (player.getVelocity().y < 0.0 || jumpCharge <= 0.0f || player.isTouchingWater()) {
            return;
        }

        if (IC2.keyboard.isJumpKeyDown(player) && IC2.keyboard.isBoostKeyDown(player)) {
            if (jumpCharge == 1.0f) {
                player.setVelocity(player.getVelocity().multiply(3.5, 1.0, 3.5));
            }
            player.setVelocity(player.getVelocity().add(0.0, jumpCharge * 0.3f, 0.0));
            jumpCharge = (float) (jumpCharge * 0.75);
        } else if (jumpCharge < 1.0f) {
            jumpCharge = 0.0f;
        }
    }

    @Override
    public boolean doesProvideHUD(ItemStack stack) {
        return getType() == Type.HELMET && ElectricItem.manager.getCharge(stack) > 0.0;
    }

    @Override
    public HudMode getHudMode(ItemStack stack) {
        NbtComponent data = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
        return HudMode.getFromID(data.copyNbt().getShort("HudMode"));
    }


    // ---- IJetpack: the chestplate flies, with the 1.12.2 numbers ----

    @Override
    public boolean drainEnergy(ItemStack pack, int amount) {
        return ElectricItem.manager.discharge(pack, amount + EU_ENERGY_INCREASE, Integer.MAX_VALUE, true, false, false) > 0.0;
    }

    @Override
    public float getPower(ItemStack stack) {
        return 1.0f;
    }

    @Override
    public float getDropPercentage(ItemStack stack) {
        return 0.05f;
    }

    @Override
    public double getChargeLevel(ItemStack stack) {
        return ElectricItem.manager.getCharge(stack) / getMaxCharge(stack);
    }

    @Override
    public boolean isJetpackActive(ItemStack stack) {
        return getType() == Type.CHESTPLATE;
    }

    @Override
    public float getHoverMultiplier(ItemStack stack, boolean upwards) {
        return 0.1f;
    }

    @Override
    public float getWorldHeightDivisor(ItemStack stack) {
        return 0.9f;
    }

    /** Effects the helmet is willing to pay to remove, exposed for the tests. */
    public static List<StatusEffect> getRemovableEffects() {
        return new ArrayList<>(removalCosts().keySet());
    }
}
