package ic2.core.item.tool;

import java.util.List;

import ic2.api.item.ElectricItem;
import ic2.core.item.armor.ItemArmorNanoSuit;
import ic2.core.item.armor.ItemArmorQuantumSuit;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.Rarity;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

/**
 * Fabric port of {@code ic2.core.item.tool.ItemNanoSaber}: a sword that is only a sword while it
 * is switched on, and that drains the nano and quantum armour of whoever it hits.
 *
 * <p>Right click toggles it. On, it costs 400 EU a hit, 80 EU a block and a trickle while it is
 * carried; off, it hits for 4 and mines at 1. The 1.12.2 swing sounds and the separate lit model
 * are not here - the port has no {@code AudioSource} and no mesh definition yet - so an active
 * saber looks like an idle one.
 */
public class ItemNanoSaber extends ItemElectricTool implements ic2.core.item.IHeldToolSounds {
    /** It hums and powers up only while switched on, as the 1.12.2 saber did. */
    @Override
    public net.minecraft.sound.SoundEvent getIdleSound(ItemStack stack) {
        return isActive(stack) ? ic2.core.init.IC2Sounds.TOOLS_NANOSABRE_NANOSABRE_IDLE : null;
    }

    @Override
    public net.minecraft.sound.SoundEvent getStartSound(ItemStack stack) {
        return ic2.core.init.IC2Sounds.TOOLS_NANOSABRE_NANOSABRE_POWERUP;
    }

    /** One of the three swings, played when the switched on saber is swung. */
    public static net.minecraft.sound.SoundEvent randomSwingSound(net.minecraft.util.math.random.Random random) {
        return switch (random.nextInt(3)) {
            case 1 -> ic2.core.init.IC2Sounds.TOOLS_NANOSABRE_NANOSABRE_SWING_2;
            case 2 -> ic2.core.init.IC2Sounds.TOOLS_NANOSABRE_NANOSABRE_SWING_3;
            default -> ic2.core.init.IC2Sounds.TOOLS_NANOSABRE_NANOSABRE_SWING_1;
        };
    }

    /** Charge a hit costs. */
    private static final double HIT_COST = 400.0;
    /** Charge drained out of each piece of the victim's nano armour. */
    private static final double NANO_DRAIN = 48000.0;
    /** Charge drained out of each piece of the victim's quantum armour. */
    private static final double QUANTUM_DRAIN = 300000.0;
    /** Charge each armour piece drained costs the saber. */
    private static final double DRAIN_COST = 2000.0;

    /** Counted up by the tick handler; the carried saber bleeds charge on its schedule. */
    public static int ticker = 0;

    public ItemNanoSaber() {
        super("nano_saber", 10, 160000, 500, 3, 1.0f, BlockTags.INCORRECT_FOR_DIAMOND_TOOL,
                List.of(BlockTags.SWORD_EFFICIENT),
                attackModifiers(20.0, -2.4));
    }

    public static boolean isActive(ItemStack stack) {
        return stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt().getBoolean("active");
    }

    private static void setActive(ItemStack stack, boolean active) {
        NbtCompound data = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
        data.putBoolean("active", active);
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(data));
    }

    /** Spend charge, and switch the saber off when there is none left. */
    public static void drainSaber(ItemStack stack, double amount, LivingEntity entity) {
        if (!ElectricItem.manager.use(stack, amount, entity)) {
            setActive(stack, false);
        }
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (world.isClient) {
            return TypedActionResult.pass(stack);
        }

        if (isActive(stack)) {
            setActive(stack, false);
            return TypedActionResult.success(stack);
        }

        if (ElectricItem.manager.canUse(stack, 16.0)) {
            setActive(stack, true);
            return TypedActionResult.success(stack);
        }

        return TypedActionResult.pass(stack);
    }

    @Override
    public float getMiningSpeed(ItemStack stack, BlockState state) {
        if (!isActive(stack)) {
            return 1.0f;
        }
        // the one block a sword is good at, as in 1.12.2
        return state.isOf(Blocks.COBWEB) ? 50.0f : 4.0f;
    }

    @Override
    public boolean postMine(ItemStack stack, World world, BlockState state, net.minecraft.util.math.BlockPos pos,
                            LivingEntity miner) {
        if (isActive(stack)) {
            drainSaber(stack, 80.0, miner);
        }
        return true;
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!isActive(stack) || attacker.getWorld().isClient) {
            return true;
        }

        drainSaber(stack, HIT_COST, attacker);

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() != EquipmentSlot.Type.HUMANOID_ARMOR) continue;
            if (!ElectricItem.manager.canUse(stack, DRAIN_COST)) break;

            ItemStack armor = target.getEquippedStack(slot);
            if (armor.isEmpty()) continue;

            double amount = 0.0;
            if (armor.getItem() instanceof ItemArmorNanoSuit) {
                amount = NANO_DRAIN;
            } else if (armor.getItem() instanceof ItemArmorQuantumSuit) {
                amount = QUANTUM_DRAIN;
            }

            if (amount <= 0.0) continue;

            ElectricItem.manager.discharge(armor, amount, tier, true, false, false);
            if (!ElectricItem.manager.canUse(armor, 1.0)) {
                target.equipStack(slot, ItemStack.EMPTY);
            }
            drainSaber(stack, DRAIN_COST, attacker);
        }

        return true;
    }

    /** An active saber in the hotbar bleeds 64 EU every 16 ticks, elsewhere 16 EU every 64. */
    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);

        if (world.isClient || !isActive(stack) || !(entity instanceof LivingEntity living)) return;

        if (ticker % 16 == 0) {
            if (slot < 9) {
                drainSaber(stack, 64.0, living);
            } else if (ticker % 64 == 0) {
                drainSaber(stack, 16.0, living);
            }
        }
    }

    public Rarity getRarity() {
        return Rarity.UNCOMMON;
    }
}
