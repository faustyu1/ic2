package ic2.core.block;

import ic2.api.energy.EnergyNet;
import ic2.api.energy.tile.IEnergyTile;
import ic2.api.network.INetworkDataProvider;
import ic2.api.network.INetworkUpdateListener;
import ic2.core.ref.TeBlock;
import java.util.List;
import java.util.Set;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

/**
 * Minimal Fabric port of TileEntityBlock.
 * Original was 998 lines with Forge capabilities, extended blockstates, components.
 * This version keeps only facing/active, NBT, and EnergyNet integration.
 */
public abstract class TileEntityBlock extends BlockEntity implements INetworkDataProvider, INetworkUpdateListener {

    protected final TeBlock teBlock;
    private boolean active = false;
    private Direction facing = Direction.NORTH;
    private boolean addedToEnet = false;

    public TileEntityBlock(BlockEntityType<?> type, BlockPos pos, BlockState state, TeBlock teBlock) {
        super(type != null ? type : typeOf(state), pos, state);
        this.teBlock = teBlock;
        if (state != null && state.contains(BlockTileEntity.FACING)) {
            this.facing = state.get(BlockTileEntity.FACING);
        }
        if (state != null && state.contains(BlockTileEntity.ACTIVE)) {
            this.active = state.get(BlockTileEntity.ACTIVE);
        }
    }

    /** Puts facing and activity into the block state, which is how the client learns of them. */
    private void updateBlockState() {
        if (world == null || world.isClient) {
            return;
        }
        BlockState state = world.getBlockState(pos);
        if (state.getBlock() != getCachedState().getBlock()) {
            return;
        }
        // machines carry both; the detector and splitter cables only show whether they are active
        BlockState updated = state;
        if (state.contains(BlockTileEntity.FACING)) {
            updated = updated.with(BlockTileEntity.FACING, facing);
        }
        if (state.contains(BlockTileEntity.ACTIVE)) {
            updated = updated.with(BlockTileEntity.ACTIVE, active);
        }
        if (updated != state) {
            world.setBlockState(pos, updated, net.minecraft.block.Block.NOTIFY_LISTENERS);
        }
    }

    /**
     * The type of the block entity a block holds. Many port classes pass no type from their
     * (pos, state) constructor, which is the one the game calls when it loads a chunk; without a
     * type the block entity could not be saved again, so it is looked up from the block.
     */
    private static BlockEntityType<?> typeOf(BlockState state) {
        return state != null && state.getBlock() instanceof BlockTileEntity block ? block.getBlockEntityType() : null;
    }

    // For subclasses that use a dummy BlockEntityType (common for stubs)
    public TileEntityBlock(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        this(type, pos, state, TeBlock.DUMMY);
    }

    public TeBlock getTeBlock() {
        return teBlock;
    }

    public Direction getFacing() {
        return facing;
    }

    public void setFacing(Direction facing) {
        if (facing == null) throw new NullPointerException("null facing");
        if (this.facing != facing) {
            this.facing = facing;
            markDirty();
            updateBlockState();
        }
    }

    public Set<Direction> getSupportedFacings() {
        return getDropTeBlock().getSupportedFacings();
    }

    public boolean getActive() {
        return active;
    }

    public void setActive(boolean active) {
        if (this.active != active) {
            this.active = active;
            markDirty();
            updateBlockState();
        }
    }

    /**
     * Whether the block joins the energy net on its own when it loads; a part of a bigger
     * structure (a reactor chamber) joins as one of the sub tiles of its core instead.
     */
    protected boolean joinsEnergyNetItself() {
        return true;
    }

    public boolean isAddedToEnet() {
        return addedToEnet;
    }

    /**
     * The tanks this block owns, created on first use.
     *
     * <p>The original kept them in a Fluids component registered with addComponent; the port has
     * no component system, so the block holds the container directly and saves it with its NBT.
     */
    protected ic2.core.block.comp.Fluids fluids() {
        if (fluids == null) {
            fluids = new ic2.core.block.comp.Fluids(this::markDirty);
        }
        return fluids;
    }

    /**
     * What a side offers to pipes and neighbouring machines: by default the tanks of the block
     * that side may fill or drain, null for a block without tanks. Fabric finds it through
     * {@code FluidStorage.SIDED} (see {@link ic2.core.block.comp.Fluids#register}).
     */
    public net.fabricmc.fabric.api.transfer.v1.storage.Storage<net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant> getFluidHandler(net.minecraft.util.math.Direction side) {
        return fluids == null ? null : fluids.getHandler(side);
    }

    /** The tanks of the block, or null when it never made any. */
    public ic2.core.block.comp.Fluids getFluidsComponent() {
        return fluids;
    }

    /** @return the named tank, or null when this block has none */
    public ic2.core.block.comp.Fluids.InternalFluidTank getFluidTank(String name) {
        return fluids == null ? null : fluids.getFluidTank(name);
    }

    /**
     * Right click on the block, before the GUI opens - a tank takes the bucket in the player's
     * hand this way, exactly as onActivated did in 1.12.2.
     *
     * @return true when the block handled the click and the GUI must stay closed
     */
    public boolean onActivated(net.minecraft.entity.player.PlayerEntity player, net.minecraft.util.Hand hand) {
        return false;
    }

    /** The weak redstone signal the block gives off to every side; 0 unless the block says otherwise. */
    public int getRedstoneOutput() {
        return 0;
    }

    /**
     * An entity is inside the block's space, as onEntityCollidedWithBlock in 1.12.2 - the charge
     * pads charge whoever stands on them this way.
     */
    public void onEntityCollision(net.minecraft.entity.Entity entity) {
    }

    /** The signal a comparator reads from this block; 0 unless the block says otherwise. */
    public int getComparatorOutput() {
        return 0;
    }

    /** A neighbouring block changed, which is when a redstone-driven block looks at its signal. */
    public void onNeighborUpdate() {
    }

    /**
     * The block is caught in an explosion, before it is cleared.
     *
     * @return true when the block took care of its own removal, as an explosive does by going off
     */
    public boolean onExploded(net.minecraft.world.explosion.Explosion explosion) {
        return false;
    }

    /**
     * A player breaks the block.
     *
     * @return true when the block took care of its own removal and leaves nothing behind
     */
    public boolean onRemovedByPlayer(net.minecraft.entity.player.PlayerEntity player) {
        return false;
    }

    /**
     * The block is gone for good - broken, blown up or replaced - as onBlockBreak in 1.12.2;
     * unlike {@link #onUnloaded()} this is not called when only the chunk unloads.
     * Runs on the server.
     */
    public void onBlockRemoved() {
    }

    /** A projectile - an arrow, say - hit the block. */
    public void onProjectileHit(net.minecraft.entity.projectile.ProjectileEntity projectile) {
    }

    private ic2.core.block.comp.Fluids fluids;

    @Override
    public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        nbt.putString("facing", facing.getName());
        nbt.putBoolean("active", active);
        if (teBlock != null) {
            nbt.putString("teBlock", teBlock.getName());
        }
        if (fluids != null && !fluids.isEmpty()) {
            nbt.put("Fluids", fluids.writeToNbt());
        }
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        if (nbt.contains("facing")) {
            try {
                facing = Direction.byName(nbt.getString("facing"));
                if (facing == null) facing = Direction.NORTH;
            } catch (Exception e) {
                facing = Direction.NORTH;
            }
        }
        if (nbt.contains("active")) {
            active = nbt.getBoolean("active");
        }
        if (fluids != null && nbt.contains("Fluids")) {
            fluids.readFromNbt(nbt.getCompound("Fluids"));
        }
    }

    // Fabric BlockEntity lifecycle: mark that we need to handle onLoad
    // In 1.21, BlockEntity has no onLoad, but we can hook via World.addBlockEntity
    // For now, use a simple method that can be called from BlockTileEntity.onPlaced
    @SuppressWarnings("unchecked")
    private static <T extends net.minecraft.block.entity.BlockEntity & IEnergyTile> void addTileHelper(T tile) {
        EnergyNet.instance.addTile(tile);
    }

    @SuppressWarnings("unchecked")
    private static <T extends net.minecraft.block.entity.BlockEntity & IEnergyTile> void removeTileHelper(T tile) {
        EnergyNet.instance.removeTile(tile);
    }

    /**
     * Value a guidef gauge shows, as a 0..1 ratio.
     *
     * <p>The guidef names the value ({@code <gauge name="progress">}); the original resolved it
     * through the same hook, which is what keeps the XML independent of the machine class.
     *
     * @return the ratio, or 0 when this block has no such value
     */
    public double getGuiValue(String name) {
        return 0.0;
    }

    /**
     * The number behind a gauge, e.g. the EU in the buffer, which the gauge's tooltip shows.
     *
     * @return the value, or 0 when this block has no number to show for that gauge
     */
    public double getGuiAmount(String name) {
        return 0.0;
    }

    /** The number a gauge is full at, e.g. the size of the energy buffer. */
    public double getGuiCapacity(String name) {
        return 0.0;
    }

    /**
     * The second running sound to play in place of the machine's usual one, e.g. the reactor's
     * Geiger counter, which ticks faster with the output; null keeps the usual one.
     */
    public net.minecraft.sound.SoundEvent getExtraLoopSound() {
        return null;
    }

    /** The EU in the block's buffer, as overlays such as Jade show it. */
    public double getEnergyForDisplay() {
        return 0.0;
    }

    /** The size of the block's EU buffer, or 0 when it has none. */
    public double getEnergyCapacityForDisplay() {
        return 0.0;
    }

    /**
     * State of a condition a guidef {@code <only if="...">} tests, e.g. whether the block cutter's
     * blade is too weak. The original read the same hook through IGuiConditionProvider.
     *
     * @return false when this block does not know the condition, which hides the element
     */
    public boolean getGuiCondition(String name) {
        return false;
    }

    /**
     * Handles a click on a guidef {@code <button>}, named by the button's {@code event} attribute.
     * Runs on the server, where the screen handler dispatches the vanilla button packet.
     */
    public void onGuiEvent(net.minecraft.entity.player.PlayerEntity player, String event) {
    }

    /**
     * Server tick, driven by the block's {@link net.minecraft.block.entity.BlockEntityTicker}.
     * The original ticked through Forge's ITickable; on Fabric the block hands out the ticker.
     */
    public void tick() {
    }

    // ---------------------------------------------------------------- drops and the wrench

    /** The item this block is, as pick block gives it: the block's own item by default. */
    public net.minecraft.item.ItemStack getPickStack() {
        return new net.minecraft.item.ItemStack(getCachedState().getBlock());
    }

    /**
     * What breaking the block gives, besides the contents of its slots.
     *
     * @param wrench whether it was taken with a wrench, which always gives the block itself;
     *               mined otherwise, a machine only gives back its casing
     */
    public java.util.List<net.minecraft.item.ItemStack> getSelfDrops(boolean wrench) {
        TeBlock block = getDropTeBlock();
        if (!block.hasItem()) {
            return java.util.List.of();
        }
        net.minecraft.item.ItemStack drop = adjustDrop(getPickStack(), wrench);
        return drop == null || drop.isEmpty() ? java.util.List.of() : java.util.List.of(drop);
    }

    /**
     * Turns the block's own item into what it actually drops. A subclass that carries its
     * contents or energy in the item writes them here, after calling this.
     *
     * @return the drop, or null for none
     */
    protected net.minecraft.item.ItemStack adjustDrop(net.minecraft.item.ItemStack drop, boolean wrench) {
        if (wrench) {
            return drop;
        }

        return switch (getDropTeBlock().getDefaultDrop()) {
            case Self -> drop;
            case None -> null;
            case Generator -> new net.minecraft.item.ItemStack(ic2.core.init.IC2Blocks.getBlock(TeBlock.generator));
            case Machine -> new net.minecraft.item.ItemStack(ic2.core.init.IC2ClassicBlocks.MACHINE_BLOCK);
            case AdvMachine -> new net.minecraft.item.ItemStack(ic2.core.init.IC2ClassicBlocks.ADVANCED_MACHINE_BLOCK);
        };
    }

    /**
     * Whether the contents of the slots go into the dropped item rather than onto the ground,
     * as a storage box keeps them.
     */
    public boolean keepsContentsInDrop() {
        return false;
    }

    /** The data a block wrote into its dropped item, read back when it is placed again. */
    protected static NbtCompound dropData(net.minecraft.item.ItemStack stack) {
        return stack.getOrDefault(net.minecraft.component.DataComponentTypes.CUSTOM_DATA,
                net.minecraft.component.type.NbtComponent.DEFAULT).copyNbt();
    }

    /** Writes into the dropped item's data, keeping what is there. */
    protected static void editDropData(net.minecraft.item.ItemStack stack, java.util.function.Consumer<NbtCompound> edit) {
        NbtCompound nbt = dropData(stack);
        edit.accept(nbt);
        stack.set(net.minecraft.component.DataComponentTypes.CUSTOM_DATA, net.minecraft.component.type.NbtComponent.of(nbt));
    }

    /**
     * Called on the server when the block is placed from an item, which is where a block reads
     * back what it put into its drop.
     */
    public void onPlaced(net.minecraft.item.ItemStack stack, net.minecraft.entity.LivingEntity placer) {
    }

    /** Lines the block's item shows in its tooltip. */
    public void addInformation(net.minecraft.item.ItemStack stack, java.util.List<net.minecraft.text.Text> tooltip) {
    }

    /** Whether the wrench may turn the block to face that way. */
    public boolean canSetFacingWrench(Direction facing, net.minecraft.entity.player.PlayerEntity player) {
        TeBlock block = getDropTeBlock();
        return block.allowWrenchRotating() && facing != getFacing() && block.getSupportedFacings().contains(facing);
    }

    public boolean setFacingWrench(Direction facing, net.minecraft.entity.player.PlayerEntity player) {
        if (!canSetFacingWrench(facing, player)) {
            return false;
        }
        setFacing(facing);
        return true;
    }

    /** Whether the wrench may take the block out; a crop or a cable it may not. */
    public boolean wrenchCanRemove(net.minecraft.entity.player.PlayerEntity player) {
        return true;
    }

    /** What the wrench gets out of the block; the slots spill on their own when it is removed. */
    public java.util.List<net.minecraft.item.ItemStack> getWrenchDrops(net.minecraft.entity.player.PlayerEntity player) {
        return getSelfDrops(true);
    }

    /**
     * The TeBlock this block entity stands for. The block entities are built from their type and
     * often do not carry it, so the block they sit in is asked first.
     */
    protected TeBlock getDropTeBlock() {
        if (getCachedState() != null && getCachedState().getBlock() instanceof BlockTileEntity block && block.teBlock != TeBlock.DUMMY) {
            return block.teBlock;
        }
        return teBlock;
    }

    /**
     * Runs {@link #onLoad()} once for this block entity. The block calls it when the block is
     * placed, the ticker on the first tick after the chunk loaded - Fabric has no load event per
     * block entity, and without it a machine read back from disk never rejoined the energy net.
     */
    public void ensureLoaded() {
        if (!loaded) {
            onLoad();
        }
    }

    private boolean loaded;

    public void onLoad() {
        loaded = true;
        if (!addedToEnet && this instanceof IEnergyTile && joinsEnergyNetItself()) {
            if (world != null && !world.isClient) {
                try {
                    addTileHelper((net.minecraft.block.entity.BlockEntity & IEnergyTile) (Object) this);
                    addedToEnet = true;
                } catch (Exception e) {
                    // Fallback: try locatable path
                }
            }
        }
    }

    public void onUnloaded() {
        if (addedToEnet && this instanceof IEnergyTile) {
            try {
                removeTileHelper((net.minecraft.block.entity.BlockEntity & IEnergyTile) (Object) this);
            } catch (Exception ignored) {}
            addedToEnet = false;
        }
    }

    @Override
    public void markRemoved() {
        onUnloaded();
        super.markRemoved();
    }

    // INetworkDataProvider
    @Override
    public List<String> getNetworkedFields() {
        return List.of("facing", "active");
    }

    // INetworkUpdateListener
    @Override
    public void onNetworkUpdate(String field) {
        if ("facing".equals(field) || "active".equals(field)) {
            if (world != null) {
                world.updateListeners(pos, getCachedState(), getCachedState(), 2);
            }
        }
    }

    // For compatibility with original code that expects update() tick
    public void updateEntityServer() {}
    public void updateEntityClient() {}

    public static <T extends TileEntityBlock> T instantiate(Class<T> cls, BlockEntityType<?> type, BlockPos pos, BlockState state) {
        try {
            var ctor = cls.getDeclaredConstructor(BlockEntityType.class, BlockPos.class, BlockState.class);
            return ctor.newInstance(type, pos, state);
        } catch (Exception e) {
            try {
                return cls.getDeclaredConstructor().newInstance();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }
}
