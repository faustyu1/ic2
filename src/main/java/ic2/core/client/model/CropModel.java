package ic2.core.client.model;

import ic2.core.crop.TileEntityCrop;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.renderer.v1.Renderer;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.ForwardingBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.texture.Sprite;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;

/**
 * Port of CropModel: draws a crop stick as four crossed planes, the vanilla crop layout, with
 * the plant's texture for its size, or the stick's when nothing grows on it.
 *
 * <p>1.12.2 baked one model per (crop, size, cross-crop) and picked it through an extended block
 * state. Here the model wraps the JSON stick model and emits the planes itself, reading the
 * crop from the block entity's render data, so the chunk mesh still carries the plant and no
 * block entity renderer runs per frame.
 */
public class CropModel extends ForwardingBakedModel {
    private static final Identifier STICK = Identifier.of("ic2", "block/crop/stick");
    private static final Identifier UPGRADED_STICK = Identifier.of("ic2", "block/crop/stick_upgraded");
    /** The planes sit one pixel low, so the plant seems to grow out of the farmland. */
    private static final float Y_OFFSET = -0.0625F;

    private RenderMaterial material;

    public CropModel(BakedModel wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public boolean isVanillaAdapter() {
        return false;
    }

    @Override
    public void emitBlockQuads(BlockRenderView blockView, BlockState state, BlockPos pos, Supplier<Random> randomSupplier, RenderContext context) {
        Sprite sprite = sprite(blockView.getBlockEntityRenderData(pos));
        QuadEmitter emitter = context.getEmitter();
        RenderMaterial material = material();

        for (float plane : new float[] {0.25F, 0.75F}) {
            // each plane is seen from both sides, so it is emitted once per side
            emitPlane(emitter, material, sprite, Direction.EAST, 1.0F - plane);
            emitPlane(emitter, material, sprite, Direction.WEST, plane);
            emitPlane(emitter, material, sprite, Direction.SOUTH, 1.0F - plane);
            emitPlane(emitter, material, sprite, Direction.NORTH, plane);
        }
    }

    private static void emitPlane(QuadEmitter emitter, RenderMaterial material, Sprite sprite, Direction face, float depth) {
        emitter.square(face, 0.0F, 0.0F, 1.0F, 1.0F, depth);
        emitter.spriteBake(sprite, MutableQuadView.BAKE_LOCK_UV);
        for (int vertex = 0; vertex < 4; vertex++) {
            emitter.pos(vertex, emitter.x(vertex), emitter.y(vertex) + Y_OFFSET, emitter.z(vertex));
        }
        emitter.color(-1, -1, -1, -1);
        emitter.material(material);
        emitter.emit();
    }

    /** The texture for what the stick carries; a missing texture shows as the missing sprite. */
    private static Sprite sprite(Object renderData) {
        Identifier id = STICK;

        if (renderData instanceof TileEntityCrop.CropRenderState state) {
            if (state.crop() != null && state.size() > 0) {
                var textures = state.crop().getTexturesLocation();
                id = atlasId(textures.get(Math.min(state.size(), textures.size()) - 1));
            } else if (state.crosscrop()) {
                id = UPGRADED_STICK;
            }
        }

        return MinecraftClient.getInstance().getSpriteAtlas(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE).apply(id);
    }

    /**
     * The crop cards name their textures the 1.12.2 way, {@code ic2:blocks/crop/wheat_1}; the
     * atlas holds them under {@code block/} since the flattening.
     */
    static Identifier atlasId(Identifier texture) {
        String path = texture.getPath();
        return path.startsWith("blocks/") ? Identifier.of(texture.getNamespace(), "block/" + path.substring("blocks/".length())) : texture;
    }

    /** Cutout like a vanilla crop, without ambient occlusion or face shading - the stick is flat. */
    private RenderMaterial material() {
        if (material == null) {
            Renderer renderer = RendererAccess.INSTANCE.getRenderer();
            material = renderer.materialFinder()
                    .blendMode(BlendMode.CUTOUT)
                    .ambientOcclusion(TriState.FALSE)
                    .disableDiffuse(true)
                    .find();
        }
        return material;
    }
}
