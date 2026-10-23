package ic2.core.block.generator.tileentity;

import ic2.core.IC2;
import ic2.core.init.IC2Blocks;
import ic2.core.init.MainConfig;
import ic2.core.ref.TeBlock;
import ic2.core.util.ConfigUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraftforge.common.BiomeDictionary;

/**
 * Fabric port of TileEntitySolarGenerator - the solar panel.
 *
 * <p>It makes up to 1 EU/t ({@code balance/energy/generator/solar}) from the sky light above it,
 * scaled by the sun's height; rain and thunder take 5/16 each away, except in sandy biomes. The
 * light is only looked at every 128 ticks and when the chunk loads, and the panel's buffer holds
 * just 2 EU, so it needs a cable or a battery to be of any use.
 */
public class TileEntitySolarGenerator extends TileEntityBaseGenerator {
    private static final int TICK_RATE = 128;

    /** How much of a full day's light reaches the panel, 0 to 1, as last measured. */
    public float skyLight;
    private int ticker = IC2.random.nextInt(TICK_RATE);

    public TileEntitySolarGenerator(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type == null ? IC2Blocks.SOLAR_BE : type, pos, state, TeBlock.solar_generator, 1.0, 1, 2);
    }

    public TileEntitySolarGenerator(BlockPos pos, BlockState state) {
        this(IC2Blocks.SOLAR_BE, pos, state);
    }

    private static double energyMultiplier() {
        return ConfigUtil.getDouble(MainConfig.get(), "balance/energy/generator/solar");
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (world != null && !world.isClient) {
            updateSunVisibility();
        }
    }

    @Override
    public boolean gainEnergy() {
        if (++ticker % TICK_RATE == 0) {
            updateSunVisibility();
        }

        if (skyLight > 0.0F) {
            energyStored = Math.min(maxStorage, energyStored + energyMultiplier() * skyLight);
            markDirty();
            return true;
        }
        return false;
    }

    @Override
    public boolean gainFuel() {
        return false;
    }

    @Override
    public boolean needsFuel() {
        return false;
    }

    /** Measures the light again, at the block above the panel. */
    public void updateSunVisibility() {
        skyLight = getSkyLight(world, pos.up());
    }

    /**
     * How much sunlight reaches the position, 0 to 1: the sky light there times the sun's
     * brightness, which rain and thunder dim except in sandy biomes. None under a ceiling.
     */
    public static float getSkyLight(World world, BlockPos pos) {
        if (world == null || world.getDimension().hasCeiling()) {
            return 0.0F;
        }
        float sunBrightness = MathHelper.clamp((float) Math.cos(world.getSkyAngleRadians(1.0F)) * 2.0F + 0.2F, 0.0F, 1.0F);
        if (!BiomeDictionary.hasType(world.getBiome(pos), BiomeDictionary.Type.SANDY)) {
            sunBrightness *= 1.0F - world.getRainGradient(1.0F) * 5.0F / 16.0F;
            sunBrightness *= 1.0F - world.getThunderGradient(1.0F) * 5.0F / 16.0F;
            sunBrightness = MathHelper.clamp(sunBrightness, 0.0F, 1.0F);
        }
        return world.getLightLevel(LightType.SKY, pos) / 15.0F * sunBrightness;
    }

    /** The sun icon of the window lights up while the panel sees the sun. */
    @Override
    public boolean getGuiCondition(String name) {
        return "sunlight".equals(name) ? skyLight > 0.0F : super.getGuiCondition(name);
    }

    @Override
    protected boolean delayActiveUpdate() {
        return true;
    }
}
