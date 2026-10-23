package ic2.core.item.handheld;

import ic2.api.crops.CropCard;
import ic2.api.crops.Crops;
import ic2.api.crops.ICropSeed;
import ic2.api.item.ElectricItem;
import ic2.api.item.IElectricItem;
import ic2.core.init.IC2ScreenHandlers;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

/**
 * Fabric port of {@code ic2.core.item.tool.ContainerCropnalyzer} together with the ticking half of
 * {@code HandHeldCropnalyzer}: three slots - a seed bag to analyse, the analysed bag, and a
 * battery to top the cropnalyzer up from.
 *
 * <p>1.12.2 ran the analysis from a world tick callback registered while the GUI was open. Here it
 * runs in {@link #sendContentUpdates()}, which the server calls once a tick for exactly as long as
 * the screen is open, so the lifetime is the same without the callback bookkeeping.
 *
 * <p>Not ported: feeding a single-use battery in as fuel. That read the stack's energy value
 * through {@code Info.itemInfo}, which the port does not have, so only rechargeable batteries
 * work - they discharge into the cropnalyzer as before.
 */
public class CropnalyzerScreenHandler extends ScreenHandler {

    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT = 1;
    public static final int SLOT_BATTERY = 2;
    public static final int SIZE = 3;

    private final PlayerEntity player;
    private final Inventory inventory;
    /** Null on the client: the analysis and the charging are the server's business. */
    private final HandHeldInventory handHeld;

    /** Client side: the three stacks arrive through the usual slot sync. */
    public CropnalyzerScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(SIZE), null);
    }

    public CropnalyzerScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory,
                                    HandHeldInventory handHeld) {
        super(IC2ScreenHandlers.CROPNALYZER, syncId);
        this.player = playerInventory.player;
        this.inventory = inventory;
        this.handHeld = handHeld;

        addSlot(new SeedSlot(inventory, SLOT_INPUT, 8, 7));
        addSlot(new Slot(inventory, SLOT_OUTPUT, 41, 7));
        addSlot(new BatterySlot(inventory, SLOT_BATTERY, 152, 7));
        addPlayerSlots(playerInventory);
    }

    /** The 1.12.2 offsets for a screen 223 tall, which is what its background is drawn for. */
    private void addPlayerSlots(PlayerInventory playerInventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, 141 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, 8 + column * 18, 199));
        }
    }

    /** The EU one more scan level costs, {@code energyForLevel} in 1.12.2. */
    public static int energyForLevel(int level) {
        return switch (level) {
            case 1 -> 90;
            case 2 -> 900;
            case 3 -> 9000;
            default -> 10;
        };
    }

    @Override
    public void sendContentUpdates() {
        if (handHeld != null) {
            chargeFromBattery();
            analyseSeed();
        }
        super.sendContentUpdates();
    }

    /** Moves EU out of a rechargeable battery in the third slot into the cropnalyzer itself. */
    private void chargeFromBattery() {
        ItemStack battery = inventory.getStack(SLOT_BATTERY);
        if (battery.isEmpty() || !(battery.getItem() instanceof IElectricItem)) return;

        ItemStack cropnalyzer = handHeld.containerStack();
        double need = ElectricItem.manager.charge(cropnalyzer, Double.POSITIVE_INFINITY, Integer.MAX_VALUE, true, true);
        if (need <= 0.0) return;

        double got = ElectricItem.manager.discharge(battery, need, Integer.MAX_VALUE, false, true, false);
        if (got > 0.0) {
            ElectricItem.manager.charge(cropnalyzer, got, 3, true, false);
            handHeld.save();
        }
    }

    /**
     * One step of analysis: a seed bag in the first slot moves to the second, one scan level
     * higher, as soon as the cropnalyzer can pay for that level.
     */
    private void analyseSeed() {
        ItemStack input = inventory.getStack(SLOT_INPUT);
        if (!inventory.getStack(SLOT_OUTPUT).isEmpty()) return;
        if (input.isEmpty() || !(input.getItem() instanceof ICropSeed seed)) return;

        int level = seed.getScannedFromStack(input);
        if (level < 4) {
            double needed = energyForLevel(level);
            double got = ElectricItem.manager.discharge(handHeld.containerStack(), needed, 2, true, false, false);
            if (got < needed) {
                return;
            }
            seed.incrementScannedFromStack(input);
        }

        inventory.setStack(SLOT_OUTPUT, input);
        inventory.setStack(SLOT_INPUT, ItemStack.EMPTY);
    }

    // ---- what the screen reads; all of it comes off the stack in the output slot ----

    /** The analysed stack, or an empty one while there is nothing to show. */
    public ItemStack getAnalysed() {
        return inventory.getStack(SLOT_OUTPUT);
    }

    /** How far the seed in the output slot has been analysed, or -1 when the slot is not a seed. */
    public int getScannedLevel() {
        ItemStack output = getAnalysed();
        return output.getItem() instanceof ICropSeed seed ? seed.getScannedFromStack(output) : -1;
    }

    public CropCard getCrop() {
        ItemStack output = getAnalysed();
        return output.isEmpty() || Crops.instance == null ? null : Crops.instance.getCropCard(output);
    }

    public int getSeedGrowth() {
        ItemStack output = getAnalysed();
        return output.getItem() instanceof ICropSeed seed ? seed.getGrowthFromStack(output) : 0;
    }

    public int getSeedGain() {
        ItemStack output = getAnalysed();
        return output.getItem() instanceof ICropSeed seed ? seed.getGainFromStack(output) : 0;
    }

    public int getSeedResistance() {
        ItemStack output = getAnalysed();
        return output.getItem() instanceof ICropSeed seed ? seed.getResistanceFromStack(output) : 0;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return player == this.player && inventory.canPlayerUse(player);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slotIndex) {
        Slot slot = slots.get(slotIndex);
        if (!slot.hasStack()) return ItemStack.EMPTY;

        ItemStack inSlot = slot.getStack();
        ItemStack original = inSlot.copy();

        if (slotIndex < SIZE) {
            if (!insertItem(inSlot, SIZE, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!insertItem(inSlot, 0, SIZE, false)) {
            return ItemStack.EMPTY;
        }

        if (inSlot.isEmpty()) {
            slot.setStack(ItemStack.EMPTY);
        } else {
            slot.markDirty();
        }

        return original;
    }

    /** Only a seed bag goes in for analysis. */
    private static final class SeedSlot extends Slot {
        private SeedSlot(Inventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean canInsert(ItemStack stack) {
            return stack.getItem() instanceof ICropSeed;
        }
    }

    /** Only something that holds EU goes in the battery slot. */
    private static final class BatterySlot extends Slot {
        private BatterySlot(Inventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean canInsert(ItemStack stack) {
            return stack.getItem() instanceof IElectricItem;
        }
    }
}
