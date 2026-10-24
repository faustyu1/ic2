package ic2.core.block;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.FluidBlock;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.fluid.Fluid;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;

/**
 * The block of an IC2 fluid that can stand in the world: hot water, coolant, hot coolant,
 * UU-matter and construction foam. 1.12.2 extended Forge's BlockFluidClassic; here it is a vanilla
 * {@link FluidBlock} of the registered IC2 fluid.
 *
 * <p>Wading through one does what it did in 1.12.2: construction foam slows, UU-matter and hot
 * water heal (hot water withers the undead instead), hot coolant sets on fire.
 */
public class BlockIC2Fluid extends FluidBlock {
    public BlockIC2Fluid(Fluid fluid, AbstractBlock.Settings settings) {
        super((FlowableFluid) fluid, settings);
    }

    /** The still fluid the block is made of. */
    public Fluid getIc2Fluid() {
        return fluid;
    }

    @Override
    protected void onEntityCollision(BlockState state, net.minecraft.world.World world, BlockPos pos, net.minecraft.entity.Entity entity) {
        super.onEntityCollision(state, world, pos, entity);
        if (world.isClient) {
            return;
        }
        if (fluid == ic2.core.ref.FluidName.hot_coolant.getInstance()) {
            entity.setOnFireFor(30);
        }
        if (!(entity instanceof net.minecraft.entity.LivingEntity living)) {
            return;
        }
        if (fluid == ic2.core.ref.FluidName.construction_foam.getInstance()) {
            addEffect(living, net.minecraft.entity.effect.StatusEffects.SLOWNESS, 300, 2);
        } else if (fluid == ic2.core.ref.FluidName.uu_matter.getInstance()) {
            addEffect(living, net.minecraft.entity.effect.StatusEffects.REGENERATION, 100, 1);
        } else if (fluid == ic2.core.ref.FluidName.hot_water.getInstance()) {
            addEffect(living, living.getType().isIn(net.minecraft.registry.tag.EntityTypeTags.UNDEAD) ? net.minecraft.entity.effect.StatusEffects.WITHER
                    : net.minecraft.entity.effect.StatusEffects.REGENERATION, 100, world.random.nextInt(2));
        }
    }

    private static void addEffect(net.minecraft.entity.LivingEntity entity,
                                  net.minecraft.registry.entry.RegistryEntry<net.minecraft.entity.effect.StatusEffect> effect,
                                  int duration, int amplifier) {
        if (!entity.hasStatusEffect(effect)) {
            entity.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(effect, duration, amplifier, true, true));
        }
    }

    @Override
    protected boolean isTransparent(BlockState state, BlockView world, BlockPos pos) {
        return true;
    }

    @Override
    protected int getOpacity(BlockState state, BlockView world, BlockPos pos) {
        return 0;
    }
}
