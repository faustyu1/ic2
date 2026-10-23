package ic2.core.block.machine.tileentity;

import ic2.api.item.ElectricItem;
import ic2.api.upgrade.IUpgradableBlock;
import ic2.api.upgrade.UpgradableProperty;
import ic2.core.block.invslot.InvSlot;
import ic2.core.block.invslot.InvSlotConsumable;
import ic2.core.block.invslot.InvSlotDischarge;
import ic2.core.block.invslot.InvSlotUpgrade;
import ic2.core.init.IC2Blocks;
import ic2.core.init.MainConfig;
import ic2.core.init.OreValues;
import ic2.core.item.tool.ItemScanner;
import ic2.core.ref.TeBlock;
import ic2.core.util.ConfigUtil;
import ic2.core.util.StackUtil;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.FluidBlock;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/**
 * Fabric port of TileEntityAdvMiner - the advanced miner.
 *
 * <p>With a charged OD scanner (radius 16) or OV scanner (radius 32) it works through the area
 * under it layer by layer, a row at a time, once a second looking at up to 5 blocks (5 more per
 * overclocker), 64 EU of the scanner's charge each. The first block it may take is mined for
 * 512 EU without any pipe: its drops - with silk touch if switched on - go into the inventories
 * around it or onto the ground. The 15 filter slots make a blacklist (default) or whitelist of
 * drops. Blocks with a block entity are only taken when they are ores; fluids never. A redstone
 * signal stops it. 4 000 000 EU, tier 3 (+ transformers), the item keeps its energy like a storage
 * block.
 */
public class TileEntityAdvMiner extends TileEntityElectricMachine implements IUpgradableBlock, InvSlotUpgrade.Listener {
    private static final int SCAN_ENERGY = 64;
    private static final int MINE_ENERGY = 512;

    private int maxBlockScanCount = 5;
    public final int workTick = 20;
    public boolean blacklist = true;
    public boolean silkTouch;
    private BlockPos mineTarget;
    private short ticker;
    public final InvSlotConsumable scannerSlot;
    public final InvSlotUpgrade upgradeSlot;
    public final InvSlot filterSlot;
    public final InvSlotDischarge dischargeSlot;

    public TileEntityAdvMiner(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type == null ? IC2Blocks.ADV_MINER_BE : type, pos, state, TeBlock.advanced_miner, 4000000,
                Math.min(2 + ConfigUtil.getInt(MainConfig.get(), "balance/minerDischargeTier"), 5));
        this.scannerSlot = new InvSlotConsumable(this, "scanner", InvSlot.Access.IO, 1, InvSlot.InvSide.BOTTOM) {
            @Override
            public boolean accepts(ItemStack stack) {
                return stack.getItem() instanceof ItemScanner;
            }
        };
        this.upgradeSlot = new InvSlotUpgrade(this, "upgrade", 4);
        this.filterSlot = new InvSlot(this, "list", InvSlot.Access.NONE, 15);
        this.dischargeSlot = new InvSlotDischarge(this, "discharge", tier);
    }

    public TileEntityAdvMiner(BlockPos pos, BlockState state) {
        this(null, pos, state);
    }

    @Override
    public void onUpgradesChanged() {
        setTier(upgradeSlot.getTier(defaultTier));
        maxBlockScanCount = 5 * (upgradeSlot.augmentation + 1);
    }

    @Override
    public void tick() {
        if (world == null || world.isClient) {
            return;
        }
        drainDischargeSlot(dischargeSlot);
        chargeTool();
        if (work()) {
            markDirty();
            setActive(true);
        } else {
            setActive(false);
        }
    }

    private int redstoneInput() {
        return upgradeSlot.getRedstoneInput(world.getReceivedRedstonePower(pos));
    }

    private boolean work() {
        if (getEnergy() < MINE_ENERGY || redstoneInput() > 0) {
            return false;
        }
        if (mineTarget != null && mineTarget.getY() < world.getBottomY()) {
            return false;
        }

        ItemStack scanner = scannerSlot.get();
        if (scanner.isEmpty() || ElectricItem.manager == null || !ElectricItem.manager.canUse(scanner, SCAN_ENERGY)) {
            return false;
        }
        if (++ticker != workTick) {
            return true;
        }
        ticker = 0;

        int range = isAdvanced(scanner) ? 32 : 16;
        if (mineTarget == null) {
            mineTarget = new BlockPos(pos.getX() - range - 1, pos.getY() - 1, pos.getZ() - range);
            if (mineTarget.getY() < world.getBottomY()) {
                return false;
            }
        }

        int blockScanCount = maxBlockScanCount;
        BlockPos.Mutable scanPos = mineTarget.mutableCopy();
        do {
            if (scanPos.getX() < pos.getX() + range) {
                scanPos.set(scanPos.getX() + 1, scanPos.getY(), scanPos.getZ());
            } else if (scanPos.getZ() < pos.getZ() + range) {
                scanPos.set(pos.getX() - range, scanPos.getY(), scanPos.getZ() + 1);
            } else {
                scanPos.set(pos.getX() - range, scanPos.getY() - 1, pos.getZ() - range);
                if (scanPos.getY() < world.getBottomY()) {
                    mineTarget = scanPos.toImmutable();
                    return true;
                }
            }

            ElectricItem.manager.discharge(scanner, SCAN_ENERGY, Integer.MAX_VALUE, true, false, false);
            BlockState state = world.getBlockState(scanPos);
            if (!state.isAir() && canMine(scanPos, state)) {
                mineTarget = scanPos.toImmutable();
                doMine(mineTarget, state);
                break;
            }
            mineTarget = scanPos.toImmutable();
        } while (--blockScanCount > 0 && ElectricItem.manager.canUse(scanner, SCAN_ENERGY));
        return true;
    }

    private static boolean isAdvanced(ItemStack scanner) {
        return Registries.ITEM.getId(scanner.getItem()).equals(Identifier.of("ic2", "advanced_scanner"));
    }

    private void chargeTool() {
        if (!scannerSlot.isEmpty() && ElectricItem.manager != null) {
            useEnergy(ElectricItem.manager.charge(scannerSlot.get(), getEnergy(), getSinkTier(), false, false));
        }
    }

    public void doMine(BlockPos target, BlockState state) {
        StackUtil.distributeDrops(this, getDrops(target, state));
        world.removeBlock(target, false);
        useEnergy(MINE_ENERGY);
    }

    /** What the block drops to a pickaxe, with silk touch when that is switched on. */
    private List<ItemStack> getDrops(BlockPos target, BlockState state) {
        if (!(world instanceof ServerWorld serverWorld)) {
            return List.of();
        }
        ItemStack tool = new ItemStack(Items.DIAMOND_PICKAXE);
        if (silkTouch) {
            tool.addEnchantment(world.getRegistryManager().get(RegistryKeys.ENCHANTMENT).entryOf(Enchantments.SILK_TOUCH), 1);
        }
        return Block.getDroppedStacks(state, serverWorld, target, world.getBlockEntity(target), null, tool);
    }

    public boolean canMine(BlockPos target, BlockState state) {
        if (state.getBlock() instanceof FluidBlock || state.getHardness(world, target) < 0.0F) {
            return false;
        }
        List<ItemStack> drops = getDrops(target, state);
        if (drops.isEmpty()) {
            return false;
        }
        if (state.hasBlockEntity() && !OreValues.isValuable(state)) {
            return false;
        }

        for (ItemStack drop : drops) {
            for (ItemStack filter : filterSlot) {
                if (!filter.isEmpty() && drop.isOf(filter.getItem())) {
                    return !blacklist;
                }
            }
        }
        return blacklist;
    }

    /** The window's buttons: restart from the top, switch black/whitelist, switch silk touch. */
    @Override
    public void onGuiEvent(PlayerEntity player, String event) {
        switch (event) {
            case "reset" -> mineTarget = null;
            case "mode" -> {
                if (!getActive()) {
                    blacklist = !blacklist;
                }
            }
            case "silktouch" -> {
                if (!getActive()) {
                    silkTouch = !silkTouch;
                }
            }
            default -> {
                return;
            }
        }
        markDirty();
    }

    public BlockPos getMineTarget() {
        return mineTarget;
    }

    @Override
    public double getGuiAmount(String name) {
        if (mineTarget != null) {
            switch (name) {
                case "targetX" -> {
                    return mineTarget.getX() - pos.getX();
                }
                case "targetY" -> {
                    return mineTarget.getY() - pos.getY();
                }
                case "targetZ" -> {
                    return mineTarget.getZ() - pos.getZ();
                }
                default -> { }
            }
        }
        return super.getGuiAmount(name);
    }

    @Override
    public boolean getGuiCondition(String name) {
        return switch (name) {
            case "blacklist" -> blacklist;
            case "silkTouch" -> silkTouch;
            case "hasTarget" -> mineTarget != null;
            default -> super.getGuiCondition(name);
        };
    }

    @Override
    public void onPlaced(ItemStack stack, LivingEntity placer) {
        super.onPlaced(stack, placer);
        energyStored = Math.min(maxEnergy, energyStored + dropData(stack).getDouble("energy"));
        markDirty();
    }

    @Override
    protected ItemStack adjustDrop(ItemStack drop, boolean wrench) {
        drop = super.adjustDrop(drop, wrench);
        if (drop != null && (wrench || getDropTeBlock().getDefaultDrop() == TeBlock.DefaultDrop.Self)) {
            double retainedRatio = ConfigUtil.getDouble(MainConfig.get(), "balance/energyRetainedInStorageBlockDrops");
            if (retainedRatio > 0.0 && energyStored > 0.0) {
                double energy = energyStored * retainedRatio;
                editDropData(drop, nbt -> nbt.putDouble("energy", energy));
            }
        }
        return drop;
    }

    @Override
    public Set<UpgradableProperty> getUpgradableProperties() {
        return EnumSet.of(UpgradableProperty.Augmentable, UpgradableProperty.RedstoneSensitive, UpgradableProperty.Transformer);
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.readNbt(nbt, lookup);
        mineTarget = nbt.contains("mineTargetX")
                ? new BlockPos(nbt.getInt("mineTargetX"), nbt.getInt("mineTargetY"), nbt.getInt("mineTargetZ")) : null;
        blacklist = !nbt.contains("blacklist") || nbt.getBoolean("blacklist");
        silkTouch = nbt.getBoolean("silkTouch");
    }

    @Override
    public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.writeNbt(nbt, lookup);
        if (mineTarget != null) {
            nbt.putInt("mineTargetX", mineTarget.getX());
            nbt.putInt("mineTargetY", mineTarget.getY());
            nbt.putInt("mineTargetZ", mineTarget.getZ());
        }
        nbt.putBoolean("blacklist", blacklist);
        nbt.putBoolean("silkTouch", silkTouch);
    }
}
