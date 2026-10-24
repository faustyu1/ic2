package ic2.core.item.tool;

import java.util.List;

import ic2.api.item.ElectricItem;
import ic2.core.IC2;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.Shearable;
import net.minecraft.sound.SoundCategory;
import net.minecraft.world.event.GameEvent;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

/**
 * Fabric port of {@code ic2.core.item.tool.ItemElectricToolChainsaw}: axe, sword and shears in
 * one, 30k EU and 100 EU per use, mining wood at 12.
 *
 * <p>ALT plus the mode switch key turns the shearing off, as in 1.12.2. Shearing works on
 * anything vanilla calls {@link Shearable}, which is the 1.21.1 equivalent of Forge's
 * {@code IShearable}.
 */
public class ItemChainsaw extends ItemElectricTool implements ic2.core.item.IHeldToolSounds {
    @Override
    public net.minecraft.sound.SoundEvent getIdleSound(ItemStack stack) {
        return ic2.core.init.IC2Sounds.TOOLS_CHAINSAW_CHAINSAW_IDLE;
    }

    @Override
    public net.minecraft.sound.SoundEvent getStopSound(ItemStack stack) {
        return ic2.core.init.IC2Sounds.TOOLS_CHAINSAW_CHAINSAW_STOP;
    }


    public ItemChainsaw() {
        super("chainsaw", 100, 30000, 100, 1, 12.0f, BlockTags.INCORRECT_FOR_IRON_TOOL,
                List.of(BlockTags.AXE_MINEABLE, BlockTags.LEAVES, BlockTags.WOOL),
                attackModifiers(9.0, -3.0));
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (world.isClient) {
            return TypedActionResult.pass(stack);
        }

        if (IC2.keyboard.isAltKeyDown(player) && IC2.keyboard.isModeSwitchKeyDown(player)) {
            NbtCompound data = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
            boolean disabled = !data.getBoolean("disableShear");
            data.putBoolean("disableShear", disabled);
            stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(data));
            player.sendMessage(Text.literal(disabled ? "Shearing disabled." : "Shearing enabled."), false);
            return TypedActionResult.success(stack);
        }

        return TypedActionResult.pass(stack);
    }

    /** Shearing costs a use of energy and is skipped while the mode is off. */
    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity player, LivingEntity entity, Hand hand) {
        NbtCompound data = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
        if (data.getBoolean("disableShear")) {
            return ActionResult.PASS;
        }

        if (!ElectricItem.manager.canUse(stack, operationEnergyCost)) {
            return ActionResult.PASS;
        }

        if (!(entity instanceof Shearable shearable) || !shearable.isShearable()) {
            return ActionResult.PASS;
        }

        if (!player.getWorld().isClient) {
            shearable.sheared(SoundCategory.PLAYERS);
            player.getWorld().emitGameEvent(player, GameEvent.SHEAR, entity.getPos());
            ElectricItem.manager.use(stack, operationEnergyCost, player);
        }

        return ActionResult.success(player.getWorld().isClient);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        ElectricItem.manager.use(stack, operationEnergyCost, attacker);
        return true;
    }
}
