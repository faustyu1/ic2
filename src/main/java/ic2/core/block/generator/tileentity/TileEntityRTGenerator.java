package ic2.core.block.generator.tileentity;

import ic2.core.block.invslot.InvSlotConsumable;
import ic2.core.block.invslot.InvSlotConsumableItemStack;
import ic2.core.init.IC2Blocks;
import ic2.core.init.IC2ItemVariants;
import ic2.core.init.MainConfig;
import ic2.core.ref.TeBlock;
import ic2.core.util.ConfigUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

/**
 * Fabric port of TileEntityRTGenerator - the radioisotope thermoelectric generator.
 *
 * <p>Six slots take one RTG pellet each; the pellets never run out. One pellet gives 1 EU/t and
 * every further pellet doubles it, up to 32 EU/t with all six, scaled by
 * {@code balance/energy/generator/radioisotope}. The buffer holds 20000 EU.
 */
public class TileEntityRTGenerator extends TileEntityBaseGenerator {
    /** Six pellet slots, one pellet each. */
    public final InvSlotConsumable fuelSlot;

    public TileEntityRTGenerator(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type == null ? IC2Blocks.RT_BE : type, pos, state, TeBlock.rt_generator,
                Math.round(16.0 * efficiency()), 1, 20000);
        this.fuelSlot = new InvSlotConsumableItemStack(this, "fuel", 6, pellet());
        this.fuelSlot.setStackSizeLimit(1);
    }

    public TileEntityRTGenerator(BlockPos pos, BlockState state) {
        this(IC2Blocks.RT_BE, pos, state);
    }

    private static double efficiency() {
        return ConfigUtil.getDouble(MainConfig.get(), "balance/energy/generator/radioisotope");
    }

    private static ItemStack pellet() {
        Item item = IC2ItemVariants.get("nuclear", "rtg_pellet");
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    @Override
    public boolean gainEnergy() {
        int pellets = 0;
        for (int i = 0; i < fuelSlot.size(); i++) {
            if (!fuelSlot.isEmpty(i)) {
                pellets++;
            }
        }

        if (pellets == 0) {
            return false;
        }

        // the 1.12.2 efficiency was a float
        energyStored = Math.min(maxStorage, energyStored + Math.pow(2.0, pellets - 1) * (float) efficiency());
        markDirty();
        return true;
    }

    @Override
    public boolean gainFuel() {
        return false;
    }

    @Override
    public boolean needsFuel() {
        return false;
    }

    @Override
    protected boolean delayActiveUpdate() {
        return true;
    }
}
