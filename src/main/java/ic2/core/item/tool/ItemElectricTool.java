package ic2.core.item.tool;

import java.util.LinkedList;
import java.util.List;

import ic2.api.item.ElectricItem;
import ic2.api.item.IElectricItem;
import ic2.api.item.IItemHudInfo;
import ic2.core.item.ItemIC2;
import net.minecraft.block.BlockState;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Fabric port of {@code ic2.core.item.tool.ItemElectricTool}: a tool that runs on EU instead of
 * wearing out, and that mines at its full speed only while it has charge for one more block.
 *
 * <p>1.12.2 described what a tool could mine with a harvest level and a set of tool classes. Since
 * 1.17 that is tag work, so each tool names the block tags it is good for and the tag of blocks
 * its level cannot handle.
 */
public abstract class ItemElectricTool extends ItemIC2 implements IElectricItem, IItemHudInfo {

    /** EU one block costs, {@code operationEnergyCost} in 1.12.2. */
    protected final double operationEnergyCost;
    protected final double maxCharge;
    protected final double transferLimit;
    protected final int tier;
    /** Mining speed while the tool has charge, the 1.12.2 {@code efficiencyOnProperMaterial}. */
    protected final float efficiency;

    private final List<TagKey<net.minecraft.block.Block>> mineable;
    private final TagKey<net.minecraft.block.Block> incorrectForDrops;

    protected ItemElectricTool(String name, double operationEnergyCost, double maxCharge, double transferLimit,
                               int tier, float efficiency, TagKey<net.minecraft.block.Block> incorrectForDrops,
                               List<TagKey<net.minecraft.block.Block>> mineable) {
        this(name, operationEnergyCost, maxCharge, transferLimit, tier, efficiency, incorrectForDrops, mineable,
                attackModifiers(2.0, -3.0));
    }

    protected ItemElectricTool(String name, double operationEnergyCost, double maxCharge, double transferLimit,
                               int tier, float efficiency, TagKey<net.minecraft.block.Block> incorrectForDrops,
                               List<TagKey<net.minecraft.block.Block>> mineable,
                               AttributeModifiersComponent attributes) {
        super(name, new Settings().maxCount(1).maxDamage(27).attributeModifiers(attributes));
        this.operationEnergyCost = operationEnergyCost;
        this.maxCharge = maxCharge;
        this.transferLimit = transferLimit;
        this.tier = tier;
        this.efficiency = efficiency;
        this.incorrectForDrops = incorrectForDrops;
        this.mineable = List.copyOf(mineable);
    }

    /**
     * The attack damage and swing speed of a tool, the two numbers 1.12.2 passed to
     * {@code ItemToolIC2}. They are fixed per item here, so a flat tool hits as hard empty as it
     * does charged; the energy is still spent on the hit.
     */
    protected static AttributeModifiersComponent attackModifiers(double damage, double speed) {
        return AttributeModifiersComponent.builder()
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE,
                        new EntityAttributeModifier(BASE_ATTACK_DAMAGE_MODIFIER_ID, damage,
                                EntityAttributeModifier.Operation.ADD_VALUE),
                        AttributeModifierSlot.MAINHAND)
                .add(EntityAttributes.GENERIC_ATTACK_SPEED,
                        new EntityAttributeModifier(BASE_ATTACK_SPEED_MODIFIER_ID, speed,
                                EntityAttributeModifier.Operation.ADD_VALUE),
                        AttributeModifierSlot.MAINHAND)
                .build();
    }

    /** True when the block is one of the kinds this tool is built for. */
    protected boolean isMineable(BlockState state) {
        for (TagKey<net.minecraft.block.Block> tag : mineable) {
            if (state.isIn(tag)) return true;
        }
        return false;
    }

    @Override
    public float getMiningSpeed(ItemStack stack, BlockState state) {
        if (!isMineable(state)) return 1.0f;
        // an empty tool is no better than a bare hand, as in 1.12.2
        return ElectricItem.manager.canUse(stack, operationEnergyCost) ? efficiency : 1.0f;
    }

    @Override
    public boolean isCorrectForDrops(ItemStack stack, BlockState state) {
        return isMineable(state)
                && !state.isIn(incorrectForDrops)
                && ElectricItem.manager.canUse(stack, operationEnergyCost);
    }

    @Override
    public boolean postMine(ItemStack stack, World world, BlockState state, BlockPos pos, LivingEntity miner) {
        // blocks that break instantly are free, the 1.12.2 hardness != 0 check
        if (state.getHardness(world, pos) != 0.0f) {
            ElectricItem.manager.use(stack, operationEnergyCost, miner);
        }
        return true;
    }

    /** Electric tools take no durability from hitting things. */
    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        return true;
    }

    // ---- IElectricItem ----

    @Override
    public boolean canProvideEnergy(ItemStack stack) {
        return false;
    }

    @Override
    public double getMaxCharge(ItemStack stack) {
        return maxCharge;
    }

    @Override
    public int getTier(ItemStack stack) {
        return tier;
    }

    @Override
    public double getTransferLimit(ItemStack stack) {
        return transferLimit;
    }

    @Override
    public List<String> getHudInfo(ItemStack stack, boolean advanced) {
        List<String> info = new LinkedList<>();
        info.add(ElectricItem.manager.getToolTip(stack));
        info.add("Tier " + tier);
        return info;
    }

    @Override
    public boolean canRepair(ItemStack stack, ItemStack ingredient) {
        return false;
    }

    @Override
    public int getEnchantability() {
        return 0;
    }

    /** Spend {@code amount} EU if there is that much, the 1.12.2 {@code tryUsePower}. */
    protected boolean spendPower(ItemStack stack, double amount) {
        return ElectricItem.manager.canUse(stack, amount)
                && ElectricItem.manager.discharge(stack, amount, tier, true, false, false) > 0.0;
    }
}
