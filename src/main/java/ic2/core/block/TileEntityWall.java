package ic2.core.block;

import ic2.core.block.comp.Obscuration;
import ic2.core.init.IC2Blocks;
import ic2.core.util.Ic2Color;
import java.util.Arrays;
import java.util.List;
import net.fabricmc.fabric.api.blockview.v2.RenderDataBlockEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

/**
 * Port of TileEntityWall: a construction foam wall with faces dressed up by the obscurator.
 *
 * <p>An ordinary foam wall is a plain block ({@code ic2:wall_<colour>}); the obscurator turns it
 * into this one, {@code ic2:wall}, which keeps the wall's colour and the obscured faces. It draws
 * as the wall of its colour with the scanned faces laid over it. The painter changes its colour
 * and leaves the faces alone, as in 1.12.2.
 *
 * <p>Broken, it gives the plain wall of its colour back. In 1.12.2 it dropped the generic wall
 * tile entity item, which placed a light grey wall again and lost the colour.
 */
public class TileEntityWall extends TileEntityBlock implements RenderDataBlockEntity {
    private Ic2Color color = BlockWall.DEFAULT_COLOR;
    private final Obscuration obscuration = new Obscuration(this::onAppearanceChanged);

    public TileEntityWall(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type == null ? IC2Blocks.WALL_BE : type, pos, state);
    }

    public TileEntityWall(BlockPos pos, BlockState state) {
        this(null, pos, state);
    }

    public Ic2Color getColor() {
        return color;
    }

    public void setColor(Ic2Color color) {
        if (this.color != color) {
            this.color = color;
            onAppearanceChanged();
        }
    }

    public Obscuration getObscuration() {
        return obscuration;
    }

    /**
     * The painter.
     *
     * @return false when the wall already has that colour
     */
    public boolean recolor(Ic2Color newColor) {
        if (newColor == color) {
            return false;
        }
        if (world != null && !world.isClient) {
            setColor(newColor);
        }
        return true;
    }

    /** Saves the change and sends it to the clients that draw the wall. */
    private void onAppearanceChanged() {
        markDirty();
        if (world != null && !world.isClient) {
            BlockState state = getCachedState();
            world.updateListeners(pos, state, state, Block.NOTIFY_LISTENERS);
        }
    }

    @Override
    public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        nbt.putByte("color", (byte) color.ordinal());
        NbtCompound obscurationNbt = obscuration.writeToNbt();
        if (obscurationNbt != null) {
            nbt.put("obscuration", obscurationNbt);
        }
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        int rawColor = nbt.getByte("color") & 255;
        color = nbt.contains("color") && rawColor < Ic2Color.values.length ? Ic2Color.values[rawColor] : BlockWall.DEFAULT_COLOR;
        obscuration.readFromNbt(nbt.contains("obscuration") ? nbt.getCompound("obscuration") : null);

        // the server changed what the wall shows, so its chunk section is drawn again
        if (world != null && world.isClient) {
            BlockState state = getCachedState();
            world.updateListeners(pos, state, state, Block.NOTIFY_ALL);
        }
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registryLookup) {
        return createNbt(registryLookup);
    }

    @Override
    public @Nullable Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public Object getRenderData() {
        return new WallRenderState(color, obscuration.getRenderState());
    }

    @Override
    public ItemStack getPickStack() {
        return new ItemStack(BlockWall.get(color));
    }

    @Override
    public List<ItemStack> getSelfDrops(boolean wrench) {
        return List.of(new ItemStack(BlockWall.get(color)));
    }

    /**
     * What the wall model draws: the wall of {@code color}, with {@code obscurations} (by
     * {@link net.minecraft.util.math.Direction#ordinal()}, null entries for plain faces, or null
     * altogether) laid over it.
     */
    public record WallRenderState(Ic2Color color, Obscuration.ObscurationData @Nullable [] obscurations) {
        @Override
        public boolean equals(Object obj) {
            return obj instanceof WallRenderState o && o.color == color && Arrays.equals(o.obscurations, obscurations);
        }

        @Override
        public int hashCode() {
            return color.hashCode() * 31 + Arrays.hashCode(obscurations);
        }
    }
}
