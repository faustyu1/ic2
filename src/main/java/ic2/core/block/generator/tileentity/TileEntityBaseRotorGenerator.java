package ic2.core.block.generator.tileentity;

import ic2.api.tile.IRotorProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public abstract class TileEntityBaseRotorGenerator extends TileEntityBaseGenerator implements IRotorProvider {

    private static final float ROTATION_SPEED = 0.4f;
    private static final Identifier ROTOR_TEXTURE = Identifier.of("ic2", "textures/item/rotor/iron_rotor_model.png");

    private final int rotorDiameter;
    private float angle = 0.0f;
    private long lastCheck = System.currentTimeMillis();

    public TileEntityBaseRotorGenerator(BlockEntityType<?> type, BlockPos pos, BlockState state, ic2.core.ref.TeBlock teBlock,
                                        double production, int tier, int maxStorage, int rotorDiameter) {
        super(type, pos, state, teBlock, production, tier, maxStorage);
        this.rotorDiameter = rotorDiameter;
    }

    public TileEntityBaseRotorGenerator(BlockEntityType<?> type, BlockPos pos, BlockState state, double production, int tier, int maxStorage, int rotorDiameter) {
        super(type, pos, state, production, tier, maxStorage);
        this.rotorDiameter = rotorDiameter;
    }

    public TileEntityBaseRotorGenerator(double production, int tier, int maxStorage, int rotorDiameter) {
        super(null, BlockPos.ORIGIN, null, production, tier, maxStorage);
        this.rotorDiameter = rotorDiameter;
    }

    @Override
    public int getRotorDiameter() {
        return rotorDiameter;
    }

    protected abstract boolean shouldRotorRotate();

    protected float rotorSpeedFactor() {
        return 1.0f;
    }

    @Override
    public float getAngle() {
        if (shouldRotorRotate()) {
            angle += (System.currentTimeMillis() - lastCheck) * ROTATION_SPEED * rotorSpeedFactor();
            angle %= 360.0f;
        }
        lastCheck = System.currentTimeMillis();
        return angle;
    }

    @Override
    public Identifier getRotorRenderTexture() {
        return ROTOR_TEXTURE;
    }

    public String getOperationSoundFile() {
        return null;
    }
}
