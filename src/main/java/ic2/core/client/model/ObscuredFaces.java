package ic2.core.client.model;

import ic2.core.block.comp.Obscuration;
import ic2.core.item.tool.ItemObscurator;
import ic2.core.network.ObscuratorScanPayload;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.texture.Sprite;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * The client half of the obscurator: which textures make up a face of a block, as
 * ItemObscurator.getRenderInfo worked them out from the baked model in 1.12.2, and the scan
 * that sends them to the server.
 */
public final class ObscuredFaces {
    /** Vertex data stride of a baked block quad: position, colour, uv, light, normal. */
    private static final int STRIDE = 8;
    private static final float EPSILON = 1.0E-5F;

    private ObscuredFaces() {}

    /**
     * One texture layer of a face: the sprite, the atlas uv at the face's low corner and at the
     * opposite corner, and the tint index of the quad it came from.
     */
    public record Layer(Sprite sprite, float uStart, float vStart, float uEnd, float vEnd, int tint) {}

    /**
     * The texture layers of {@code side} of the block state's model, or null when the face
     * cannot be copied: a translucent block, a face without quads on the block's side, or one
     * whose quads do not cover the whole side as a square.
     */
    public static @Nullable List<Layer> getRenderInfo(BlockState state, Direction side) {
        if (state.isAir() || RenderLayers.getBlockLayer(state) == RenderLayer.getTranslucent()) {
            return null;
        }
        BakedModel model = MinecraftClient.getInstance().getBlockRenderManager().getModel(state);
        if (model == null) {
            return null;
        }
        List<BakedQuad> quads = model.getQuads(state, side, Random.create(0L));
        if (quads.isEmpty()) {
            return null;
        }

        int dx = side.getOffsetX();
        int dy = side.getOffsetY();
        int dz = side.getOffsetZ();
        // the corner of the face nearest to the origin
        int xS = (dx + 1) / 2;
        int yS = (dy + 1) / 2;
        int zS = (dz + 1) / 2;

        List<Layer> ret = new ArrayList<>(quads.size());
        float[] p = new float[12];
        float[] uv = new float[8];
        for (BakedQuad quad : quads) {
            int[] data = quad.getVertexData();
            if (data.length < 4 * STRIDE) {
                continue;
            }
            for (int v = 0; v < 4; v++) {
                p[v * 3] = Float.intBitsToFloat(data[v * STRIDE]);
                p[v * 3 + 1] = Float.intBitsToFloat(data[v * STRIDE + 1]);
                p[v * 3 + 2] = Float.intBitsToFloat(data[v * STRIDE + 2]);
                uv[v * 2] = Float.intBitsToFloat(data[v * STRIDE + 4]);
                uv[v * 2 + 1] = Float.intBitsToFloat(data[v * STRIDE + 5]);
            }

            int first = -1;
            for (int v = 0; v < 4; v++) {
                if (similar(p[v * 3], xS) && similar(p[v * 3 + 1], yS) && similar(p[v * 3 + 2], zS)) {
                    first = v;
                    break;
                }
            }
            if (first == -1) {
                continue;
            }

            // the quad must be a unit square with its normal along the side
            float[] e1 = {p[3] - p[0], p[4] - p[1], p[5] - p[2]};
            float[] e4 = {p[9] - p[0], p[10] - p[1], p[11] - p[2]};
            float[] e3 = {p[9] - p[6], p[10] - p[7], p[11] - p[8]};
            if (!similar(lengthSq(e1), 1) || !similar(lengthSq(e4), 1)
                    || !similar(lengthSq(new float[] {e3[0] + e1[0], e3[1] + e1[1], e3[2] + e1[2]}), 0)) {
                continue;
            }
            float nx = e1[1] * e4[2] - e1[2] * e4[1];
            float ny = e1[2] * e4[0] - e1[0] * e4[2];
            float nz = e1[0] * e4[1] - e1[1] * e4[0];
            if (!similar(lengthSq(new float[] {nx - dx, ny - dy, nz - dz}), 0)) {
                continue;
            }

            int opposite = (first + 2) % 4;
            ret.add(new Layer(quad.getSprite(), uv[first * 2], uv[first * 2 + 1], uv[opposite * 2], uv[opposite * 2 + 1], quad.getColorIndex()));
        }
        return ret.isEmpty() ? null : ret;
    }

    /**
     * The scan: the face's layers and the colours the block gives them where it stands. Sends
     * them to the server unless the obscurator holds this very face already.
     */
    public static boolean scan(ItemStack stack, PlayerEntity player, World world, BlockPos pos, Direction side, int slot) {
        BlockState state = world.getBlockState(pos);
        List<Layer> layers = getRenderInfo(state, side);
        if (layers == null) {
            return false;
        }

        int[] colorMultipliers = new int[layers.size()];
        for (int i = 0; i < colorMultipliers.length; i++) {
            int tint = layers.get(i).tint();
            // an untinted layer stays white; 1.12.2 asked the block's colour for it too, which
            // tinted the dirt of a grass block's side green
            colorMultipliers[i] = tint == -1 ? -1 : MinecraftClient.getInstance().getBlockColors().getColor(state, world, pos, tint);
        }

        Obscuration.ObscurationData stored = ItemObscurator.getData(stack);
        if (stored != null && stored.state.equals(state) && stored.side == side && Arrays.equals(stored.colorMultipliers, colorMultipliers)) {
            return false;
        }
        if (!ClientPlayNetworking.canSend(ObscuratorScanPayload.ID)) {
            return false;
        }
        ClientPlayNetworking.send(new ObscuratorScanPayload(slot, Block.getRawIdFromState(state), side.ordinal(), colorMultipliers));
        return true;
    }

    private static float lengthSq(float[] v) {
        return v[0] * v[0] + v[1] * v[1] + v[2] * v[2];
    }

    private static boolean similar(float a, float b) {
        return Math.abs(a - b) < EPSILON;
    }
}
