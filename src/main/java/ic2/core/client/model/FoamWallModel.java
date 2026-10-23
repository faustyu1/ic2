package ic2.core.client.model;

import ic2.core.block.BlockWall;
import ic2.core.block.TileEntityWall;
import ic2.core.block.comp.Obscuration;
import ic2.core.block.wiring.BlockCable;
import ic2.core.block.wiring.CableFoam;
import java.util.List;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.renderer.v1.Renderer;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.ForwardingBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;

/**
 * Port of RenderBlockWall and the hardened-foam branch of CableModel: draws a foam wall in its
 * colour with the faces the obscurator copied laid over it.
 *
 * <p>It wraps the model of the obscurable wall block ({@code ic2:wall}) and the models of the
 * cables. A cable only draws through it while its foam has set; otherwise its own multipart
 * model is used. The colour and the faces come from the block entity's render data
 * ({@link TileEntityWall.WallRenderState}).
 *
 * <p>Each copied texture layer becomes a quad 0.001 in front of the wall's face, its uv spanning
 * the scanned face's uv the way MergedBlockModel spread it (u along x, or z on the east and west
 * faces; v along z on the top and bottom, y elsewhere), tinted with the colour scanned with it.
 * The layers are drawn cutout, so the wall shows through the holes of a leaf texture.
 */
public class FoamWallModel extends ForwardingBakedModel {
    /** How far the copied layers sit in front of the wall's face. */
    private static final float OFFSET = 0.001F;

    private RenderMaterial material;

    public FoamWallModel(BakedModel wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public boolean isVanillaAdapter() {
        return false;
    }

    @Override
    public void emitBlockQuads(BlockRenderView blockView, BlockState state, BlockPos pos, Supplier<Random> randomSupplier, RenderContext context) {
        if (state.contains(BlockCable.FOAM) && state.get(BlockCable.FOAM) != CableFoam.hardened) {
            super.emitBlockQuads(blockView, state, pos, randomSupplier, context);
            return;
        }

        TileEntityWall.WallRenderState wall = blockView.getBlockEntityRenderData(pos) instanceof TileEntityWall.WallRenderState s
                ? s : new TileEntityWall.WallRenderState(BlockWall.DEFAULT_COLOR, null);
        BlockWall wallBlock = BlockWall.get(wall.color());
        BlockState wallState = wallBlock.getDefaultState();
        MinecraftClient.getInstance().getBlockRenderManager().getModel(wallState)
                .emitBlockQuads(blockView, wallState, pos, randomSupplier, context);

        Obscuration.ObscurationData[] faces = wall.obscurations();
        if (faces == null) {
            return;
        }
        QuadEmitter emitter = context.getEmitter();
        for (Direction face : Direction.values()) {
            Obscuration.ObscurationData data = faces[face.ordinal()];
            if (data == null) {
                continue;
            }
            List<ObscuredFaces.Layer> layers = ObscuredFaces.getRenderInfo(data.state, data.side);
            if (layers == null || layers.size() != data.colorMultipliers.length) {
                continue;
            }
            for (int i = 0; i < layers.size(); i++) {
                emitLayer(emitter, face, layers.get(i), data.colorMultipliers[i]);
            }
        }
    }

    private void emitLayer(QuadEmitter emitter, Direction face, ObscuredFaces.Layer layer, int colorMultiplier) {
        emitter.square(face, 0.0F, 0.0F, 1.0F, 1.0F, 0.0F);
        float du = layer.uEnd() - layer.uStart();
        float dv = layer.vEnd() - layer.vStart();
        for (int vertex = 0; vertex < 4; vertex++) {
            float x = emitter.x(vertex);
            float y = emitter.y(vertex);
            float z = emitter.z(vertex);
            float a = face.getAxis() == Direction.Axis.X ? z : x;
            float b = face.getAxis() == Direction.Axis.Y ? z : y;
            emitter.uv(vertex, layer.uStart() + du * a, layer.vStart() + dv * b);
            emitter.pos(vertex, x + face.getOffsetX() * OFFSET, y + face.getOffsetY() * OFFSET, z + face.getOffsetZ() * OFFSET);
        }
        int color = colorMultiplier == -1 ? -1 : 0xFF000000 | colorMultiplier;
        emitter.color(color, color, color, color);
        emitter.material(material());
        emitter.emit();
    }

    private RenderMaterial material() {
        if (material == null) {
            Renderer renderer = RendererAccess.INSTANCE.getRenderer();
            material = renderer.materialFinder().blendMode(BlendMode.CUTOUT_MIPPED).find();
        }
        return material;
    }
}
