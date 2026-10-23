package ic2.core.crop.cards;

import ic2.api.crops.CropProperties;
import ic2.api.crops.ICropTile;
import ic2.api.item.ItemWrapper;
import ic2.core.IC2;
import ic2.core.IC2DamageSource;
import ic2.core.crop.IC2CropCard;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.common.BiomeDictionary;

/**
 * Fabric port of CropEating - the plant that eats whatever walks into it.
 *
 * <p>Once past its first size it pulls a living thing in, bites it and blinds it, and grows a
 * little from the meal; metal armour keeps the effects off. To pass its third size it has to
 * stand over lava, which is what keeps it out of an ordinary farm.
 */
public class CropEating extends IC2CropCard {
    private static final double PULL = 0.5;

    @Override public String getDiscoveredBy() { return "Hasudako"; }
    @Override public String getId() { return "eatingplant"; }
    @Override public CropProperties getProperties() { return new CropProperties(6, 1, 1, 3, 1, 4); }
    @Override public String[] getAttributes() { return new String[]{"Bad", "Food"}; }
    @Override public int getMaxSize() { return 6; }

    @Override
    public boolean canGrow(ICropTile crop) {
        if (crop.getCurrentSize() < 3) {
            return crop.getLightLevel() > 10;
        }
        return crop.isBlockBelow(Blocks.LAVA) && crop.getCurrentSize() < getMaxSize() && crop.getLightLevel() > 10;
    }

    @Override public int getOptimalHarvestSize(ICropTile crop) { return 4; }

    @Override
    public boolean canBeHarvested(ICropTile crop) {
        return crop.getCurrentSize() >= 4 && crop.getCurrentSize() < 6;
    }

    @Override
    public ItemStack getGain(ICropTile crop) {
        return crop.getCurrentSize() >= 4 && crop.getCurrentSize() < 6
                ? new ItemStack(Blocks.CACTUS) : ItemStack.EMPTY;
    }

    @Override
    public void tick(ICropTile crop) {
        if (crop.getCurrentSize() == 1) {
            return;
        }

        var world = crop.getWorldObj();
        if (world == null || world.isClient) {
            return;
        }

        BlockPos pos = crop.getPosition();
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;

        // what it ate last time comes back out as rotten flesh
        if (crop.getCustomData().getBoolean("eaten")) {
            net.minecraft.util.ItemScatterer.spawn(world, x, y, z, new ItemStack(Items.ROTTEN_FLESH));
            crop.getCustomData().putBoolean("eaten", false);
        }

        List<LivingEntity> nearby = new ArrayList<>(world.getNonSpectatingEntities(LivingEntity.class,
                new Box(x - 1.0, pos.getY(), z - 1.0, x + 1.0, pos.getY() + 2.0, z + 1.0)));
        if (nearby.isEmpty()) {
            return;
        }
        Collections.shuffle(nearby);

        for (LivingEntity entity : nearby) {
            if (entity instanceof PlayerEntity player && player.getAbilities().creativeMode) {
                continue;
            }

            Vec3d velocity = entity.getVelocity();
            entity.setVelocity((x - entity.getX()) * PULL,
                    Math.min(velocity.y, -0.05),
                    (z - entity.getZ()) * PULL);
            entity.velocityModified = true;

            entity.damage(IC2DamageSource.generic(world), crop.getCurrentSize() * 2.0f);

            if (!hasMetalArmor(entity)) {
                entity.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 64, 50));
                entity.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, 64, 0));
                entity.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 64, 0));
            }

            if (canGrow(crop)) {
                crop.setGrowthPoints(crop.getGrowthPoints() + 100);
            }

            world.playSound(null, x, y, z, SoundEvents.ENTITY_GENERIC_EAT, SoundCategory.BLOCKS,
                    1.0f, IC2.random.nextFloat() * 0.1f + 0.9f);
            crop.getCustomData().putBoolean("eaten", true);
            break;
        }
    }

    @Override public int getRootsLength(ICropTile crop) { return 5; }

    /** Swamps and mountains suit it, and thin air slows it down. */
    @Override
    public int getGrowthDuration(ICropTile crop) {
        float multiplier = 1.0f;
        var world = crop.getWorldObj();
        if (world != null) {
            var biome = world.getBiome(crop.getPosition());
            if (BiomeDictionary.hasType(biome, BiomeDictionary.Type.SWAMP)
                    || BiomeDictionary.hasType(biome, BiomeDictionary.Type.MOUNTAIN)) {
                multiplier /= 1.5f;
            }
        }

        multiplier /= 1.0f + crop.getTerrainAirQuality() / 10.0f;
        return (int) (super.getGrowthDuration(crop) * multiplier);
    }

    private static boolean hasMetalArmor(LivingEntity entity) {
        if (!(entity instanceof PlayerEntity player)) {
            return false;
        }

        for (ItemStack stack : player.getInventory().armor) {
            if (!stack.isEmpty() && ItemWrapper.isMetalArmor(stack, player)) {
                return true;
            }
        }
        return false;
    }
}
