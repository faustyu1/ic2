package ic2.core.block.kineticgenerator.tileentity;

import ic2.api.energy.tile.IKineticSource;
import ic2.api.item.IKineticRotor;
import ic2.core.block.TileEntityInventory;
import ic2.core.block.invslot.InvSlot;
import ic2.core.block.invslot.InvSlotConsumableKineticRotor;
import ic2.core.ref.TeBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * What the wind and the water kinetic generators share: a rotor slot, the kinetic energy they
 * hand out of their back, and the check for the space the rotor sweeps.
 *
 * <p>In 1.12.2 both were separate copies of the same code; the arithmetic is unchanged, the copy
 * is gone.
 */
public abstract class TileEntityRotorKineticGenerator extends TileEntityInventory implements IKineticSource {
    public final InvSlotConsumableKineticRotor rotorSlot;
    protected int updateTicker;
    protected float rotationSpeed;
    /** What the clients were last told, so they hear again only when the rotor or its speed changes. */
    private Item syncedRotor = Items.AIR;
    private float syncedSpeed;
    /** The angle the rotor is drawn at, turned on by the renderer every frame. */
    private float clientAngle;
    private double clientAngleTime = Double.NaN;

    protected TileEntityRotorKineticGenerator(BlockEntityType<?> type, BlockPos pos, BlockState state, TeBlock teBlock,
                                              IKineticRotor.GearboxType gearbox) {
        super(type, pos, state, teBlock);
        this.rotorSlot = new InvSlotConsumableKineticRotor(this, "rotorslot", InvSlot.Access.IO, 1, InvSlot.InvSide.ANY, gearbox);
        this.updateTicker = ic2.core.IC2.random.nextInt(getTickRate());
    }

    protected abstract int getTickRate();

    /** The kinetic energy a tick, which the generator behind takes. */
    public abstract int getKuOutput();

    /** Whether a block in the swept area is not what the rotor may turn in. */
    protected abstract boolean blocksRotor(BlockPos pos);

    public boolean hasRotor() {
        return !rotorSlot.isEmpty();
    }

    protected IKineticRotor rotor() {
        ItemStack stack = rotorSlot.get();
        return stack.getItem() instanceof IKineticRotor rotor ? rotor : null;
    }

    public int getRotorDiameter() {
        IKineticRotor rotor = rotor();
        return rotor == null ? 0 : rotor.getDiameter(rotorSlot.get());
    }

    public float getEfficiency() {
        IKineticRotor rotor = rotor();
        return rotor == null ? 0.0F : rotor.getEfficiency(rotorSlot.get());
    }

    /** Health of the rotor in percent, as the window shows it. */
    public int getRotorHealth() {
        ItemStack stack = rotorSlot.get();
        int max = ic2.core.item.DamageHandler.getMaxDamage(stack);
        return stack.isEmpty() || max <= 0 ? 0 : (int) (100.0F - (float) ic2.core.item.DamageHandler.getDamage(stack) / max * 100.0F);
    }

    public float getRotationSpeed() {
        return rotationSpeed;
    }

    /** The picture the renderer wraps around the rotor blades, or null without a rotor. */
    public Identifier getRotorRenderTexture() {
        IKineticRotor rotor = rotor();
        return rotor == null ? null : rotor.getRotorRenderTexture(rotorSlot.get());
    }

    /**
     * The rotor's angle in degrees at {@code time} (world ticks with the partial tick), turned on by
     * the synced speed since the last frame - a full turn a second at the top speed of 2.
     */
    public float getRotorAngle(double time) {
        if (!Double.isNaN(clientAngleTime)) {
            clientAngle = (float) ((clientAngle + (time - clientAngleTime) * rotationSpeed * 9.0) % 360.0);
        }
        clientAngleTime = time;
        return clientAngle;
    }

    // ---------------------------------------------------------------- what the clients draw

    @Override
    public void markDirty() {
        super.markDirty();
        syncRotor();
    }

    /** Sends the rotor and its speed to the clients when either changed. */
    protected void syncRotor() {
        if (world == null || world.isClient || rotorSlot == null) {
            return;
        }
        Item rotor = rotorSlot.get().getItem();
        if (rotor != syncedRotor || Math.abs(rotationSpeed - syncedSpeed) > 0.01F) {
            syncedRotor = rotor;
            syncedSpeed = rotationSpeed;
            world.updateListeners(pos, getCachedState(), getCachedState(), Block.NOTIFY_LISTENERS);
        }
    }

    @Override
    public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.writeNbt(nbt, registries);
        nbt.putFloat("rotationSpeed", rotationSpeed);
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.readNbt(nbt, registries);
        rotationSpeed = nbt.getFloat("rotationSpeed");
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registries) {
        return createNbt(registries);
    }

    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    /**
     * Counts the rows of the area in front of the generator that something blocks.
     *
     * @param length     how far forward to look
     * @param onlyRotor  just the disc the rotor itself turns in
     * @return the blocked rows, or -1 when another generator of the kind is in the way
     */
    public int checkSpace(int length, boolean onlyRotor) {
        int box = getRotorDiameter() / 2;
        int lentemp = 0;
        if (onlyRotor) {
            length = 1;
            lentemp = length + 1;
        } else {
            box *= 2;
        }

        Direction fwdDir = getFacing();
        Direction rightDir = fwdDir.rotateYClockwise();
        int ret = 0;
        BlockPos.Mutable cursor = new BlockPos.Mutable();

        for (int up = -box; up <= box; up++) {
            for (int right = -box; right <= box; right++) {
                boolean occupied = false;
                for (int fwd = lentemp - length; fwd <= length; fwd++) {
                    cursor.set(pos.getX() + fwd * fwdDir.getOffsetX() + right * rightDir.getOffsetX(),
                            pos.getY() + up,
                            pos.getZ() + fwd * fwdDir.getOffsetZ() + right * rightDir.getOffsetZ());
                    if (blocksRotor(cursor)) {
                        occupied = true;
                        if ((up != 0 || right != 0 || fwd != 0) && !onlyRotor
                                && world.getBlockEntity(cursor) != null && world.getBlockEntity(cursor).getClass() == getClass()) {
                            return -1;
                        }
                    }
                }
                if (occupied) {
                    ret++;
                }
            }
        }
        return ret;
    }

    public boolean rotorHasSpace() {
        return checkSpace(1, true) == 0;
    }

    // ---------------------------------------------------------------- IKineticSource: out of the back

    private boolean isBack(Direction side) {
        return side.getOpposite() == getFacing();
    }

    @Override
    public int maxrequestkineticenergyTick(Direction directionFrom) {
        return getConnectionBandwidth(directionFrom);
    }

    @Override
    public int getConnectionBandwidth(Direction side) {
        return isBack(side) ? getKuOutput() : 0;
    }

    @Override
    public int requestkineticenergy(Direction directionFrom, int amount) {
        return drawKineticEnergy(directionFrom, amount, false);
    }

    @Override
    public int drawKineticEnergy(Direction side, int request, boolean simulate) {
        return isBack(side) ? Math.min(request, getKuOutput()) : 0;
    }

    @Override
    public double getGuiAmount(String name) {
        return switch (name) {
            case "output" -> getKuOutput();
            case "rotorHealth" -> getRotorHealth();
            default -> super.getGuiAmount(name);
        };
    }

    @Override
    public boolean getGuiCondition(String name) {
        return switch (name) {
            case "hasRotor" -> hasRotor();
            case "rotorSpace" -> hasRotor() && rotorHasSpace();
            default -> super.getGuiCondition(name);
        };
    }
}
