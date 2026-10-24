package ic2.core.client.render;

import ic2.core.block.kineticgenerator.tileentity.TileEntityRotorKineticGenerator;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;

/**
 * Fabric port of KineticGeneratorRenderer: the rotor turning in front of a wind or water kinetic
 * generator.
 *
 * <p>The rotor is the 1.12.2 KineticGeneratorRotor model - four blades a pixel thick and eight
 * wide, each half the rotor's diameter long, on the 32x256 picture of the rotor item - standing
 * just off the generator's front face and turning with the speed the server sends.
 */
public class KineticRotorRenderer implements BlockEntityRenderer<TileEntityRotorKineticGenerator> {
    /** How far each blade is turned about its length, in radians. */
    private static final float BLADE_PITCH = 0.5F;
    /** One model per diameter, built the first time a rotor of that size is drawn. */
    private final Map<Integer, ModelPart> models = new HashMap<>();

    public KineticRotorRenderer(BlockEntityRendererFactory.Context context) {
    }

    private ModelPart model(int diameter) {
        return models.computeIfAbsent(diameter, d -> {
            ModelData data = new ModelData();
            ModelPartData root = data.getRoot();
            ModelPartBuilder blade = ModelPartBuilder.create().uv(0, 0).cuboid(0.0F, 0.0F, -4.0F, 1, d * 8, 8);
            for (int i = 0; i < 4; i++) {
                // each arm points its own way round the hub; the blade on it is twisted about its
                // length like a propeller's, so the rotor reads as blades rather than a flat cross
                ModelPartData arm = root.addChild("arm" + i, ModelPartBuilder.create(),
                        ModelTransform.of(0.0F, 0.0F, 0.0F, i * (float) Math.PI / 2.0F, 0.0F, 0.0F));
                arm.addChild("blade", blade, ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, BLADE_PITCH, 0.0F));
            }
            return TexturedModelData.of(data, 32, 256).createModel();
        });
    }

    @Override
    public void render(TileEntityRotorKineticGenerator generator, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light, int overlay) {
        int diameter = generator.getRotorDiameter();
        Identifier texture = generator.getRotorRenderTexture();
        // a broken generator can linger in the renderer's list of blocks drawn beyond their bounds
        // until its chunk section is rebuilt, which an emptied section never is
        if (generator.isRemoved() || diameter <= 0 || texture == null || generator.getWorld() == null) {
            return;
        }
        Direction facing = generator.getFacing();
        float angle = generator.getRotorAngle(generator.getWorld().getTime() + tickDelta);
        // the generator is a solid block, so the light inside it is none: light the rotor with the air in front
        light = WorldRenderer.getLightmapCoordinates(generator.getWorld(), generator.getPos().offset(facing));

        matrices.push();
        matrices.translate(0.5, 0.5, 0.5);
        // the model's -X is the front: turn it to face the way the generator does
        if (facing.getAxis().isHorizontal()) {
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90.0F - facing.asRotation()));
        } else {
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(facing == Direction.UP ? -90.0F : 90.0F));
        }
        // clear of the face by the depth the twisted blades take up, half their width tilted plus their thickness
        matrices.translate(-0.5 - (Math.cos(BLADE_PITCH) + 4.0 * Math.sin(BLADE_PITCH) + 0.25) / 16.0, 0.0, 0.0);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(angle));
        model(diameter).render(matrices, vertexConsumers.getBuffer(RenderLayer.getEntityCutout(texture)), light, overlay);
        matrices.pop();
    }

    /** The blades reach far past the block, so it is drawn even when the block itself is off screen. */
    @Override
    public boolean rendersOutsideBoundingBox(TileEntityRotorKineticGenerator generator) {
        return true;
    }

    @Override
    public int getRenderDistance() {
        return 128;
    }
}
