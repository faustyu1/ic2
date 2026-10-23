package ic2.core.item.armor;

import java.util.LinkedList;
import java.util.List;

import ic2.api.item.ElectricItem;
import ic2.api.item.IElectricItem;
import ic2.api.item.IItemHudInfo;
import ic2.core.IC2;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.world.World;

/**
 * Fabric port of {@code ic2.core.item.armor.ItemArmorNightvisionGoggles}: 200k EU, 1 EU a tick for
 * night vision, and blindness instead when the sky is bright enough that the goggles would blind
 * the wearer.
 *
 * <p>ALT plus the mode switch key turns them on and off, with the same 10 tick lockout the suits
 * use so one press is one toggle.
 */
public class ItemArmorNightvisionGoggles extends ItemArmorUtility implements IElectricItem, IItemHudInfo {

    public ItemArmorNightvisionGoggles() {
        super("nightvision_goggles", IC2ArmorMaterials.NIGHTVISION, Type.HELMET,
                new Settings().maxCount(1).maxDamage(27));
    }

    @Override
    public boolean canProvideEnergy(ItemStack stack) {
        return false;
    }

    @Override
    public double getMaxCharge(ItemStack stack) {
        return 200000.0;
    }

    @Override
    public int getTier(ItemStack stack) {
        return 1;
    }

    @Override
    public double getTransferLimit(ItemStack stack) {
        return 200.0;
    }

    @Override
    public List<String> getHudInfo(ItemStack stack, boolean advanced) {
        List<String> info = new LinkedList<>();
        info.add(ElectricItem.manager.getToolTip(stack));
        return info;
    }

    @Override
    public void onArmorTick(World world, PlayerEntity player, ItemStack stack) {
        if (world.isClient) return;

        NbtCompound data = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
        boolean active = data.getBoolean("active");
        byte toggleTimer = data.getByte("toggleTimer");
        boolean changed = false;

        if (IC2.keyboard.isAltKeyDown(player) && IC2.keyboard.isModeSwitchKeyDown(player) && toggleTimer == 0) {
            toggleTimer = 10;
            active = !active;
            data.putBoolean("active", active);
            player.sendMessage(Text.literal(active ? "Nightvision enabled." : "Nightvision disabled."), false);
            changed = true;
        }

        if (toggleTimer > 0) {
            data.putByte("toggleTimer", (byte) (toggleTimer - 1));
            changed = true;
        }

        if (changed) {
            stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(data));
        }

        if (active && ElectricItem.manager.use(stack, 1.0, player)) {
            applyNightvision(world, player);
        }
    }

    /** The 1.12.2 behaviour both the goggles and the nano/quantum helmet share. */
    static void applyNightvision(World world, PlayerEntity player) {
        int skylight = world.getLightLevel(net.minecraft.world.LightType.SKY, player.getBlockPos());
        if (skylight > 8) {
            player.removeStatusEffect(StatusEffects.NIGHT_VISION);
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 100, 0, true, true));
        } else {
            player.removeStatusEffect(StatusEffects.BLINDNESS);
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 300, 0, true, true));
        }
    }
}
