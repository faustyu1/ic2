package ic2.core.client.render;

import ic2.core.item.tool.EntityMiningLaser;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

/**
 * Fabric port of {@code ic2.core.item.tool.RenderCrossed}: a laser shot drawn as an arrow is - a
 * back plate and four crossed side planes - with {@code textures/models/laser.png}, at full
 * brightness since it glows.
 */
public class MiningLaserRenderer extends EntityRenderer<EntityMiningLaser> {
    private static final Identifier TEXTURE = Identifier.of("ic2", "textures/models/laser.png");
    private static final int FULL_BRIGHT = 0xF000F0;

    public MiningLaserRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public void render(EntityMiningLaser entity, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light) {
        if (entity.prevYaw == 0.0F && entity.prevPitch == 0.0F) {
            return;
        }

        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(MathHelper.lerp(tickDelta, entity.prevYaw, entity.getYaw()) - 90.0F));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(MathHelper.lerp(tickDelta, entity.prevPitch, entity.getPitch())));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(45.0F));
        float scale = 0.05625F;
        matrices.scale(scale, scale, scale);
        matrices.translate(-4.0F, 0.0F, 0.0F);

        VertexConsumer buffer = vertexConsumers.getBuffer(RenderLayer.getEntityCutout(TEXTURE));
        MatrixStack.Entry entry = matrices.peek();
        // the back, both faces
        vertex(entry, buffer, -7, -2, -2, 0.0F, 0.15625F, -1, 0, 0);
        vertex(entry, buffer, -7, -2, 2, 0.15625F, 0.15625F, -1, 0, 0);
        vertex(entry, buffer, -7, 2, 2, 0.15625F, 0.3125F, -1, 0, 0);
        vertex(entry, buffer, -7, 2, -2, 0.0F, 0.3125F, -1, 0, 0);
        vertex(entry, buffer, -7, 2, -2, 0.0F, 0.15625F, 1, 0, 0);
        vertex(entry, buffer, -7, 2, 2, 0.15625F, 0.15625F, 1, 0, 0);
        vertex(entry, buffer, -7, -2, 2, 0.15625F, 0.3125F, 1, 0, 0);
        vertex(entry, buffer, -7, -2, -2, 0.0F, 0.3125F, 1, 0, 0);

        for (int side = 0; side < 4; side++) {
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0F));
            vertex(entry, buffer, -8, -2, 0, 0.0F, 0.0F, 0, 1, 0);
            vertex(entry, buffer, 8, -2, 0, 0.5F, 0.0F, 0, 1, 0);
            vertex(entry, buffer, 8, 2, 0, 0.5F, 0.15625F, 0, 1, 0);
            vertex(entry, buffer, -8, 2, 0, 0.0F, 0.15625F, 0, 1, 0);
        }

        matrices.pop();
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    private static void vertex(MatrixStack.Entry entry, VertexConsumer buffer, int x, int y, int z, float u, float v,
                               int normalX, int normalY, int normalZ) {
        buffer.vertex(entry, x, y, z)
                .color(0xFFFFFFFF)
                .texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(FULL_BRIGHT)
                .normal(entry, normalX, normalY, normalZ);
    }

    @Override
    public Identifier getTexture(EntityMiningLaser entity) {
        return TEXTURE;
    }
}
