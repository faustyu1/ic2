package ic2.core.block.heatgenerator.tileentity;

import ic2.core.block.TileEntityHeatSourceInventory;
import ic2.core.block.invslot.InvSlotConsumable;
import ic2.core.block.invslot.InvSlotConsumableItemStack;
import ic2.core.init.IC2Blocks;
import ic2.core.init.IC2ItemVariants;
import ic2.core.init.MainConfig;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;

/**
 * Fabric port of {@code TileEntityRTHeatGenerator}: the radioisotope heat generator. Its six slots
 * take one RTG pellet each; n pellets give {@code 2^(n-1) * 2} HU a tick (times {@code balance/
 * energy/heatgenerator/radioisotope}) for ever, the pellets are never used up.
 */
public class TileEntityRTHeatGenerator extends TileEntityHeatSourceInventory {
    public final InvSlotConsumable fuelSlot;

    public TileEntityRTHeatGenerator(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type == null ? IC2Blocks.RT_HEAT_BE : type, pos, state);
        Item pellet = IC2ItemVariants.get("nuclear", "rtg_pellet");
        this.fuelSlot = new InvSlotConsumableItemStack(this, "fuelSlot", 6, new ItemStack(pellet == null ? Items.AIR : pellet));
        this.fuelSlot.setStackSizeLimit(1);
    }

    public TileEntityRTHeatGenerator(BlockPos pos, BlockState state) {
        this(null, pos, state);
    }

    public static float outputMultiplier() {
        return 2.0F * (float) MainConfig.get().getDouble("balance/energy/heatgenerator/radioisotope", 1.0);
    }

    @Override
    public void updateEntityServer() {
        super.updateEntityServer();
        setActive(heatBuffer > 0);
    }

    @Override
    protected int fillHeatBuffer(int maxAmount) {
        return Math.min(maxAmount, getMaxHeatEmittedPerTick());
    }

    @Override
    public int getMaxHeatEmittedPerTick() {
        int counter = 0;
        for (int i = 0; i < fuelSlot.size(); i++) {
            if (!fuelSlot.isEmpty(i)) {
                counter++;
            }
        }
        return counter == 0 ? 0 : (int) (Math.pow(2.0, counter - 1) * outputMultiplier());
    }
}
