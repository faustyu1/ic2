package ic2.core.block.machine;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.Direction;

/**
 * The priority list of the weighted distributors: up to five sides, highest first.
 *
 * <p>1.12.2 edited the list on the client in GuiWeightedDistributor, a 5x6 grid of sticky buttons,
 * and sent the whole list to the server. The port's guidef buttons only send which cell was
 * clicked, so the same toggling rules run here on the server instead.
 */
public final class WeightedPriority {
    public static final int ROWS = 5;

    private final List<Direction> priority = new ArrayList<>(ROWS);

    public List<Direction> get() {
        return priority;
    }

    /** The side shown in a column of the grid, as GuiWeightedDistributor ordered them (U, N, S, W, E, D). */
    public static Direction columnSide(int column) {
        return Direction.byId((column + 1) % 6);
    }

    public boolean isOn(int row, Direction side) {
        return row < priority.size() && priority.get(row) == side;
    }

    /**
     * A click on the cell of a side in a row: a side already in the list is taken out (the rows
     * below move up); unless that was the clicked cell, the side then goes into the clicked row,
     * or the first free one above it.
     */
    public void click(int row, Direction side) {
        int current = priority.indexOf(side);
        boolean switchingOff = false;
        if (current >= 0) {
            priority.remove(current);
            switchingOff = current == row;
        }
        if (!switchingOff) {
            int target = Math.min(row, priority.size());
            if (target == priority.size()) {
                priority.add(side);
            } else {
                priority.set(target, side);
            }
        }
    }

    /** Takes a side out, e.g. when the distributor is turned to face it. */
    public boolean remove(Direction side) {
        return priority.remove(side);
    }

    public void write(NbtCompound nbt) {
        if (!priority.isEmpty()) {
            int[] indexes = new int[priority.size()];
            for (int i = 0; i < indexes.length; i++) {
                indexes[i] = priority.get(i).getId();
            }
            nbt.putIntArray("priority", indexes);
        }
    }

    public void read(NbtCompound nbt) {
        priority.clear();
        for (int index : nbt.getIntArray("priority")) {
            priority.add(Direction.byId(index));
        }
    }

    /** Handles a {@code prio:<row>:<column>} button event; returns false for any other event. */
    public boolean onGuiEvent(String event, Direction facing) {
        if (!event.startsWith("prio:")) {
            return false;
        }
        String[] parts = event.split(":");
        try {
            int row = Integer.parseInt(parts[1]);
            Direction side = columnSide(Integer.parseInt(parts[2]));
            if (row >= 0 && row < ROWS && side != facing) {
                click(row, side);
            }
        } catch (RuntimeException e) {
            // a malformed event from the client changes nothing
        }
        return true;
    }

    /** {@code prio_<row>_<column>} is on when that side has that place, {@code side_<column>} when the side is free to use. */
    public Boolean getGuiCondition(String name, Direction facing) {
        if (name.startsWith("prio_")) {
            String[] parts = name.split("_");
            return isOn(Integer.parseInt(parts[1]), columnSide(Integer.parseInt(parts[2])));
        }
        if (name.startsWith("side_")) {
            return columnSide(Integer.parseInt(name.substring(5))) != facing;
        }
        return null;
    }
}
