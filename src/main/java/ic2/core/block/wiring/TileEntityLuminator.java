package ic2.core.block.wiring;

import ic2.api.energy.EnergyNet;
import ic2.api.energy.tile.IEnergyEmitter;
import ic2.api.energy.tile.IEnergySink;
import ic2.api.energy.tile.IEnergyTile;
import ic2.api.item.ElectricItem;
import ic2.core.block.TileEntityBlock;
import ic2.core.init.IC2Blocks;
import ic2.core.ref.TeBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.Entity;
import net.minecraft.registry.tag.EntityTypeTags;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * Fabric port of TileEntityLuminator - the flat luminator.
 *
 * <p>A light panel on a surface: with a redstone signal (or without one, after a right click or a
 * wrench turned it round) it gives light 15 for 0.25 EU a tick, taken from behind it. Right
 * clicking with a charged item fills its buffer from the item instead, up to 10 000 EU. A lit
 * panel sets hostile mobs touching it on fire, undead for longer. It falls off when what it hangs
 * on is gone and is no energy emitter.
 */
public class TileEntityLuminator extends TileEntityBlock implements IEnergySink {
    private static final double MANUAL_CHARGE_CAPACITY = 10000.0;
    private static final double CAPACITY = 5.0;

    private double energy;
    private boolean invertRedstone;

    public TileEntityLuminator(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type == null ? IC2Blocks.LUMINATOR_FLAT_BE : type, pos, state, TeBlock.luminator_flat);
    }

    public TileEntityLuminator(BlockPos pos, BlockState state) {
        this(null, pos, state);
    }

    @Override
    public void tick() {
        if (world == null || world.isClient) {
            return;
        }
        boolean lit = isLit() && energy >= 0.25;
        if (lit) {
            energy -= 0.25;
            markDirty();
        }
        setActive(lit);
    }

    private boolean isLit() {
        return world.isReceivingRedstonePower(pos) != invertRedstone;
    }

    @Override
    public boolean onActivated(PlayerEntity player, Hand hand) {
        if (!world.isClient) {
            ItemStack stack = player.getStackInHand(hand);
            double amount = MANUAL_CHARGE_CAPACITY - energy;
            if (!stack.isEmpty() && amount > 0.0 && ElectricItem.manager != null
                    && (amount = ElectricItem.manager.discharge(stack, amount, getSinkTier(), true, true, false)) > 0.0) {
                energy += amount;
            } else {
                invertRedstone = !invertRedstone;
            }
            markDirty();
        }
        return true;
    }

    @Override
    public boolean canSetFacingWrench(Direction facing, PlayerEntity player) {
        return true;
    }

    /** The wrench flips the redstone behaviour instead of turning the panel. */
    @Override
    public boolean setFacingWrench(Direction facing, PlayerEntity player) {
        invertRedstone = !invertRedstone;
        markDirty();
        return true;
    }

    @Override
    public void onNeighborUpdate() {
        checkPlacement();
    }

    private void checkPlacement() {
        if (world == null || world.isClient) {
            return;
        }
        BlockPos support = pos.offset(getFacing().getOpposite());
        if (!world.getBlockState(support).isSideSolidFullSquare(world, support, getFacing())) {
            IEnergyTile tile = EnergyNet.instance == null ? null : EnergyNet.instance.getSubTile(world, support);
            if (!(tile instanceof IEnergyEmitter)) {
                world.breakBlock(pos, true);
            }
        }
    }

    @Override
    public void onEntityCollision(Entity entity) {
        if (getActive() && entity instanceof HostileEntity mob) {
            boolean undead = mob.getType().isIn(EntityTypeTags.UNDEAD);
            entity.setOnFireFor(undead ? 20 : 10);
        }
    }

    public boolean isInverted() {
        return invertRedstone;
    }

    public double getEnergy() {
        return energy;
    }

    @Override
    public int getComparatorOutput() {
        return energy <= 0 ? 0 : (int) Math.round(Math.min(1.0, energy / CAPACITY) * 14.0) + 1;
    }

    // ---------------------------------------------------------------- energy from behind

    @Override
    public boolean acceptsEnergyFrom(IEnergyEmitter emitter, Direction side) {
        return side == getFacing().getOpposite();
    }

    @Override
    public double getDemandedEnergy() {
        return Math.max(0.0, CAPACITY - energy);
    }

    @Override
    public double injectEnergy(Direction directionFrom, double amount, double voltage) {
        double accepted = Math.min(amount, getDemandedEnergy());
        energy += accepted;
        markDirty();
        return amount - accepted;
    }

    @Override
    public int getSinkTier() {
        return Integer.MAX_VALUE;
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.readNbt(nbt, lookup);
        invertRedstone = nbt.getBoolean("invert");
        energy = nbt.getDouble("energy");
    }

    @Override
    public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.writeNbt(nbt, lookup);
        nbt.putBoolean("invert", invertRedstone);
        nbt.putDouble("energy", energy);
    }
}
