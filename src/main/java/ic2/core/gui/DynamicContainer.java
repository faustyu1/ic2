package ic2.core.gui;

import ic2.core.block.TileEntityInventory;
import ic2.core.block.comp.Fluids;
import ic2.core.block.invslot.InvSlot;
import ic2.core.gui.dynamic.GuiDefinition;
import ic2.core.gui.dynamic.GuiElementNode;
import ic2.core.gui.dynamic.SlotCraftResult;
import ic2.core.gui.dynamic.SlotGridLayout;
import ic2.core.gui.dynamic.SlotHologram;
import ic2.core.gui.dynamic.SlotIC2;
import ic2.core.gui.dynamic.SlotStyle;
import ic2.core.init.IC2ScreenHandlers;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

/**
 * The screen handler every guidef-driven machine GUI uses.
 *
 * <p>Slots are built from the parsed {@link GuiDefinition}: a {@code <slot>} or {@code <slotgrid>}
 * element names an {@link InvSlot} group of the machine, so the layout in the XML and the machine's
 * inventory model stay the single source of truth on both sides. The client builds the same handler
 * from the block position it receives with the open packet, which is why no slot data has to be
 * sent.
 */
public class DynamicContainer extends ScreenHandler {
    /** Vanilla slot pitch; the guidef coordinates are in the same units. */
    public static final int SLOT_SIZE = 18;

    /** Gauge values travel as permille so they fit the vanilla integer property sync. */
    private static final int VALUE_SCALE = 1000;

    private final TileEntityInventory tile;
    private final GuiDefinition definition;
    /** Number of slots belonging to the machine, i.e. everything before the player inventory. */
    private int machineSlots;

    /** Names of the gauge values this GUI shows, in guidef order so both sides agree. */
    private final List<String> valueNames = new ArrayList<>();
    /** Conditions of the {@code <only>} elements, synced as 0 or 1 after the gauge values. */
    private final List<String> conditionNames = new ArrayList<>();
    /** Events of the {@code <button>} elements; their guidef order is the button id both sides use. */
    private final List<String> eventNames = new ArrayList<>();
    /** Tanks the GUI shows; each one syncs its colour, content and capacity after the conditions. */
    private final List<String> tankNames = new ArrayList<>();
    /**
     * Number of ints one tank occupies in {@link #values}.
     *
     * <p>Vanilla sends a screen handler property as a short, so the three numbers a tank syncs -
     * which fluid, how much, how much fits - each take two slots: a fluid id or the content of an
     * iridium tank does not fit in sixteen bits.
     */
    private static final int TANK_VALUES = 6;

    /**
     * Number of ints one gauge occupies: the ratio it is drawn with, plus the absolute value and
     * the capacity behind it, which the tooltip shows and which need two slots each.
     */
    private static final int GAUGE_VALUES = 5;
    private final int[] values;

    public DynamicContainer(int syncId, PlayerInventory playerInventory, BlockPos pos) {
        this(syncId, playerInventory, resolve(playerInventory, pos));
    }

    /** End of the player's slots; the display slots of {@code <itemimage>} elements follow. */
    private int playerSlotsEnd;
    private final java.util.List<String> itemImageNames = new java.util.ArrayList<>();
    private net.minecraft.inventory.SimpleInventory itemImages;

    public DynamicContainer(int syncId, PlayerInventory playerInventory, TileEntityInventory tile) {
        super(IC2ScreenHandlers.DYNAMIC, syncId);
        this.tile = tile;
        this.definition = tile == null ? null : GuiParser.parse(tile.getGuiName(playerInventory.player));

        if (definition != null) {
            addMachineSlots(definition);
        }
        this.machineSlots = slots.size();

        addPlayerSlots(playerInventory, definition);
        this.playerSlotsEnd = slots.size();
        addHologramAndResultSlots(definition);
        addItemImages(definition);

        collectNames(definition);
        this.values = new int[valueNames.size() * GAUGE_VALUES
                + conditionNames.size()
                + tankNames.size() * TANK_VALUES];
        addProperties(new ValueDelegate());
    }

    /**
     * Collects what has to travel to the client: the gauge values, the conditions the
     * {@code <only>} elements test and the button events. An {@code <energygauge>} always shows the
     * energy buffer; a {@code <gauge>} shows the value it names.
     */
    private void collectNames(GuiDefinition definition) {
        if (definition == null) {
            return;
        }
        for (GuiElementNode element : definition.getElements()) {
            switch (element.name) {
                case "energygauge", "energyGauge" -> valueNames.add("energy");
                case "gauge" -> {
                    valueNames.add(element.get("name", ""));
                    // active="true": the machine says when the gauge's background is lit
                    String activeCondition = "active:" + element.get("name", "");
                    if (element.has("active") && !conditionNames.contains(activeCondition)) {
                        conditionNames.add(activeCondition);
                    }
                }
                case "only" -> {
                    String condition = element.get("if", "");
                    if (!condition.isEmpty() && !conditionNames.contains(condition)) {
                        conditionNames.add(condition);
                    }
                }
                case "button" -> eventNames.add(element.get("event", ""));
                // %amount:name% in a text or a tooltip needs the value synced like a gauge's
                case "text" -> addNumberNames(element.getText().isEmpty() ? element.get("text", "") : element.getText());
                case "tooltip" -> addNumberNames(element.get("text", ""));
                case "fluidtank", "fluidslot" -> {
                    String tank = element.get("name", "");
                    if (!tank.isEmpty() && !tankNames.contains(tank)) {
                        tankNames.add(tank);
                    }
                }
                default -> { }
            }
        }
    }

    private void addNumberNames(String text) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("%(?:amount|capacity):([A-Za-z0-9_]+)%").matcher(text);
        while (matcher.find()) {
            if (!valueNames.contains(matcher.group(1))) {
                valueNames.add(matcher.group(1));
            }
        }
    }

    /** Backing store of the synced gauge values; the server refreshes it before each sync. */
    private final class ValueDelegate implements PropertyDelegate {
        @Override
        public int get(int index) {
            return values[index];
        }

        @Override
        public void set(int index, int value) {
            values[index] = value;
        }

        @Override
        public int size() {
            return values.length;
        }
    }

    /** Reads the gauge values from the machine before the vanilla sync compares them. */
    @Override
    public void sendContentUpdates() {
        for (Slot slot : slots) {
            if (slot instanceof SlotCraftResult result && tile != null) {
                result.refresh();
            }
        }
        if (tile != null && itemImages != null) {
            for (int i = 0; i < itemImageNames.size(); i++) {
                ItemStack shown = tile.getGuiItem(itemImageNames.get(i));
                itemImages.setStack(i, shown == null ? ItemStack.EMPTY : shown.copy());
            }
        }
        if (tile != null) {
            for (int i = 0; i < valueNames.size(); i++) {
                String name = valueNames.get(i);
                int base = gaugeOffset(i);

                values[base] = (int) Math.round(clamp(tile.getGuiValue(name)) * VALUE_SCALE);
                putWide(base + 1, (int) Math.round(tile.getGuiAmount(name)));
                putWide(base + 3, (int) Math.round(tile.getGuiCapacity(name)));
            }
            for (int i = 0; i < conditionNames.size(); i++) {
                values[conditionOffset(i)] = tile.getGuiCondition(conditionNames.get(i)) ? 1 : 0;
            }
            for (int i = 0; i < tankNames.size(); i++) {
                Fluids.InternalFluidTank tank = tile.getFluidTank(tankNames.get(i));
                FluidStack content = tank == null ? null : tank.getFluid();
                int base = tankOffset(i);

                putWide(base, content == null ? 0 : FluidRegistry.getFluidId(content.getFluid()));
                putWide(base + 2, content == null ? 0 : content.amount);
                putWide(base + 4, tank == null ? 0 : tank.getCapacity());
            }
        }
        super.sendContentUpdates();
    }

    private int gaugeOffset(int index) {
        return index * GAUGE_VALUES;
    }

    private int conditionOffset(int index) {
        return valueNames.size() * GAUGE_VALUES + index;
    }

    private int tankOffset(int index) {
        return valueNames.size() * GAUGE_VALUES + conditionNames.size() + index * TANK_VALUES;
    }

    /** Splits a number over two property slots, because one only carries sixteen bits. */
    private void putWide(int index, int value) {
        values[index] = value & 0xFFFF;
        values[index + 1] = (value >>> 16) & 0xFFFF;
    }

    private int readWide(int index) {
        return (values[index] & 0xFFFF) | ((values[index + 1] & 0xFFFF) << 16);
    }

    /**
     * What a tank holds: which fluid, how much of it and how much fits.
     *
     * <p>The fluid travels as the id {@link FluidRegistry#getFluidId} gives it, which both sides
     * derive from the fluid's name rather than from the order things were registered in. An empty
     * tank has an amount of zero.
     *
     * @return fluid id, amount and capacity, or null when the GUI does not show that tank
     */
    public int[] getTank(String name) {
        int index = tankNames.indexOf(name);
        if (index < 0) {
            return null;
        }

        int base = tankOffset(index);
        return new int[] {readWide(base), readWide(base + 2), readWide(base + 4)};
    }

    /**
     * State of a condition an {@code <only if="...">} element tests, on either side.
     *
     * <p>Unknown conditions are false, which hides the element - the same thing the original did
     * for a machine that does not provide the condition.
     */
    public boolean getGuiCondition(String name) {
        int index = conditionNames.indexOf(name);
        return index >= 0 && values[conditionOffset(index)] != 0;
    }

    /** @return true when every {@code <only>} wrapping the element is satisfied */
    public boolean isEnabled(GuiElementNode element) {
        for (GuiElementNode condition : element.getConditions()) {
            boolean state = getGuiCondition(condition.get("if", ""));
            if (condition.has("inverted")) {
                state = !state;
            }
            if (!state) {
                return false;
            }
        }
        return true;
    }

    /**
     * Handles a click on a {@code <button>}. The id is the button's position in the guidef, which
     * both sides read from the same file, so the vanilla button packet is all the networking a
     * button needs.
     */
    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        if (tile == null || id < 0 || id >= eventNames.size() || !canUse(player)) {
            return false;
        }

        tile.onGuiEvent(player, eventNames.get(id));
        return true;
    }

    /** Event name of the button with the given id, or null when there is no such button. */
    public String getEventName(int id) {
        return id < 0 || id >= eventNames.size() ? null : eventNames.get(id);
    }

    /** @return the gauge value as a 0..1 ratio, on either side */
    public double getGuiValue(String name) {
        int index = valueNames.indexOf(name);
        return index < 0 ? 0.0 : (double) values[gaugeOffset(index)] / VALUE_SCALE;
    }

    /**
     * The numbers behind a gauge - how much there is and how much fits - which the gauge's
     * tooltip shows.
     *
     * @return amount and capacity, or null when the GUI does not show that gauge
     */
    public int[] getGuiNumbers(String name) {
        int index = valueNames.indexOf(name);
        if (index < 0) {
            return null;
        }

        int base = gaugeOffset(index);
        return new int[] {readWide(base + 1), readWide(base + 3)};
    }

    private static double clamp(double value) {
        return value < 0.0 ? 0.0 : Math.min(value, 1.0);
    }

    private static TileEntityInventory resolve(PlayerInventory playerInventory, BlockPos pos) {
        if (pos == null) {
            return null;
        }
        BlockEntity be = playerInventory.player.getWorld().getBlockEntity(pos);
        return be instanceof TileEntityInventory inventory ? inventory : null;
    }

    /**
     * An {@code <itemimage name="...">} shows an item the machine names, such as the pattern a
     * replicator is set to. It is a slot of a small inventory of the screen, not of the machine:
     * vanilla syncs and draws it like any slot, but nothing can be put in or taken out.
     */
    private void addItemImages(GuiDefinition definition) {
        if (definition == null) {
            return;
        }
        java.util.List<GuiElementNode> images = definition.getElements("itemimage");
        if (images.isEmpty()) {
            return;
        }
        itemImages = new net.minecraft.inventory.SimpleInventory(images.size());
        for (int i = 0; i < images.size(); i++) {
            GuiElementNode element = images.get(i);
            itemImageNames.add(element.get("name", ""));
            addSlot(new Slot(itemImages, i, element.getX(), element.getY()) {
                @Override
                public boolean canInsert(ItemStack stack) {
                    return false;
                }

                @Override
                public boolean canTakeItems(PlayerEntity player) {
                    return false;
                }
            });
        }
    }

    private void addMachineSlots(GuiDefinition definition) {
        for (GuiElementNode element : definition.getElements()) {
            switch (element.name) {
                case "slot" -> addNamedSlot(element);
                case "slothologram" -> {
                    if (tile.getHologramSlots(element.get("name", "")) == null) {
                        addNamedSlot(element);
                    }
                }
                case "slotgrid" -> addSlotGrid(element);
                default -> { /* visual elements are handled by the screen */ }
            }
        }
    }

    private void addNamedSlot(GuiElementNode element) {
        InvSlot invSlot = tile.getInvSlot(element.get("name", ""));
        if (invSlot == null) {
            return;
        }

        int index = element.getInt("index", 0);
        if (index >= invSlot.size()) {
            return;
        }
        SlotStyle style = SlotStyle.of(element);
        SlotIC2 slot = new SlotIC2(tile, indexOf(invSlot, index),
                element.getX() + style.insetX(), element.getY() + style.insetY());
        addSlot(element.getBool("readonly", false) ? slot.readOnly() : slot);
    }

    private void addSlotGrid(GuiElementNode element) {
        if (element.getBool("hologram", false)) {
            return; // added after the player's slots
        }
        InvSlot invSlot = tile.getInvSlot(element.get("name", ""));
        if (invSlot == null) {
            return;
        }

        SlotGridLayout layout = SlotGridLayout.of(element, invSlot.size());
        int x = element.getX() + layout.style().insetX();
        int y = element.getY() + layout.style().insetY();

        for (int cell = 0; cell < layout.count(invSlot.size()); cell++) {
            addSlot(new SlotIC2(tile, indexOf(invSlot, layout.slotIndex(cell)),
                    x + layout.columnOf(cell) * layout.stepX(),
                    y + layout.rowOf(cell) * layout.stepY()));
        }
    }

    /** Size of the slot group an element names, or 0 when the machine has no such group. */
    public int getGroupSize(GuiElementNode element) {
        if (tile != null && element.getBool("hologram", false)) {
            ItemStack[] stacks = tile.getHologramSlots(element.get("name", ""));
            return stacks == null ? 0 : stacks.length;
        }
        InvSlot invSlot = tile == null ? null : tile.getInvSlot(element.get("name", ""));
        return invSlot == null ? 0 : invSlot.size();
    }

    /** Flat inventory index of an entry inside a named slot group. */
    private int indexOf(InvSlot invSlot, int index) {
        int offset = 0;
        for (InvSlot other : tile.getInvSlots()) {
            if (other == invSlot) {
                return offset + index;
            }
            offset += other.size();
        }
        return index;
    }

    /**
     * Adds the player's inventory where the guidef asks for it, falling back to the vanilla
     * position of a 176x166 window when the file does not say.
     */
    private void addPlayerSlots(PlayerInventory playerInventory, GuiDefinition definition) {
        int x = 8;
        int y = 84;

        if (definition != null) {
            for (GuiElementNode element : definition.getElements("playerInventory")) {
                x = element.getX() + 1;
                y = element.getY() + 1;
                break;
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, 9 + row * 9 + col,
                        x + col * SLOT_SIZE, y + row * SLOT_SIZE));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, x + col * SLOT_SIZE, y + 3 * SLOT_SIZE + 4));
        }
    }

    /**
     * Adds the ghost slots and the crafting result slots. They follow the player's inventory, so
     * neither shift-clicking into the machine nor out of it ever lands in them.
     */
    private void addHologramAndResultSlots(GuiDefinition definition) {
        if (definition == null || tile == null) {
            return;
        }
        List<GuiElementNode> results = new ArrayList<>();
        for (GuiElementNode element : definition.getElements()) {
            String name = element.get("name", "");
            ItemStack[] stacks = tile.getHologramSlots(name);
            if (element.name.equals("slothologram") && stacks != null) {
                int index = element.getInt("index", 0);
                SlotStyle style = SlotStyle.of(element);
                if (index < stacks.length) {
                    addSlot(new SlotHologram(tile, name, stacks, index,
                            element.getX() + style.insetX(), element.getY() + style.insetY(),
                            element.getInt("stack", 64)));
                }
            } else if (element.name.equals("slotgrid") && element.getBool("hologram", false) && stacks != null) {
                SlotGridLayout layout = SlotGridLayout.of(element, stacks.length);
                int x = element.getX() + layout.style().insetX();
                int y = element.getY() + layout.style().insetY();
                for (int cell = 0; cell < layout.count(stacks.length); cell++) {
                    addSlot(new SlotHologram(tile, name, stacks, layout.slotIndex(cell),
                            x + layout.columnOf(cell) * layout.stepX(),
                            y + layout.rowOf(cell) * layout.stepY(),
                            element.getInt("stack", 64)));
                }
            } else if (element.name.equals("craftresult")) {
                results.add(element);
            }
        }

        if (!results.isEmpty()) {
            net.minecraft.inventory.SimpleInventory inventory = new net.minecraft.inventory.SimpleInventory(results.size());
            for (int i = 0; i < results.size(); i++) {
                GuiElementNode element = results.get(i);
                SlotStyle style = SlotStyle.of(element);
                addSlot(new SlotCraftResult(tile, element.get("name", ""), inventory, i,
                        element.getX() + style.insetX(), element.getY() + style.insetY()));
            }
        }
    }

    /** Ghost slots take clicks their own way and never hand an item out. */
    @Override
    public void onSlotClick(int slotIndex, int button, net.minecraft.screen.slot.SlotActionType actionType, PlayerEntity player) {
        if (slotIndex >= 0 && slotIndex < slots.size() && slots.get(slotIndex) instanceof SlotHologram hologram) {
            hologram.click(button, actionType, getCursorStack(), player);
            return;
        }
        super.onSlotClick(slotIndex, button, actionType, player);
    }

    /** Collecting a stack by double click leaves crafting results alone, as the vanilla crafting table does. */
    @Override
    public boolean canInsertIntoSlot(ItemStack stack, Slot slot) {
        return !(slot instanceof SlotCraftResult) && super.canInsertIntoSlot(stack, slot);
    }

    /**
     * Vanilla merges into any slot holding the same item before it asks the slot; IC2 only ever
     * moved items into slots that accept them, which keeps a shift-click out of output and
     * read-only slots.
     */
    @Override
    protected boolean insertItem(ItemStack stack, int startIndex, int endIndex, boolean fromLast) {
        boolean moved = false;
        for (int pass = 0; pass < 2 && !stack.isEmpty(); pass++) {
            int i = fromLast ? endIndex - 1 : startIndex;
            while (!stack.isEmpty() && (fromLast ? i >= startIndex : i < endIndex)) {
                Slot slot = slots.get(i);
                ItemStack existing = slot.getStack();
                if (slot.canInsert(stack)) {
                    int limit = slot.getMaxItemCount(stack);
                    if (pass == 0 && !existing.isEmpty() && ItemStack.areItemsAndComponentsEqual(stack, existing)
                            && existing.getCount() < limit) {
                        int add = Math.min(stack.getCount(), limit - existing.getCount());
                        existing.increment(add);
                        stack.decrement(add);
                        slot.markDirty();
                        moved = true;
                    } else if (pass == 1 && existing.isEmpty()) {
                        slot.setStack(stack.split(Math.min(stack.getCount(), limit)));
                        slot.markDirty();
                        moved = true;
                    }
                }
                i += fromLast ? -1 : 1;
            }
        }
        return moved;
    }

    /** Whether the player's slots have room for the whole stack, which a crafting result needs. */
    private boolean playerSlotsFit(ItemStack stack) {
        int room = 0;
        for (int i = machineSlots; i < playerSlotsEnd; i++) {
            ItemStack existing = slots.get(i).getStack();
            if (existing.isEmpty()) {
                room += stack.getMaxCount();
            } else if (ItemStack.areItemsAndComponentsEqual(existing, stack)) {
                room += Math.max(0, stack.getMaxCount() - existing.getCount());
            }
            if (room >= stack.getCount()) {
                return true;
            }
        }
        return false;
    }

    public TileEntityInventory getTile() {
        return tile;
    }

    public GuiDefinition getDefinition() {
        return definition;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return tile != null && tile.canPlayerUse(player);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int index) {
        if (index >= 0 && index < slots.size() && slots.get(index) instanceof SlotCraftResult result) {
            // vanilla calls this again while the same item is offered, which crafts a whole stack
            ItemStack stack = result.getStack();
            if (stack.isEmpty() || !playerSlotsFit(stack)) {
                return ItemStack.EMPTY;
            }
            ItemStack original = stack.copy();
            ItemStack moving = result.takeStack(stack.getCount());
            insertItem(moving, machineSlots, playerSlotsEnd, true);
            result.onTakeItem(player, original);
            return original;
        }
        if (index >= playerSlotsEnd) {
            return ItemStack.EMPTY;
        }
        Slot slot = slots.get(index);
        if (!slot.hasStack()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getStack();
        ItemStack original = stack.copy();

        if (index < machineSlots) {
            // machine -> player
            if (!insertItem(stack, machineSlots, playerSlotsEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else if (machineSlots > 0) {
            // player -> the first machine slot that accepts it
            if (!insertItem(stack, 0, machineSlots, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setStack(ItemStack.EMPTY);
        } else {
            slot.markDirty();
        }

        return original;
    }
}
