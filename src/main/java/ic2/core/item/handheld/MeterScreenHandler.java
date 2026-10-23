package ic2.core.item.handheld;

import ic2.api.energy.EnergyNet;
import ic2.api.energy.NodeStats;
import ic2.api.energy.tile.IEnergyTile;
import ic2.core.init.IC2ScreenHandlers;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

/**
 * Fabric port of {@code ic2.core.item.tool.ContainerMeter}: the EU reader watching one energy net
 * tile.
 *
 * <p>Every tick it asks the energy net for that tile's numbers and folds them into a running
 * average, a minimum and a maximum, exactly as 1.12.2 did. The reader has no slots of its own;
 * the five numbers it shows travel as screen handler properties, so the vanilla sync does the
 * work IC2's own {@code sendContainerFields} used to.
 *
 * <p>A property is an {@code int} and three of these numbers are not, so each one travels as the
 * bits of a {@code float} - the reader shows three significant digits, which a float carries.
 */
public class MeterScreenHandler extends ScreenHandler {

    /** What the reader can show, {@code ContainerMeter.Mode} in 1.12.2. */
    public enum Mode {
        ENERGY_IN,
        ENERGY_OUT,
        ENERGY_GAIN,
        VOLTAGE;

        public static final Mode[] VALUES = values();

        /** The line under the mode buttons. */
        public String getTranslationKey() {
            return "itemToolMEter.mode." + switch (this) {
                case ENERGY_IN -> "EnergyIn";
                case ENERGY_OUT -> "EnergyOut";
                case ENERGY_GAIN -> "EnergyGain";
                case VOLTAGE -> "Voltage";
            };
        }
    }

    public static final int PROPERTY_COUNT = 5;
    private static final int PROPERTY_AVG = 0;
    private static final int PROPERTY_MIN = 1;
    private static final int PROPERTY_MAX = 2;
    private static final int PROPERTY_COUNT_INDEX = 3;
    private static final int PROPERTY_MODE = 4;

    /** The button ids the screen sends; the first four pick a mode, the last one resets. */
    public static final int BUTTON_RESET = Mode.VALUES.length;

    private final PropertyDelegate properties;
    private final PlayerEntity player;

    /** Null on the client: it only ever reads the properties. */
    private IEnergyTile target;

    private Mode mode = Mode.ENERGY_IN;
    private double resultAvg;
    private double resultMin;
    private double resultMax;
    private int resultCount;

    /** Client side. */
    public MeterScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new ArrayPropertyDelegate(PROPERTY_COUNT), null);
    }

    public MeterScreenHandler(int syncId, PlayerInventory playerInventory, PropertyDelegate properties,
                              IEnergyTile target) {
        super(IC2ScreenHandlers.METER, syncId);
        this.properties = properties;
        this.player = playerInventory.player;
        this.target = target;

        addPlayerSlots(playerInventory);
        addProperties(properties);
    }

    /** The 1.12.2 offsets for a screen 218 tall, which is what its background is drawn for. */
    private void addPlayerSlots(PlayerInventory playerInventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, 136 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, 8 + column * 18, 194));
        }
    }

    /** The tile the reader was pointed at; set once, right after the screen opens. */
    public void setTarget(IEnergyTile target) {
        this.target = target;
    }

    @Override
    public void sendContentUpdates() {
        if (target != null) {
            sample();
        }
        super.sendContentUpdates();
    }

    /** One tick of measurement, the 1.12.2 {@code detectAndSendChanges} body. */
    private void sample() {
        NodeStats stats = EnergyNet.instance == null ? null : EnergyNet.instance.getNodeStats(target);
        if (stats == null) {
            // the tile is gone from the net, so there is nothing left to watch
            if (player instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer) {
                serverPlayer.closeHandledScreen();
            }
            target = null;
            return;
        }

        double result = switch (mode) {
            case ENERGY_IN -> stats.getEnergyIn();
            case ENERGY_OUT -> stats.getEnergyOut();
            case ENERGY_GAIN -> stats.getEnergyIn() - stats.getEnergyOut();
            case VOLTAGE -> stats.getVoltage();
        };

        if (resultCount == 0) {
            resultAvg = resultMin = resultMax = result;
        } else {
            resultMin = Math.min(resultMin, result);
            resultMax = Math.max(resultMax, result);
            resultAvg = (resultAvg * resultCount + result) / (resultCount + 1);
        }
        resultCount++;

        properties.set(PROPERTY_AVG, Float.floatToIntBits((float) resultAvg));
        properties.set(PROPERTY_MIN, Float.floatToIntBits((float) resultMin));
        properties.set(PROPERTY_MAX, Float.floatToIntBits((float) resultMax));
        properties.set(PROPERTY_COUNT_INDEX, resultCount);
        properties.set(PROPERTY_MODE, mode.ordinal());
    }

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        if (id == BUTTON_RESET) {
            reset();
            return true;
        }
        if (id >= 0 && id < Mode.VALUES.length) {
            mode = Mode.VALUES[id];
            reset();
            return true;
        }
        return false;
    }

    /** Throws the readings away and starts measuring again. */
    public void reset() {
        resultCount = 0;
        resultAvg = resultMin = resultMax = 0.0;
        properties.set(PROPERTY_COUNT_INDEX, 0);
    }

    // ---- what the screen reads ----

    public double getResultAvg() {
        return Float.intBitsToFloat(properties.get(PROPERTY_AVG));
    }

    public double getResultMin() {
        return Float.intBitsToFloat(properties.get(PROPERTY_MIN));
    }

    public double getResultMax() {
        return Float.intBitsToFloat(properties.get(PROPERTY_MAX));
    }

    /** Ticks measured; the screen shows seconds. */
    public int getResultCount() {
        return properties.get(PROPERTY_COUNT_INDEX);
    }

    public Mode getMode() {
        int ordinal = properties.get(PROPERTY_MODE);
        return ordinal >= 0 && ordinal < Mode.VALUES.length ? Mode.VALUES[ordinal] : Mode.ENERGY_IN;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return player == this.player;
    }

    /** No slots of its own, so a shift-click has nowhere to go. */
    @Override
    public ItemStack quickMove(PlayerEntity player, int slotIndex) {
        return ItemStack.EMPTY;
    }
}
