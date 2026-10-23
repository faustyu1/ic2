package ic2.core.block.comp;

import ic2.core.block.BlockWall;
import ic2.core.block.TileEntityWall;
import ic2.core.block.wiring.TileEntityCable;
import ic2.core.init.IC2Blocks;
import java.util.Arrays;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Port of the Obscuration component: the faces of a foam wall (or of a hardened foam cable) that
 * the obscurator has dressed up as another block.
 *
 * <p>Each of the six faces may carry one {@link ObscurationData}: the block state that was
 * scanned, which of its faces, and the colour multipliers its quads had where it was scanned
 * (grass is green in one biome and yellow in another). The client draws that face of the
 * scanned block over the wall's face.
 */
public class Obscuration {
    private final Runnable changeHandler;
    private ObscurationData[] dataMap;

    /** @param changeHandler run whenever a face changes, to save and send the change */
    public Obscuration(Runnable changeHandler) {
        this.changeHandler = changeHandler;
    }

    public boolean applyObscuration(Direction side, ObscurationData data) {
        if (dataMap != null && data.equals(dataMap[side.ordinal()])) {
            return false;
        }
        if (dataMap == null) {
            dataMap = new ObscurationData[Direction.values().length];
        }
        dataMap[side.ordinal()] = data;
        changeHandler.run();
        return true;
    }

    public void clear() {
        // unlike 1.12.2 a clear that changes nothing does not resend the block
        if (dataMap != null) {
            dataMap = null;
            changeHandler.run();
        }
    }

    public boolean hasObscuration() {
        return dataMap != null;
    }

    /** The face data by {@link Direction#ordinal()}, or null when no face is obscured. */
    public ObscurationData @Nullable [] getRenderState() {
        return dataMap == null ? null : Arrays.copyOf(dataMap, dataMap.length);
    }

    /** The faces as NBT, one compound per obscured face named after it; null when there is none. */
    public @Nullable NbtCompound writeToNbt() {
        if (dataMap == null) {
            return null;
        }
        NbtCompound ret = new NbtCompound();
        for (Direction facing : Direction.values()) {
            ObscurationData data = dataMap[facing.ordinal()];
            if (data != null) {
                ret.put(facing.asString(), data.toNbt());
            }
        }
        return ret;
    }

    public void readFromNbt(@Nullable NbtCompound nbt) {
        dataMap = null;
        if (nbt == null || nbt.isEmpty()) {
            return;
        }
        for (Direction facing : Direction.values()) {
            if (nbt.contains(facing.asString(), NbtElement.COMPOUND_TYPE)) {
                ObscurationData data = ObscurationData.fromNbt(nbt.getCompound(facing.asString()));
                if (data != null) {
                    if (dataMap == null) {
                        dataMap = new ObscurationData[Direction.values().length];
                    }
                    dataMap[facing.ordinal()] = data;
                }
            }
        }
    }

    /**
     * What the obscurator's RetextureEvent did in 1.12.2: dresses the face {@code side} of the
     * block at {@code pos} up as the scanned face. A hardened foam cable and a foam wall take it;
     * a plain wall first turns into the wall block that can carry obscured faces, keeping its
     * colour, and turns back if the face is not taken.
     *
     * @return whether the face changed, which is what costs the obscurator its energy
     */
    public static boolean retexture(World world, BlockPos pos, Direction side, ObscurationData data) {
        if (world.getBlockEntity(pos) instanceof TileEntityCable cable) {
            return cable.applyObscuration(side, data);
        }
        if (world.getBlockEntity(pos) instanceof TileEntityWall wall) {
            return wall.getObscuration().applyObscuration(side, data);
        }

        BlockState state = world.getBlockState(pos);
        if (state.getBlock() instanceof BlockWall plain && IC2Blocks.WALL_BLOCK != null) {
            if (world.setBlockState(pos, IC2Blocks.WALL_BLOCK.getDefaultState())
                    && world.getBlockEntity(pos) instanceof TileEntityWall wall) {
                wall.setColor(plain.color);
                if (wall.getObscuration().applyObscuration(side, data)) {
                    return true;
                }
            }
            world.setBlockState(pos, state);
        }
        return false;
    }

    /** One obscured face: which face of which block state it shows, tinted how. */
    public static final class ObscurationData {
        public final BlockState state;
        public final Direction side;
        public final int[] colorMultipliers;

        public ObscurationData(BlockState state, Direction side, int[] colorMultipliers) {
            this.state = state;
            this.side = side;
            this.colorMultipliers = colorMultipliers;
        }

        public NbtCompound toNbt() {
            NbtCompound nbt = new NbtCompound();
            nbt.put("state", NbtHelper.fromBlockState(state));
            nbt.putByte("side", (byte) side.ordinal());
            nbt.putIntArray("colorMuls", colorMultipliers);
            return nbt;
        }

        /** The data again, or null when the block is gone or the entry is broken. */
        public static @Nullable ObscurationData fromNbt(NbtCompound nbt) {
            BlockState state = readState(nbt.getCompound("state"));
            int rawSide = nbt.getByte("side");
            int[] colorMultipliers = nbt.getIntArray("colorMuls");
            if (state == null || rawSide < 0 || rawSide >= Direction.values().length || colorMultipliers.length == 0) {
                return null;
            }
            return new ObscurationData(state, Direction.byId(rawSide), colorMultipliers);
        }

        /** A block state written with {@link NbtHelper#fromBlockState}; null for an unknown or air block. */
        public static @Nullable BlockState readState(NbtCompound nbt) {
            if (nbt.isEmpty()) {
                return null;
            }
            RegistryEntryLookup<Block> blocks = Registries.BLOCK.getReadOnlyWrapper();
            BlockState state = NbtHelper.toBlockState(blocks, nbt);
            return state.isAir() ? null : state;
        }

        @Override
        public boolean equals(Object obj) {
            return obj == this || obj instanceof ObscurationData o
                    && o.state.equals(state)
                    && o.side == side
                    && Arrays.equals(o.colorMultipliers, colorMultipliers);
        }

        @Override
        public int hashCode() {
            return (state.hashCode() * 7 + side.ordinal()) * 23;
        }

        @Override
        public String toString() {
            return "Obscuration<" + state + ", " + side + ", " + Arrays.toString(colorMultipliers) + '>';
        }
    }
}
