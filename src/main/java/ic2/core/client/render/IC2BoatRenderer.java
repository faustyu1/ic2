package ic2.core.client.render;

import ic2.core.entity.EntityIC2Boat;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.model.BoatEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Quaternionf;

/**
 * Draws an IC2 boat: the vanilla boat model with the boat's own texture, as RenderIC2Boat did.
 * The textures have the 1.12.2 boat layout, which the 1.21 model still uses.
 */
public class IC2BoatRenderer extends EntityRenderer<EntityIC2Boat> {
    private final BoatEntityModel model;
    private final Identifier texture;

    public IC2BoatRenderer(EntityRendererFactory.Context ctx, String texture) {
        super(ctx);
        this.shadowRadius = 0.8F;
        this.model = new BoatEntityModel(ctx.getPart(EntityModelLayers.createBoat(BoatEntity.Type.OAK)));
        this.texture = Identifier.of("ic2", "textures/models/" + texture + ".png");
    }

    @Override
    public void render(EntityIC2Boat boat, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        matrices.push();
        matrices.translate(0.0F, 0.375F, 0.0F);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F - yaw));
        float wobbleTicks = boat.getDamageWobbleTicks() - tickDelta;
        float wobbleStrength = Math.max(0.0F, boat.getDamageWobbleStrength() - tickDelta);
        if (wobbleTicks > 0.0F) {
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(
                    MathHelper.sin(wobbleTicks) * wobbleTicks * wobbleStrength / 10.0F * boat.getDamageWobbleSide()));
        }

        float bubbleWobble = boat.interpolateBubbleWobble(tickDelta);
        if (!MathHelper.approximatelyEquals(bubbleWobble, 0.0F)) {
            matrices.multiply(new Quaternionf().setAngleAxis(bubbleWobble * 0.017453292F, 1.0F, 0.0F, 1.0F));
        }

        matrices.scale(-1.0F, -1.0F, 1.0F);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90.0F));
        model.setAngles(boat, tickDelta, 0.0F, -0.1F, 0.0F, 0.0F);
        VertexConsumer vertices = vertexConsumers.getBuffer(model.getLayer(texture));
        model.render(matrices, vertices, light, OverlayTexture.DEFAULT_UV);
        if (!boat.isSubmergedInWater()) {
            ModelPart waterPatch = model.getWaterPatch();
            waterPatch.render(matrices, vertexConsumers.getBuffer(RenderLayer.getWaterMask()), light, OverlayTexture.DEFAULT_UV);
        }
        matrices.pop();
        super.render(boat, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    @Override
    public Identifier getTexture(EntityIC2Boat boat) {
        return texture;
    }
}
