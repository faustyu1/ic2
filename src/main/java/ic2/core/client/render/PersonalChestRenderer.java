package ic2.core.client.render;

import ic2.core.block.personal.TileEntityPersonalChest;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

/**
 * Fabric port of TileEntityPersonalChestRenderer: the safe's door, which swings open on its hinge
 * at the front right while somebody has the safe open. The case around it is the block model.
 */
public class PersonalChestRenderer implements BlockEntityRenderer<TileEntityPersonalChest> {
    private static final Identifier TEXTURE = Identifier.of("ic2", "textures/models/newsafe.png");
    /** The hinge, in block units from the north west corner. */
    private static final float HINGE_X = 0.84375F;
    private static final float HINGE_Z = 0.15625F;

    private final ModelPart door;

    public PersonalChestRenderer(BlockEntityRendererFactory.Context context) {
        ModelData data = new ModelData();
        data.getRoot().addChild("door", ModelPartBuilder.create().uv(30, 0).mirrored().cuboid(2.0F, 1.0F, 2.0F, 12, 14, 1),
                ModelTransform.NONE);
        this.door = TexturedModelData.of(data, 64, 64).createModel().getChild("door");
    }

    @Override
    public void render(TileEntityPersonalChest safe, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light, int overlay) {
        float turn = switch (safe.getFacing()) {
            case SOUTH -> 180.0F;
            case WEST -> 90.0F;
            case EAST -> -90.0F;
            default -> 0.0F;
        };
        float opening = safe.getDoorOpening(tickDelta);
        // eased like a chest lid: quick at first, slowing as it swings wide
        float shut = 1.0F - opening * opening * opening;

        matrices.push();
        matrices.translate(0.5F, 0.5F, 0.5F);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(turn));
        matrices.translate(-0.5F + HINGE_X, -0.5F, -0.5F + HINGE_Z);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(shut * 90.0F - 90.0F));
        matrices.translate(-HINGE_X, 0.0F, -HINGE_Z);
        door.render(matrices, vertexConsumers.getBuffer(RenderLayer.getEntityCutout(TEXTURE)), light, overlay);
        matrices.pop();
    }
}
