package ic2.core.block.wiring;

import ic2.api.energy.EnergyNet;
import ic2.api.energy.tile.IEnergyAcceptor;
import ic2.api.energy.tile.IEnergyEmitter;
import ic2.api.energy.tile.IEnergySink;
import ic2.api.energy.tile.IMultiEnergySource;
import ic2.core.block.TileEntityInventory;
import ic2.core.ref.TeBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * Fabric port of TileEntityTransformer - the LV, MV, HV and EV transformers.
 *
 * <p>A transformer links its own voltage and the next higher one. Stepping down it takes the high
 * voltage in through its face and gives out four packets of its own tier on every other side;
 * stepping up it takes its own voltage in on the five other sides and sends one packet of the
 * higher tier out of the face. It buffers eight packets of its own tier and only sends a packet
 * once it has the whole of it.
 *
 * <p>The window's buttons choose the mode: by redstone (the default - stepping up while powered,
 * the block's active look), always down or always up.
 */
public abstract class TileEntityTransformer extends TileEntityInventory implements IEnergySink, IMultiEnergySource {

    public enum Mode {
        redstone, stepdown, stepup;

        public static final Mode[] VALUES = values();
    }

    private static final Mode DEFAULT_MODE = Mode.redstone;

    protected final int defaultTier;
    private final double capacity;
    protected double energyStored;
    private Mode configuredMode = DEFAULT_MODE;
    /** The mode in effect, never {@link Mode#redstone}; null until the transformer first looked. */
    private Mode transformMode = null;
    private int sinkTier;
    private int sourceTier;
    private int packets = 1;
    private double inputFlow;
    private double outputFlow;

    public TileEntityTransformer(BlockEntityType<?> type, BlockPos pos, BlockState state, TeBlock teBlock, int tier) {
        super(type, pos, state, teBlock);
        this.defaultTier = tier;
        this.sinkTier = tier;
        this.sourceTier = tier;
        this.capacity = EnergyNet.instance.getPowerFromTier(tier) * 8.0;
    }

    public TileEntityTransformer(BlockEntityType<?> type, BlockPos pos, BlockState state, int tier) {
        this(type, pos, state, TeBlock.DUMMY, tier);
    }

    /** LV, MV, HV or EV, after the voltage the transformer gives out stepping down. */
    public String getVoltageName() {
        return switch (defaultTier) {
            case 1 -> "LV";
            case 2 -> "MV";
            case 3 -> "HV";
            case 4 -> "EV";
            default -> "";
        };
    }

    public int getTier() {
        return defaultTier;
    }

    public Mode getMode() {
        return configuredMode;
    }

    public void setMode(Mode mode) {
        configuredMode = mode;
        markDirty();
        if (world != null && !world.isClient) {
            updateRedstone(false);
        }
    }

    public boolean isStepUp() {
        return transformMode == Mode.stepup;
    }

    public double getEnergy() {
        return energyStored;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (world != null && !world.isClient) {
            updateRedstone(true);
        }
    }

    @Override
    public void tick() {
        if (world == null || world.isClient) {
            return;
        }
        updateRedstone(false);
    }

    @Override
    public void setFacing(Direction facing) {
        super.setFacing(facing);
        if (world != null && !world.isClient) {
            updateRedstone(true);
        }
    }

    /**
     * Picks the direction of transformation and, when it changed, the tiers, the packet count and
     * which sides take and give energy. The energy net is told by leaving and rejoining it, as the
     * energy component did when its directions changed.
     */
    private void updateRedstone(boolean force) {
        Mode newMode = switch (configuredMode) {
            case redstone -> world.isReceivingRedstonePower(pos) ? Mode.stepup : Mode.stepdown;
            case stepdown, stepup -> configuredMode;
        };

        if (!force && transformMode == newMode) {
            return;
        }

        boolean directionsChanged = transformMode != newMode;
        transformMode = newMode;
        setActive(isStepUp());
        if (isStepUp()) {
            sourceTier = defaultTier + 1;
            sinkTier = defaultTier;
            packets = 1;
        } else {
            sourceTier = defaultTier;
            sinkTier = defaultTier + 1;
            packets = 4;
        }

        outputFlow = EnergyNet.instance.getPowerFromTier(sourceTier);
        inputFlow = EnergyNet.instance.getPowerFromTier(sinkTier);

        if ((directionsChanged || force) && isAddedToEnet()) {
            EnergyNet.instance.removeTile(this);
            EnergyNet.instance.addTile(this);
        }
    }

    // ---------------------------------------------------------------- energy

    /** Stepping up the face is the output, stepping down it is the input. */
    @Override
    public boolean acceptsEnergyFrom(IEnergyEmitter emitter, Direction side) {
        return isStepUp() ? side != getFacing() : side == getFacing();
    }

    @Override
    public boolean emitsEnergyTo(IEnergyAcceptor receiver, Direction side) {
        return isStepUp() ? side == getFacing() : side != getFacing();
    }

    @Override
    public double getDemandedEnergy() {
        return energyStored < capacity ? capacity - energyStored : 0.0;
    }

    @Override
    public double injectEnergy(Direction directionFrom, double amount, double voltage) {
        energyStored += amount;
        markDirty();
        return 0.0;
    }

    @Override
    public int getSinkTier() {
        return sinkTier;
    }

    /** Only whole packets leave: nothing is offered until a packet of the output tier is buffered. */
    @Override
    public double getOfferedEnergy() {
        return energyStored >= EnergyNet.instance.getPowerFromTier(sourceTier) ? energyStored : 0.0;
    }

    @Override
    public void drawEnergy(double amount) {
        energyStored -= amount;
        markDirty();
    }

    @Override
    public int getSourceTier() {
        return sourceTier;
    }

    @Override
    public boolean sendMultipleEnergyPackets() {
        return true;
    }

    /** Up to four packets stepping down, one stepping up, and never more than are buffered. */
    @Override
    public int getMultipleEnergyPacketAmount() {
        return Math.min(packets, (int) Math.floor(energyStored / EnergyNet.instance.getPowerFromTier(sourceTier)));
    }

    // ---------------------------------------------------------------- window

    /**
     * The "Input" line of the window. 1.12.2 swapped the two flows when stepping up, so the window
     * shows the higher voltage as input and the lower as output in either mode; kept as it was.
     */
    public double getInputFlow() {
        return isStepUp() ? outputFlow : inputFlow;
    }

    /** The "Output" line of the window, the lower voltage in either mode. */
    public double getOutputFlow() {
        return isStepUp() ? inputFlow : outputFlow;
    }

    /** Clicks on the three mode buttons; the 1.12.2 window sent the mode's index. */
    @Override
    public void onGuiEvent(PlayerEntity player, String event) {
        if (event.startsWith("mode")) {
            try {
                int mode = Integer.parseInt(event.substring(4));
                if (mode >= 0 && mode < Mode.VALUES.length) {
                    setMode(Mode.VALUES[mode]);
                }
            } catch (NumberFormatException ignored) {
                // not one of the mode buttons
            }
        }
    }

    /** The wrench beside the button of the chosen mode. */
    @Override
    public ItemStack getGuiItem(String name) {
        if (name.equals("mode" + configuredMode.ordinal())) {
            return new ItemStack(Registries.ITEM.get(Identifier.of("ic2", "wrench")));
        }
        return super.getGuiItem(name);
    }

    @Override
    public double getGuiAmount(String name) {
        return switch (name) {
            case "inputflow" -> getInputFlow();
            case "outputflow" -> getOutputFlow();
            default -> super.getGuiAmount(name);
        };
    }

    @Override
    public void addInformation(ItemStack stack, java.util.List<Text> tooltip) {
        tooltip.add(Text.translatable("item.tooltip.Low").append(" " + (long) EnergyNet.instance.getPowerFromTier(defaultTier) + " ")
                .append(Text.translatable("generic.text.EUt")).append(" ")
                .append(Text.translatable("item.tooltip.High")).append(" " + (long) EnergyNet.instance.getPowerFromTier(defaultTier + 1) + " ")
                .append(Text.translatable("generic.text.EUt")));
    }

    // ---------------------------------------------------------------- saving

    @Override
    public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        nbt.putInt("mode", configuredMode.ordinal());
        nbt.putDouble("energy", energyStored);
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        int mode = nbt.getInt("mode");
        configuredMode = mode >= 0 && mode < Mode.VALUES.length ? Mode.VALUES[mode] : DEFAULT_MODE;
        energyStored = nbt.getDouble("energy");
    }
}
