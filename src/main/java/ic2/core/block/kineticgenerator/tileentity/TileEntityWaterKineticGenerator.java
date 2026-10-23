package ic2.core.block.kineticgenerator.tileentity;

import ic2.api.item.IKineticRotor;
import ic2.core.init.IC2Blocks;
import ic2.core.init.MainConfig;
import ic2.core.ref.TeBlock;
import ic2.core.util.ConfigUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;

/**
 * Fabric port of TileEntityWaterKineticGenerator - a water turbine.
 *
 * <p>It only turns in an ocean or a river, its rotor in water all round. In an ocean the tide does
 * the work: the flow follows a sine over the day, stronger the further the turbine stands from the
 * coast. In a river the current is steadier, again stronger away from the banks, and wears the
 * rotor half as fast. The output is {@code flow * 0.2} kU a tick.
 */
public class TileEntityWaterKineticGenerator extends TileEntityRotorKineticGenerator {
    public BiomeState type = BiomeState.UNKNOWN;
    private boolean rightFacing;
    private int distanceToNormalBiome;
    private int crossSection;
    private int obstructedCrossSection;
    private int waterFlow;

    public TileEntityWaterKineticGenerator(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type == null ? IC2Blocks.WATER_KINETIC_BE : type, pos, state, TeBlock.water_kinetic_generator, IKineticRotor.GearboxType.WATER);
    }

    public TileEntityWaterKineticGenerator(BlockPos pos, BlockState state) {
        this(IC2Blocks.WATER_KINETIC_BE, pos, state);
    }

    private static float outputModifier() {
        return 0.2F * (float) ConfigUtil.getDouble(MainConfig.get(), "balance/energy/kineticgenerator/water");
    }

    @Override
    protected int getTickRate() {
        return 20;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (world != null && !world.isClient) {
            updateSeaInfo();
        }
    }

    @Override
    public void setFacing(Direction facing) {
        super.setFacing(facing);
        if (world != null && !world.isClient) {
            updateSeaInfo();
        }
    }

    @Override
    public void tick() {
        if (world == null || world.isClient || updateTicker++ % getTickRate() != 0) {
            return;
        }

        if (type == BiomeState.UNKNOWN) {
            RegistryEntry<Biome> biome = world.getBiome(pos);
            if (BiomeDictionary.hasType(biome, BiomeDictionary.Type.OCEAN)) {
                type = BiomeState.OCEAN;
            } else if (BiomeDictionary.hasType(biome, BiomeDictionary.Type.RIVER)) {
                type = BiomeState.RIVER;
            } else {
                type = BiomeState.INVALID;
                return;
            }
        }
        if (type == BiomeState.INVALID) {
            return;
        }

        boolean active = hasRotor() && checkSpace(1, true) == 0;
        if (active) {
            crossSection = square(getRotorDiameter() / 2 * 2 * 2 + 1);
            obstructedCrossSection = checkSpace(getRotorDiameter() * 3, false);
            if (obstructedCrossSection > 0 && obstructedCrossSection <= (getRotorDiameter() + 1) / 2) {
                obstructedCrossSection = 0;
            }

            int rotorDamage = 0;
            if (obstructedCrossSection < 0) {
                stopSpinning();
            } else if (type == BiomeState.OCEAN) {
                float diff = (float) Math.sin(world.getTimeOfDay() * Math.PI / 6000.0);
                diff *= Math.abs(diff);
                rotationSpeed = (float) (diff * distanceToNormalBiome / 100.0F
                        * (1.0 - Math.pow((double) obstructedCrossSection / crossSection, 2.0)));
                waterFlow = (int) (rotationSpeed * 3000.0F);
                if (rightFacing) {
                    rotationSpeed *= -1.0F;
                }
                waterFlow = (int) (waterFlow * getEfficiency());
                rotorDamage = 2;
            } else if (type == BiomeState.RIVER) {
                rotationSpeed = Math.max(20, Math.min(50, distanceToNormalBiome)) / 50.0F;
                waterFlow = (int) (rotationSpeed * 1000.0F);
                if (getFacing() == Direction.EAST || getFacing() == Direction.NORTH) {
                    rotationSpeed *= -1.0F;
                }
                waterFlow = (int) (waterFlow * getEfficiency()
                        * (1.0F - 0.3F * world.random.nextFloat() - 0.1F * ((float) obstructedCrossSection / crossSection)));
                rotorDamage = 1;
            }
            rotorSlot.damage(rotorDamage, false);
        } else {
            stopSpinning();
        }

        setActive(active);
        markDirty();
    }

    private static int square(int x) {
        return x * x;
    }

    protected void stopSpinning() {
        rotationSpeed = 0.0F;
        waterFlow = 0;
    }

    /** A rotor in a river is two thirds the size it is in the sea. */
    @Override
    public int getRotorDiameter() {
        int diameter = super.getRotorDiameter();
        return type == BiomeState.OCEAN ? diameter : (diameter + 1) * 2 / 3;
    }

    @Override
    protected boolean blocksRotor(BlockPos at) {
        return !world.getBlockState(at).isOf(Blocks.WATER);
    }

    /** How far the turbine is from land, along its axis, which is what makes the flow strong. */
    public void updateSeaInfo() {
        Direction facing = getFacing();
        for (int distance = 1; distance < 200; distance++) {
            if (!isValidBiome(world.getBiome(pos.offset(facing, distance)))) {
                distanceToNormalBiome = distance;
                rightFacing = true;
                return;
            }
            if (!isValidBiome(world.getBiome(pos.offset(facing, -distance)))) {
                distanceToNormalBiome = distance;
                rightFacing = false;
                return;
            }
        }
        distanceToNormalBiome = 200;
        rightFacing = true;
    }

    public static boolean isValidBiome(RegistryEntry<Biome> biome) {
        return BiomeDictionary.hasType(biome, BiomeDictionary.Type.OCEAN) || BiomeDictionary.hasType(biome, BiomeDictionary.Type.RIVER);
    }

    @Override
    public int getKuOutput() {
        return getActive() ? (int) Math.abs(waterFlow * outputModifier()) : 0;
    }

    @Override
    public boolean getGuiCondition(String name) {
        return "validBiome".equals(name) ? type != BiomeState.INVALID : super.getGuiCondition(name);
    }

    public enum BiomeState {
        UNKNOWN,
        OCEAN,
        RIVER,
        INVALID
    }
}
