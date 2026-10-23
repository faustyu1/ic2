package ic2.core.block.wiring;

import ic2.api.energy.tile.IColoredEnergyTile;
import ic2.api.energy.tile.IEnergyConductor;
import ic2.core.block.BlockFoam;
import ic2.core.block.BlockWall;
import ic2.core.block.TileEntityBlock;
import ic2.core.block.TileEntityWall;
import ic2.core.block.comp.Obscuration;
import ic2.core.init.IC2ClassicBlocks;
import ic2.core.ref.TeBlock;
import ic2.core.util.Ic2Color;
import ic2.core.init.IC2Blocks;
import net.fabricmc.fabric.api.blockview.v2.RenderDataBlockEntity;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * Fabric port of TileEntityCable - a cable's part in the energy net: its type, insulation, paint
 * and loss.
 *
 * <p>The paint lives in the block state of a {@link BlockColoredCable}, the arms in the
 * connection properties of {@link BlockCable}.
 *
 * <p>A cable can be buried in construction foam - sprayed with the foam sprayer, or a foam block
 * used on it - and keeps working inside. Soft foam fills the block but can be walked through, and
 * sets on its own as loose foam does, 300 * (16 - light) seconds on average, or at once when sand
 * is used on it. Set, the cable sits inside a light grey foam wall: solid, dark, as hard to mine
 * as a wall. The painter colours that wall (and wipes what the obscurator put on it), the
 * obscurator dresses its faces up as other blocks. Breaking a foamed cable only breaks the foam
 * away - nothing drops - and leaves the bare cable. The foam state is the {@code foam} property
 * of the block state; the wall's colour and faces are kept here.
 */
public class TileEntityCable extends TileEntityBlock implements IEnergyConductor, IColoredEnergyTile, RenderDataBlockEntity {

    protected CableType cableType = CableType.copper;
    protected int insulation = 0;
    /** The colour of the foam wall around the cable; light grey until painted. */
    private Ic2Color foamColor = BlockWall.DEFAULT_COLOR;
    private final Obscuration obscuration = new Obscuration(this::onFoamAppearanceChanged);

    public TileEntityCable(BlockEntityType<?> type, BlockPos pos, BlockState state, TeBlock teBlock, CableType cableType, int insulation) {
        super(type, pos, state, teBlock);
        this.cableType = cableType;
        this.insulation = insulation;
    }

    public TileEntityCable(BlockEntityType<?> type, BlockPos pos, BlockState state, CableType cableType, int insulation) {
        super(type, pos, state);
        this.cableType = cableType;
        this.insulation = insulation;
    }

    public TileEntityCable(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public TileEntityCable(BlockPos pos, BlockState state) {
        this(IC2Blocks.CABLE_BE, pos, state);
    }

    /**
     * The block entity of a cable block: a detector or splitter cable gets its own kind, every
     * other cable the plain one - TeBlock.Delegated in 1.12.2.
     */
    public static TileEntityCable create(BlockPos pos, BlockState state) {
        if (state != null && state.getBlock() instanceof BlockCable cable) {
            if (cable.getCableType() == CableType.detector) {
                return new TileEntityCableDetector(pos, state);
            }
            if (cable.getCableType() == CableType.splitter) {
                return new TileEntityCableSplitter(pos, state);
            }
        }
        return new TileEntityCable(pos, state);
    }

    /**
     * Takes the cable's kind from the block it belongs to.
     *
     * <p>Every variant is its own block since the flattening, so the block is what knows whether
     * this is a bare tin cable or a triple insulated iron one.
     */
    @Override
    public void onLoad() {
        if (world != null && getCachedState().getBlock() instanceof BlockCable cable) {
            this.cableType = cable.getCableType();
            this.insulation = cable.getInsulation();
        }
        super.onLoad();
    }

    public CableType getCableType() {
        return cableType;
    }

    public int getInsulation() {
        return insulation;
    }

    /**
     * Whether the cable takes that paint. A bare cable must take paint at all and not have it
     * already; soft foam takes none; a foam wall takes any colour but the cable's own - 1.12.2
     * compared the paint with the cable inside, not with the wall, and that is kept.
     */
    public boolean canBeColored(Ic2Color color) {
        BlockState state = getCachedState();
        return switch (getFoam()) {
            case soft -> false;
            case hardened -> (state.contains(BlockColoredCable.COLOR) ? state.get(BlockColoredCable.COLOR) : Ic2Color.black) != color;
            case none -> state.contains(BlockColoredCable.COLOR) && state.get(BlockColoredCable.COLOR) != color;
        };
    }

    /** The cable's paint; black for an unpainted cable, null for one that takes no paint. */
    public Ic2Color getColor() {
        BlockState state = getCachedState();
        return state.contains(BlockColoredCable.COLOR) ? state.get(BlockColoredCable.COLOR) : null;
    }

    /**
     * Paints the cable. It leaves the energy net and joins it again, so the net sees which
     * neighbours it still connects to, and its arms are recomputed for the new colour.
     *
     * @return false when the cable takes no paint or already has it
     */
    public boolean recolor(Ic2Color color) {
        if (world == null || !canBeColored(color)) {
            return false;
        }
        if (world.isClient) {
            return true;
        }
        if (getFoam() != CableFoam.none) {
            // the paint goes onto the foam wall and covers whatever the obscurator put there
            foamColor = color;
            obscuration.clear();
            onFoamAppearanceChanged();
            return true;
        }

        onUnloaded();
        BlockState state = getCachedState().with(BlockColoredCable.COLOR, color);
        if (state.getBlock() instanceof BlockCable cable) {
            state = cable.withConnections(state, world, pos);
        }
        world.setBlockState(pos, state, net.minecraft.block.Block.NOTIFY_ALL);
        onLoad();
        markDirty();
        return true;
    }

    /** Painted cables only talk to unpainted ones and to their own colour. */
    public boolean canInteractWith(ic2.api.energy.tile.IEnergyTile tile, Direction side) {
        if (!(tile instanceof IColoredEnergyTile other)) {
            return true;
        }
        DyeColor thisColor = getColor(side);
        DyeColor otherColor = other.getColor(side.getOpposite());
        return thisColor == null || otherColor == null || thisColor == otherColor;
    }

    // IEnergyConductor
    @Override public double getConductionLoss() { return cableType.loss; }
    @Override public double getConductorBreakdownEnergy() { return cableType.capacity + 1; }
    @Override public double getInsulationBreakdownEnergy() { return cableType.capacity + 1; }
    @Override public double getInsulationEnergyAbsorption() { return 0; }
    @Override public boolean acceptsEnergyFrom(ic2.api.energy.tile.IEnergyEmitter emitter, Direction side) { return canInteractWith(emitter, side); }
    @Override public boolean emitsEnergyTo(ic2.api.energy.tile.IEnergyAcceptor receiver, Direction side) { return canInteractWith(receiver, side); }
    @Override public void removeInsulation() { if (insulation > 0) { insulation--; markDirty(); } }
    @Override public void removeConductor() { /* drop cable */ markDirty(); }

    // IColoredEnergyTile
    @Override public DyeColor getColor(Direction side) {
        return BlockCable.colorOf(getCachedState());
    }

    // NBT
    @Override public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        nbt.putString("cableType", cableType.getName());
        nbt.putInt("insulation", insulation);
        nbt.putByte("foamColor", (byte) foamColor.ordinal());
        NbtCompound obscurationNbt = obscuration.writeToNbt();
        if (obscurationNbt != null) {
            nbt.put("obscuration", obscurationNbt);
        }
    }

    @Override public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        if (nbt.contains("cableType")) {
            CableType t = CableType.get(nbt.getString("cableType"));
            if (t != null) cableType = t;
        }
        if (nbt.contains("insulation")) insulation = nbt.getInt("insulation");
        int rawFoamColor = nbt.getByte("foamColor") & 255;
        foamColor = nbt.contains("foamColor") && rawFoamColor < Ic2Color.values.length ? Ic2Color.values[rawFoamColor] : BlockWall.DEFAULT_COLOR;
        obscuration.readFromNbt(nbt.contains("obscuration") ? nbt.getCompound("obscuration") : null);

        // a sync from the server may have changed the foam wall's look, so it is drawn again
        if (world != null && world.isClient) {
            BlockState state = getCachedState();
            world.updateListeners(pos, state, state, Block.NOTIFY_ALL);
        }
    }

    // ---------------------------------------------------------------- construction foam

    /** How far the cable is buried in foam, read off its block state. */
    public CableFoam getFoam() {
        BlockState state = getCachedState();
        return state.contains(BlockCable.FOAM) ? state.get(BlockCable.FOAM) : CableFoam.none;
    }

    public boolean isFoamed() {
        return getFoam() != CableFoam.none;
    }

    /**
     * Buries the cable in soft foam, which is what the foam sprayer does to it.
     *
     * @return false when it already was
     */
    public boolean foam() {
        return changeFoam(CableFoam.soft);
    }

    public Ic2Color getFoamColor() {
        return foamColor;
    }

    public Obscuration getObscuration() {
        return obscuration;
    }

    /**
     * The obscurator. Only a cable in hardened foam takes it: 1.12.2 also stored faces on a bare
     * cable, used up the energy and showed nothing, and the faces were wiped before the next
     * foam could set anyway.
     */
    public boolean applyObscuration(Direction side, Obscuration.ObscurationData data) {
        return getFoam() == CableFoam.hardened && obscuration.applyObscuration(side, data);
    }

    /**
     * Puts the cable into the given foam state. Anything but a hardened wall loses the wall's
     * colour and obscured faces.
     *
     * @return false when nothing changed
     */
    public boolean changeFoam(CableFoam foam) {
        BlockState state = getCachedState();
        if (world == null || !state.contains(BlockCable.FOAM) || state.get(BlockCable.FOAM) == foam) {
            return false;
        }
        if (world.isClient) {
            return true;
        }

        if (foam != CableFoam.hardened) {
            foamColor = BlockWall.DEFAULT_COLOR;
            obscuration.clear();
        }
        // the new state carries this block entity's data to the clients with it
        world.setBlockState(pos, state.with(BlockCable.FOAM, foam), Block.NOTIFY_ALL);
        markDirty();
        return true;
    }

    /** Soft foam sets with the odds loose foam has, checked every tick as 1.12.2 did. */
    @Override
    public void tick() {
        super.tick();
        if (world == null || world.isClient || getFoam() != CableFoam.soft) {
            return;
        }
        if (world.random.nextFloat() < BlockFoam.getHardenChance(world, pos, getCachedState())) {
            changeFoam(CableFoam.hardened);
        }
    }

    /** Sand sets soft foam at once; a construction foam block buries a bare cable. */
    @Override
    public boolean onActivated(PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        CableFoam foam = getFoam();
        if (foam == CableFoam.soft && stack.isOf(Items.SAND)) {
            consumeOne(player, stack);
            changeFoam(CableFoam.hardened);
            return true;
        }
        if (foam == CableFoam.none && IC2ClassicBlocks.FOAM != null && stack.isOf(IC2ClassicBlocks.FOAM.asItem())) {
            consumeOne(player, stack);
            foam();
            return true;
        }
        return super.onActivated(player, hand);
    }

    private static void consumeOne(PlayerEntity player, ItemStack stack) {
        if (!player.getAbilities().creativeMode) {
            stack.decrement(1);
        }
    }

    /**
     * Breaking a foamed cable breaks the foam away and keeps the cable - onRemovedByPlayer in
     * 1.12.2, which refused the removal. Fabric lets a break be cancelled before it happens.
     */
    public static void registerFoamBreaking() {
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (blockEntity instanceof TileEntityCable cable && cable.isFoamed()) {
                cable.changeFoam(CableFoam.none);
                return false;
            }
            return true;
        });
    }

    private void onFoamAppearanceChanged() {
        markDirty();
        if (world != null && !world.isClient) {
            BlockState state = getCachedState();
            world.updateListeners(pos, state, state, Block.NOTIFY_LISTENERS);
        }
    }

    /** What the cable model draws once the foam has set: the wall's colour and faces. */
    @Override
    public Object getRenderData() {
        return new TileEntityWall.WallRenderState(foamColor, obscuration.getRenderState());
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registryLookup) {
        return createNbt(registryLookup);
    }

    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    // For TeBlock compatibility
    public static Class<? extends TileEntityCable> delegate() { return TileEntityCable.class; }

    /** The wrench leaves it alone, as in 1.12.2. */
    @Override
    public boolean wrenchCanRemove(net.minecraft.entity.player.PlayerEntity player) {
        return false;
    }
}
