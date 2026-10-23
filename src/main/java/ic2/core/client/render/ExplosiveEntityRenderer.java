package ic2.core.client.render;

import ic2.core.block.EntityIC2Explosive;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.TntMinecartEntityRenderer;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

/**
 * Fabric port of {@code ic2.core.block.RenderExplosiveBlock}: lit industrial TNT or a lit nuke,
 * drawn as its block, flashing white and swelling in its last half second like vanilla TNT.
 */
public class ExplosiveEntityRenderer extends EntityRenderer<EntityIC2Explosive> {
    private final BlockRenderManager blockRenderManager;

    public ExplosiveEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.shadowRadius = 0.5F;
        this.blockRenderManager = context.getBlockRenderManager();
    }

    @Override
    public void render(EntityIC2Explosive entity, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light) {
        matrices.push();
        matrices.translate(0.0F, 0.5F, 0.0F);
        int fuse = entity.getFuse();
        if (fuse - tickDelta + 1.0F < 10.0F) {
            float scale = 1.0F - (fuse - tickDelta + 1.0F) / 10.0F;
            scale = MathHelper.clamp(scale, 0.0F, 1.0F);
            scale *= scale;
            scale *= scale;
            scale = 1.0F + scale * 0.3F;
            matrices.scale(scale, scale, scale);
        }

        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-90.0F));
        matrices.translate(-0.5F, -0.5F, 0.5F);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90.0F));
        TntMinecartEntityRenderer.renderFlashingBlock(blockRenderManager, entity.getRenderBlockState(), matrices, vertexConsumers,
                light, fuse / 5 % 2 == 0);
        matrices.pop();
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    @Override
    @SuppressWarnings("deprecation")
    public Identifier getTexture(EntityIC2Explosive entity) {
        return SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE;
    }
}
