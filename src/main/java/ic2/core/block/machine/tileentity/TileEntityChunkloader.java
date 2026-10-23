package ic2.core.block.machine.tileentity;

import ic2.api.upgrade.IUpgradableBlock;
import ic2.api.upgrade.UpgradableProperty;
import ic2.core.ChunkLoaderLogic;
import ic2.core.block.invslot.InvSlotDischarge;
import ic2.core.block.invslot.InvSlotUpgrade;
import ic2.core.init.IC2Blocks;
import ic2.core.init.MainConfig;
import ic2.core.ref.TeBlock;
import ic2.core.util.ConfigUtil;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtLong;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

/**
 * Fabric port of TileEntityChunkloader - the chunk loader.
 *
 * <p>Keeps its own chunk and up to 24 more within four chunks of it loaded, chosen on the 9x9
 * map in its window; its own chunk cannot be switched off. It uses {@code euPerChunk} (1) EU a
 * tick for every chosen chunk and lets them all go while it lacks the energy. 2500 EU, tier 1;
 * energy storage and transformer upgrades raise both.
 */
public class TileEntityChunkloader extends TileEntityElectricMachine implements IUpgradableBlock, InvSlotUpgrade.Listener {
    /** How far, in chunks, the loader reaches from its own chunk. */
    public static final int RANGE = 4;

    private final Set<ChunkPos> loadedChunks = new HashSet<>();
    public final InvSlotUpgrade upgradeSlot;
    public final InvSlotDischarge dischargeSlot;
    private final double euPerChunk = ConfigUtil.getDouble(MainConfig.get(), "balance/euPerChunk");

    public TileEntityChunkloader(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type == null ? IC2Blocks.CHUNK_LOADER_BE : type, pos, state, TeBlock.chunk_loader, 2500, 1);
        this.upgradeSlot = new InvSlotUpgrade(this, "upgrade", 4);
        this.dischargeSlot = new InvSlotDischarge(this, "discharge", 1);
        onUpgradesChanged();
    }

    public TileEntityChunkloader(BlockPos pos, BlockState state) {
        this(null, pos, state);
    }

    @Override
    public void tick() {
        if (world == null || world.isClient) {
            return;
        }

        drainDischargeSlot(dischargeSlot);
        boolean active = useEnergy(loadedChunks.size() * euPerChunk);
        if (active != getActive()) {
            setActive(active);
        }
        upgradeSlot.tick();
    }

    /** Switching on forces the chosen chunks, switching off lets them go, as taking and releasing the ticket did. */
    @Override
    public void setActive(boolean active) {
        if (world instanceof ServerWorld serverWorld && getActive() != active) {
            if (active) {
                for (ChunkPos chunk : loadedChunks) {
                    ChunkLoaderLogic.addChunk(serverWorld, pos, chunk, false);
                }
            } else {
                ChunkLoaderLogic.release(serverWorld, pos, loadedChunks);
            }
        }
        super.setActive(active);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        // the loader's own chunk is always among them, as the ticket always held it
        if (loadedChunks.add(ChunkLoaderLogic.getChunkCoords(pos))) {
            markDirty();
        }
        // a loader that was working when the world was saved finds its chunks still forced and claims them again
        if (world instanceof ServerWorld serverWorld && getActive()) {
            for (ChunkPos chunk : loadedChunks) {
                ChunkLoaderLogic.addChunk(serverWorld, pos, chunk, true);
            }
        }
    }

    @Override
    public void onUnloaded() {
        super.onUnloaded();
        if (world instanceof ServerWorld serverWorld) {
            ChunkLoaderLogic.forget(serverWorld, pos);
        }
    }

    @Override
    public void onBlockRemoved() {
        super.onBlockRemoved();
        if (world instanceof ServerWorld serverWorld && getActive()) {
            ChunkLoaderLogic.release(serverWorld, pos, loadedChunks);
        }
    }

    @Override
    public void onPlaced(ItemStack stack, LivingEntity placer) {
        super.onPlaced(stack, placer);
        loadedChunks.add(ChunkLoaderLogic.getChunkCoords(pos));
        markDirty();
    }

    /** Adds a chunk in range, unless the loader already holds as many as it may. */
    public void addChunkToLoaded(ChunkPos chunk) {
        if (!isChunkInRange(chunk) || loadedChunks.size() >= ChunkLoaderLogic.MAX_CHUNKS_PER_LOADER) {
            return;
        }
        if (getActive() && world instanceof ServerWorld serverWorld) {
            ChunkLoaderLogic.addChunk(serverWorld, pos, chunk, false);
        }
        loadedChunks.add(chunk);
        markDirty();
    }

    /** Lets a chunk go; the loader's own chunk stays. */
    public void removeChunkFromLoaded(ChunkPos chunk) {
        if (ChunkLoaderLogic.getChunkCoords(pos).equals(chunk)) {
            return;
        }
        if (getActive() && world instanceof ServerWorld serverWorld) {
            ChunkLoaderLogic.removeChunk(serverWorld, pos, chunk);
        }
        loadedChunks.remove(chunk);
        markDirty();
    }

    public Set<ChunkPos> getLoadedChunks() {
        return Collections.unmodifiableSet(loadedChunks);
    }

    public boolean isChunkInRange(ChunkPos chunk) {
        ChunkPos mainChunk = ChunkLoaderLogic.getChunkCoords(pos);
        return Math.abs(chunk.x - mainChunk.x) <= RANGE && Math.abs(chunk.z - mainChunk.z) <= RANGE;
    }

    /** The chunk of a map cell; cells run 0..8 from the north-west corner. */
    private ChunkPos cellChunk(int column, int row) {
        ChunkPos mainChunk = ChunkLoaderLogic.getChunkCoords(pos);
        return new ChunkPos(mainChunk.x + column - RANGE, mainChunk.z + row - RANGE);
    }

    /** A click on map cell {@code chunk:<column>:<row>} switches that chunk. */
    @Override
    public void onGuiEvent(PlayerEntity player, String event) {
        String[] parts = event.split(":");
        if (parts.length != 3 || !parts[0].equals("chunk")) {
            return;
        }

        ChunkPos chunk;
        try {
            chunk = cellChunk(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
        } catch (NumberFormatException e) {
            return;
        }
        if (!isChunkInRange(chunk)) {
            return;
        }

        if (loadedChunks.contains(chunk)) {
            removeChunkFromLoaded(chunk);
        } else {
            addChunkToLoaded(chunk);
        }
    }

    /** {@code chunk_<column>_<row>}: whether the chunk of that map cell is chosen. */
    @Override
    public boolean getGuiCondition(String name) {
        if (name.startsWith("chunk_")) {
            String[] parts = name.split("_");
            if (parts.length == 3) {
                return loadedChunks.contains(cellChunk(Integer.parseInt(parts[1]), Integer.parseInt(parts[2])));
            }
        }
        if (name.equals("main_chunk")) {
            return true;
        }
        return super.getGuiCondition(name);
    }

    @Override
    public double getGuiAmount(String name) {
        return "chunks".equals(name) ? loadedChunks.size() : super.getGuiAmount(name);
    }

    @Override
    public double getGuiCapacity(String name) {
        return "chunks".equals(name) ? ChunkLoaderLogic.MAX_CHUNKS_PER_LOADER : super.getGuiCapacity(name);
    }

    @Override
    public void onUpgradesChanged() {
        setTier(upgradeSlot.getTier(defaultTier));
        setCapacity(upgradeSlot.getEnergyStorage(defaultEnergyStorage, 0, 0));
    }

    @Override
    public Set<UpgradableProperty> getUpgradableProperties() {
        return EnumSet.of(UpgradableProperty.EnergyStorage, UpgradableProperty.ItemConsuming, UpgradableProperty.ItemProducing,
                UpgradableProperty.Transformer);
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.readNbt(nbt, lookup);
        loadedChunks.clear();
        NbtList list = nbt.getList("loadedChunks", NbtElement.LONG_TYPE);
        for (int i = 0; i < list.size(); i++) {
            loadedChunks.add(new ChunkPos(((NbtLong) list.get(i)).longValue()));
        }
    }

    @Override
    public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.writeNbt(nbt, lookup);
        NbtList list = new NbtList();
        for (ChunkPos chunk : loadedChunks) {
            list.add(NbtLong.of(chunk.toLong()));
        }
        nbt.put("loadedChunks", list);
    }
}
