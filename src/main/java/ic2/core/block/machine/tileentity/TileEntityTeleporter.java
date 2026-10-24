package ic2.core.block.machine.tileentity;

import ic2.api.tile.IEnergyStorage;
import ic2.core.IC2;
import ic2.core.block.TileEntityBlock;
import ic2.core.init.IC2Blocks;
import ic2.core.init.IC2Sounds;
import ic2.core.init.MainConfig;
import ic2.core.ref.TeBlock;
import ic2.core.util.ConfigUtil;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.mob.GhastEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import org.joml.Vector3f;

/**
 * Fabric port of TileEntityTeleporter.
 *
 * <p>Two teleporters are linked with the frequency transmitter. While a teleporter gets a
 * redstone signal it sends the nearest entity standing in the 3x3 area above it (up to three
 * blocks high) to its partner. The trip costs {@code weight * (distance + 10)^0.7 * 5} EU, drawn
 * from the energy storage blocks touching the teleporter, evenly from each of them; it has no
 * buffer of its own. A player weighs 1000, a ghast 2500, a wither 5000, the ender dragon 10 000,
 * an animal, minecart or boat 100, any other creature 500, a dropped stack 100 for a full stack;
 * with {@code teleporterUseInventoryWeight} every carried stack adds 100 for a full stack. After
 * an arrival the target teleporter rests for 20 ticks so nothing bounces back at once. A
 * comparator reads 15 from a linked teleporter.
 */
public class TileEntityTeleporter extends TileEntityBlock {
    private BlockPos target;
    private int targetCheckTicker = IC2.random.nextInt(1024);
    private int cooldown = 0;

    public TileEntityTeleporter(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type == null ? IC2Blocks.TELEPORTER_BE : type, pos, state, TeBlock.teleporter);
    }

    public TileEntityTeleporter(BlockPos pos, BlockState state) {
        this(null, pos, state);
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.readNbt(nbt, lookup);
        target = nbt.contains("targetX") ? new BlockPos(nbt.getInt("targetX"), nbt.getInt("targetY"), nbt.getInt("targetZ")) : null;
        cooldown = nbt.getInt("cooldown");
    }

    @Override
    public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.writeNbt(nbt, lookup);
        if (target != null) {
            nbt.putInt("targetX", target.getX());
            nbt.putInt("targetY", target.getY());
            nbt.putInt("targetZ", target.getZ());
        }
        if (cooldown > 0) {
            nbt.putInt("cooldown", cooldown);
        }
    }

    @Override
    public void tick() {
        if (world == null || world.isClient) {
            return;
        }

        boolean coolingDown = cooldown > 0;
        if (coolingDown) {
            cooldown--;
        }

        if (world.isReceivingRedstonePower(pos) && target != null) {
            setActive(true);
            List<Entity> entitiesNearby = coolingDown ? List.of()
                    : world.getEntitiesByClass(Entity.class, new Box(pos.getX() - 1, pos.getY(), pos.getZ() - 1,
                            pos.getX() + 2, pos.getY() + 3, pos.getZ() + 2), entity -> true);

            if (!entitiesNearby.isEmpty() && verifyTarget()) {
                double minDistanceSquared = Double.MAX_VALUE;
                Entity closestEntity = null;
                for (Entity entity : entitiesNearby) {
                    if (entity.getVehicle() == null) {
                        // measured to the block's corner, as BlockPos.distanceSq did
                        double dx = pos.getX() - entity.getX();
                        double dy = pos.getY() - entity.getY();
                        double dz = pos.getZ() - entity.getZ();
                        double distSquared = dx * dx + dy * dy + dz * dz;
                        if (distSquared < minDistanceSquared) {
                            minDistanceSquared = distSquared;
                            closestEntity = entity;
                        }
                    }
                }

                if (closestEntity != null) {
                    teleport(closestEntity, Math.sqrt(pos.getSquaredDistance(target)));
                }
            } else if (++targetCheckTicker % 1024 == 0) {
                verifyTarget();
            }
        } else {
            setActive(false);
        }

        if (getActive() && world instanceof ServerWorld serverWorld) {
            if (cooldown > 0) {
                spawnParticles(serverWorld, 2, pos, GREEN);
            } else {
                spawnParticles(serverWorld, 2, pos, BLUE);
            }
        }
    }

    /** Forgets a partner that is no longer there. */
    private boolean verifyTarget() {
        if (world.getBlockEntity(target) instanceof TileEntityTeleporter) {
            return true;
        }

        target = null;
        markDirty();
        setActive(false);
        updateComparators();
        return false;
    }

    /**
     * Sends the entity to the partner if the storage blocks around hold enough for its weight.
     *
     * @param distance distance between the two teleporters, in blocks
     */
    public void teleport(Entity user, double distance) {
        int weight = getWeightOf(user);
        if (weight == 0) {
            return;
        }

        int energyCost = getEnergyCost(weight, distance);
        int available = getAvailableEnergy();
        if (energyCost > available) {
            // 1.12.2 just did nothing; a player standing on it is told what the trip takes, once a second
            if (user instanceof PlayerEntity player && world.getTime() % 20 == 0) {
                player.sendMessage(net.minecraft.text.Text.translatable("ic2.teleporter.no_energy",
                        energyCost, available), true);
            }
            return;
        }

        consumeEnergy(energyCost);
        user.requestTeleport(target.getX() + 0.5, target.getY() + 1.5 + getYOffset(user), target.getZ() + 0.5);

        if (world.getBlockEntity(target) instanceof TileEntityTeleporter other) {
            other.onTeleportTo(this, user);
        }

        world.playSound(null, pos, IC2Sounds.MACHINES_TELEPORTER_TELE_USE, SoundCategory.BLOCKS, 1.0F, 1.0F);
        world.playSound(null, target, IC2Sounds.MACHINES_TELEPORTER_TELE_USE, SoundCategory.BLOCKS, 1.0F, 1.0F);
        if (world instanceof ServerWorld serverWorld) {
            spawnParticles(serverWorld, 20, pos, BLUE);
            spawnParticles(serverWorld, 20, target, BLUE);
        }
    }

    /** EU a trip costs: {@code weight * (distance + 10)^0.7 * 5}. */
    public static int getEnergyCost(int weight, double distance) {
        return (int) (weight * Math.pow(distance + 10.0, 0.7) * 5.0);
    }

    /**
     * 1.12.2 placed the entity at its own Entity.getYOffset above the arrival point, which was
     * -0.35 for a player and 0 for most other things.
     */
    private static double getYOffset(Entity entity) {
        return entity instanceof PlayerEntity ? -0.35 : 0.0;
    }

    private static final DustParticleEffect BLUE = new DustParticleEffect(new Vector3f(0.0F, 0.0F, 1.0F), 1.0F);
    private static final DustParticleEffect GREEN = new DustParticleEffect(new Vector3f(0.0F, 1.0F, 0.0F), 1.0F);

    /** The redstone dust swirl above a teleporter, at one and two blocks above it. */
    private static void spawnParticles(ServerWorld world, int n, BlockPos pos, DustParticleEffect color) {
        for (int i = 0; i < n; i++) {
            world.spawnParticles(color, pos.getX() + world.random.nextFloat(), pos.getY() + 1 + world.random.nextFloat(),
                    pos.getZ() + world.random.nextFloat(), 1, 0.0, 0.0, 0.0, 0.0);
            world.spawnParticles(color, pos.getX() + world.random.nextFloat(), pos.getY() + 2 + world.random.nextFloat(),
                    pos.getZ() + world.random.nextFloat(), 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    /** Draws the energy from the neighbouring storage blocks, an equal share from each. */
    public void consumeEnergy(int energy) {
        List<IEnergyStorage> energySources = new LinkedList<>();
        for (Direction dir : Direction.values()) {
            BlockEntity neighbour = world.getBlockEntity(pos.offset(dir));
            if (neighbour instanceof IEnergyStorage storage && storage.isTeleporterCompatible(dir.getOpposite()) && storage.getStored() > 0) {
                energySources.add(storage);
            }
        }

        // the caller checked getAvailableEnergy, so the sources cannot run dry before the cost is paid
        while (energy > 0 && !energySources.isEmpty()) {
            int drain = (energy + energySources.size() - 1) / energySources.size();
            Iterator<IEnergyStorage> it = energySources.iterator();
            while (it.hasNext()) {
                IEnergyStorage energySource = it.next();
                if (drain > energy) {
                    drain = energy;
                }

                if (energySource.getStored() <= drain) {
                    energy -= energySource.getStored();
                    energySource.setStored(0);
                    it.remove();
                } else {
                    energy -= drain;
                    energySource.addEnergy(-drain);
                }
            }
        }
    }

    /** EU held by the storage blocks next to the teleporter. */
    public int getAvailableEnergy() {
        int energy = 0;
        for (Direction dir : Direction.values()) {
            BlockEntity neighbour = world.getBlockEntity(pos.offset(dir));
            if (neighbour instanceof IEnergyStorage storage && storage.isTeleporterCompatible(dir.getOpposite())) {
                energy += storage.getStored();
            }
        }
        return energy;
    }

    /** What an entity, its load and its passengers weigh for the energy cost; 0 cannot be sent. */
    public int getWeightOf(Entity user) {
        boolean teleporterUseInventoryWeight = ConfigUtil.getBool(MainConfig.get(), "balance/teleporterUseInventoryWeight");
        int weight = 0;
        if (user instanceof ItemEntity item) {
            ItemStack stack = item.getStack();
            weight += 100 * stack.getCount() / stack.getMaxCount();
        } else if (user instanceof AnimalEntity || user instanceof AbstractMinecartEntity || user instanceof BoatEntity) {
            weight += 100;
        } else if (user instanceof PlayerEntity player) {
            weight += 1000;
            if (teleporterUseInventoryWeight) {
                for (ItemStack stack : player.getInventory().main) {
                    weight += getStackCost(stack);
                }
            }
        } else if (user instanceof GhastEntity) {
            weight += 2500;
        } else if (user instanceof WitherEntity) {
            weight += 5000;
        } else if (user instanceof EnderDragonEntity) {
            weight += 10000;
        } else if (user instanceof PathAwareEntity) {
            weight += 500;
        }

        if (teleporterUseInventoryWeight && user instanceof LivingEntity living) {
            for (ItemStack stack : living.getEquippedItems()) {
                weight += getStackCost(stack);
            }

            // the held item is already counted with the main inventory
            if (user instanceof PlayerEntity) {
                weight -= getStackCost(living.getMainHandStack());
            }
        }

        for (Entity passenger : user.getPassengerList()) {
            weight += getWeightOf(passenger);
        }

        return weight;
    }

    private static int getStackCost(ItemStack stack) {
        return stack.isEmpty() ? 0 : 100 * stack.getCount() / stack.getMaxCount();
    }

    private void onTeleportTo(TileEntityTeleporter from, Entity entity) {
        cooldown = 20;
        markDirty();
    }

    public boolean hasTarget() {
        return target != null;
    }

    public BlockPos getTarget() {
        return target;
    }

    public void setTarget(BlockPos pos) {
        target = pos;
        markDirty();
        updateComparators();
    }

    private void updateComparators() {
        if (world != null) {
            world.updateComparators(pos, getCachedState().getBlock());
        }
    }

    @Override
    public int getComparatorOutput() {
        return target != null ? 15 : 0;
    }
}
