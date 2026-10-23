package ic2.core.block.machine.tileentity;

import ic2.api.upgrade.IUpgradableBlock;
import ic2.api.upgrade.UpgradableProperty;
import ic2.core.block.invslot.InvSlot;
import ic2.core.block.invslot.InvSlotDischarge;
import ic2.core.block.invslot.InvSlotUpgrade;
import ic2.core.crop.TileEntityCrop;
import ic2.core.init.IC2Blocks;
import ic2.core.ref.TeBlock;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

/**
 * Fabric port of TileEntityCropHarvester - harvests the ripe crops around it.
 *
 * <p>It walks the same 9x3x9 area as the Crop-Matron, one block every ten ticks, and harvests a
 * crop that has reached its best or its last size. The harvest goes into its fifteen slots; what
 * does not fit drops out of the machine. A visit costs 1 EU, every item harvested 20.
 */
public class TileEntityCropHarvester extends TileEntityElectricMachine implements IUpgradableBlock {
    public final InvSlot contentSlot;
    public final InvSlotUpgrade upgradeSlot;
    public final InvSlotDischarge dischargeSlot;

    public int scanX = -4;
    public int scanY = -1;
    public int scanZ = -4;

    public TileEntityCropHarvester(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type == null ? IC2Blocks.CROP_HARVESTER_BE : type, pos, state, TeBlock.crop_harvester, 10000, 1);
        this.contentSlot = new InvSlot(this, "content", InvSlot.Access.IO, 15);
        this.upgradeSlot = new InvSlotUpgrade(this, "upgrade", 4);
        this.dischargeSlot = new InvSlotDischarge(this, "discharge", 1);
    }

    public TileEntityCropHarvester(BlockPos pos, BlockState state) {
        this(IC2Blocks.CROP_HARVESTER_BE, pos, state);
    }

    public TileEntityCropHarvester() {
        this(IC2Blocks.CROP_HARVESTER_BE, BlockPos.ORIGIN, null);
    }

    @Override
    public void tick() {
        if (world == null || world.isClient) {
            return;
        }

        drainDischargeSlot(dischargeSlot);
        upgradeSlot.tickNoMark();

        if (world.getTime() % 10L == 0L && getEnergy() >= 21.0) {
            scan();
        }
    }

    public void scan() {
        scanX++;
        if (scanX > 4) {
            scanX = -4;
            scanZ++;
            if (scanZ > 4) {
                scanZ = -4;
                scanY++;
                if (scanY > 1) {
                    scanY = -1;
                }
            }
        }

        useEnergy(1.0);

        if (!(world.getBlockEntity(pos.add(scanX, scanY, scanZ)) instanceof TileEntityCrop crop) || isInvFull() || crop.getCrop() == null) {
            return;
        }

        List<ItemStack> drops = null;
        if (crop.getCurrentSize() == crop.getCrop().getOptimalHarvestSize(crop)
                || crop.getCurrentSize() == crop.getCrop().getMaxSize()) {
            drops = crop.performHarvest();
        }

        if (drops == null) {
            return;
        }

        for (ItemStack drop : drops) {
            ItemStack rest = store(drop);
            if (!rest.isEmpty()) {
                Block.dropStack(world, pos, rest);
            }
            useEnergy(20.0);
        }
        markDirty();
    }

    /**
     * Puts a stack into the content slots, topping up matching stacks first.
     *
     * @return what did not fit
     */
    private ItemStack store(ItemStack stack) {
        ItemStack rest = stack.copy();

        for (int i = 0; i < contentSlot.size() && !rest.isEmpty(); i++) {
            ItemStack present = contentSlot.get(i);
            if (!present.isEmpty() && ItemStack.areItemsAndComponentsEqual(present, rest)) {
                int limit = Math.min(present.getMaxCount(), contentSlot.getStackSizeLimit());
                int moved = Math.min(rest.getCount(), limit - present.getCount());
                if (moved > 0) {
                    present.increment(moved);
                    rest.decrement(moved);
                    contentSlot.put(i, present);
                }
            }
        }

        for (int i = 0; i < contentSlot.size() && !rest.isEmpty(); i++) {
            if (contentSlot.isEmpty(i)) {
                int moved = Math.min(rest.getCount(), Math.min(rest.getMaxCount(), contentSlot.getStackSizeLimit()));
                contentSlot.put(i, rest.split(moved));
            }
        }

        return rest;
    }

    private boolean isInvFull() {
        for (int i = 0; i < contentSlot.size(); i++) {
            ItemStack stack = contentSlot.get(i);
            if (stack.isEmpty() || stack.getCount() < Math.min(stack.getMaxCount(), contentSlot.getStackSizeLimit())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Set<UpgradableProperty> getUpgradableProperties() {
        return EnumSet.of(UpgradableProperty.Transformer, UpgradableProperty.EnergyStorage, UpgradableProperty.ItemProducing);
    }
}
