package ic2.api.energy.prefab;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;

/**
 * Base {@link BlockEntity} that owns a {@link BasicEnergyTile} buffer and wires up
 * NBT serialization and the removal lifecycle.
 *
 * <p>Note: the original 1.12.2 version relied on {@code TileEntity.onLoad()} /
 * {@code onChunkUnload()}. Modern {@link BlockEntity} has no {@code onLoad()} hook,
 * so {@link #onLoad()} (and {@link BasicEnergyTile#update()}) must be invoked by the
 * concrete block entity's tick to lazily register with the energy net.
 */
public class BasicEnergyTe<T extends BasicEnergyTile> extends BlockEntity {
    protected T energyBuffer;

    protected BasicEnergyTe(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public T getEnergyBuffer() {
        return energyBuffer;
    }

    public void onLoad() {
        energyBuffer.onLoad();
    }

    @Override
    public void markRemoved() {
        super.markRemoved();
        energyBuffer.invalidate();
    }

    public void onChunkUnload() {
        energyBuffer.onChunkUnload();
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        energyBuffer.readFromNBT(nbt);
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        energyBuffer.writeToNBT(nbt);
    }

    public static class Sink extends BasicEnergyTe<BasicSink> {
        public Sink(BlockEntityType<?> type, BlockPos pos, BlockState state, int capacity, int tier) {
            super(type, pos, state);
            this.energyBuffer = new BasicSink(this, (double) capacity, tier);
        }
    }

    public static class Source extends BasicEnergyTe<BasicSource> {
        public Source(BlockEntityType<?> type, BlockPos pos, BlockState state, int capacity, int tier) {
            super(type, pos, state);
            this.energyBuffer = new BasicSource(this, (double) capacity, tier);
        }
    }
}
