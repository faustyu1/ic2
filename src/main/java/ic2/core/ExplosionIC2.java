package ic2.core;

import ic2.api.event.ExplosionEvent;
import ic2.api.item.IHazmatLike;
import ic2.api.tile.ExplosionWhitelist;
import ic2.core.init.IC2Sounds;
import ic2.core.util.Util;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import org.jetbrains.annotations.Nullable;

/**
 * Fabric port of {@code ic2.core.ExplosionIC2}: the explosion of the nuke, the industrial TNT,
 * the reactor, overloaded cables and the explosive mining laser.
 *
 * <p>Unlike the vanilla explosion it shoots one ray per direction step from the centre and lets
 * each block soak up part of the ray's power (its blast resistance, more for the heat explosion,
 * water only stopping the non-normal kinds); a ray that meets a very resistant block splits into
 * five weaker ones. Entities take damage from the rays passing near them, items survive with a
 * health of 5, and what the broken blocks drop is gathered into stacks per 2x2 column - so a nuke
 * does not leave ten thousand item entities. A nuclear explosion also starves and irradiates the
 * mobs around it, unless they wear a full hazmat suit.
 *
 * <p>Ported without changes to the arithmetic, including the original's quirks: the motion limit
 * mixes the pushed X motion with the entity's own Y and Z motion, and the radiation only reaches
 * mobs, not players. What changed is the plumbing: the world height starts at the world's bottom
 * rather than at 0, blocks are read straight from the world instead of a chunk cache, the client
 * effects are played from the server instead of through IC2's own packet, and the cancelable
 * Forge event is {@link ExplosionEvent}.
 */
public class ExplosionIC2 extends Explosion {

    public enum Type {
        Normal,
        Heat,
        Electrical,
        Nuclear
    }

    private static final double DROP_POWER_LIMIT = 8.0;
    private static final double MOTION_LIMIT = 60.0;
    private static final int SECONDARY_RAY_COUNT = 5;
    private static final int BIT_SET_ELEMENT_SIZE = 2;

    private final World worldObj;
    @Nullable private final Entity exploder;
    private final double explosionX;
    private final double explosionY;
    private final double explosionZ;
    private final int bottomY;
    private final int mapHeight;
    private final float power;
    private final float explosionDropRate;
    private final Type type;
    private final int radiationRange;
    @Nullable private final LivingEntity igniter;
    private final Random rng = new Random();
    private final double maxDistance;
    private final int areaSize;
    private final int areaX;
    private final int areaZ;
    private final DamageSource damageSource;
    private final List<EntityDamage> entitiesInRange = new ArrayList<>();
    /** Per height above the bottom of the world, two bits per column: 1 destroy and drop, 3 destroy only. */
    private final long[][] destroyedBlockPositions;

    public ExplosionIC2(World world, @Nullable Entity entity, double x, double y, double z, float power, float drop) {
        this(world, entity, x, y, z, power, drop, Type.Normal);
    }

    public ExplosionIC2(World world, @Nullable Entity entity, double x, double y, double z, float power, float drop, Type type) {
        this(world, entity, x, y, z, power, drop, type, null, 0);
    }

    public ExplosionIC2(World world, @Nullable Entity entity, BlockPos pos, float power, float drop, Type type) {
        this(world, entity, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, power, drop, type);
    }

    public ExplosionIC2(World world, @Nullable Entity entity, double x, double y, double z, float power, float drop,
                        Type type, @Nullable LivingEntity igniter, int radiationRange) {
        super(world, entity, x, y, z, power, false, DestructionType.DESTROY);
        this.worldObj = world;
        this.exploder = entity;
        this.explosionX = x;
        this.explosionY = y;
        this.explosionZ = z;
        this.bottomY = world.getBottomY();
        this.mapHeight = world.getHeight();
        this.power = power;
        this.explosionDropRate = drop;
        this.type = type;
        this.igniter = igniter;
        this.radiationRange = radiationRange;
        this.maxDistance = this.power / 0.4;
        int maxDistanceInt = (int) Math.ceil(this.maxDistance);
        this.areaSize = maxDistanceInt * 2;
        this.areaX = MathHelper.floor(x) - maxDistanceInt;
        this.areaZ = MathHelper.floor(z) - maxDistanceInt;
        this.damageSource = isNuclear()
                ? IC2DamageSource.nuke(world, entity, igniter)
                : world.getDamageSources().explosion(this);
        this.destroyedBlockPositions = new long[this.mapHeight][];
    }

    // ---- the overloads the rest of the port calls, without an exploding entity ----

    public ExplosionIC2(World world, double x, double y, double z, float power, float drop, Type type) {
        this(world, null, x, y, z, power, drop, type);
    }

    public ExplosionIC2(World world, BlockPos pos, float power, float drop, Type type) {
        this(world, null, pos, power, drop, type);
    }

    public void doExplosion() {
        if (power <= 0.0F || worldObj.isClient) {
            return;
        }

        ExplosionEvent event = new ExplosionEvent(worldObj, exploder, getPosition(), power, igniter, radiationRange, maxDistance);
        if (!ExplosionEvent.post(event)) {
            return;
        }

        int range = areaSize / 2;
        BlockPos pos = BlockPos.ofFloored(getPosition());
        BlockPos start = pos.add(-range, -range, -range);
        BlockPos end = pos.add(range, range, range);

        for (Entity entity : worldObj.getOtherEntities(null, new Box(Vec3d.of(start), Vec3d.of(end)))) {
            if (entity instanceof LivingEntity || entity instanceof ItemEntity) {
                int distance = (int) (Util.square(entity.getX() - explosionX)
                        + Util.square(entity.getY() - explosionY)
                        + Util.square(entity.getZ() - explosionZ));
                entitiesInRange.add(new EntityDamage(entity, distance, getEntityHealth(entity)));
            }
        }

        boolean entitiesAreInRange = !entitiesInRange.isEmpty();
        if (entitiesAreInRange) {
            entitiesInRange.sort((a, b) -> a.distance - b.distance);
        }

        int steps = (int) Math.ceil(Math.PI / Math.atan(1.0 / maxDistance));
        BlockPos.Mutable tmpPos = new BlockPos.Mutable();

        for (int phiN = 0; phiN < 2 * steps; phiN++) {
            for (int thetaN = 0; thetaN < steps; thetaN++) {
                double phi = (Math.PI * 2) / steps * phiN;
                double theta = Math.PI / steps * thetaN;
                shootRay(explosionX, explosionY, explosionZ, phi, theta, power,
                        entitiesAreInRange && phiN % 8 == 0 && thetaN % 8 == 0, tmpPos);
            }
        }

        for (EntityDamage entry : entitiesInRange) {
            Entity entity = entry.entity;
            entity.damage(damageSource, (float) entry.damage);

            double motionSq = Util.square(entry.motionX) + Util.square(entity.getVelocity().y) + Util.square(entity.getVelocity().z);
            double reduction = motionSq > MOTION_LIMIT * MOTION_LIMIT ? Math.sqrt(MOTION_LIMIT * MOTION_LIMIT / motionSq) : 1.0;
            entity.setVelocity(entity.getVelocity().add(entry.motionX * reduction, entry.motionY * reduction, entry.motionZ * reduction));
            // a player moves itself, so it has to be told
            entity.velocityModified = true;
        }

        if (isNuclear() && radiationRange >= 1) {
            Box box = new Box(explosionX - radiationRange, explosionY - radiationRange, explosionZ - radiationRange,
                    explosionX + radiationRange, explosionY + radiationRange, explosionZ + radiationRange);
            for (MobEntity entity : worldObj.getEntitiesByClass(MobEntity.class, box, e -> true)) {
                if (IHazmatLike.hasCompleteHazmat(entity)) continue;

                double distance = Math.sqrt(entity.squaredDistanceTo(explosionX, explosionY, explosionZ));
                int hungerLength = (int) (120.0 * (radiationRange - distance));
                int poisonLength = (int) (80.0 * (radiationRange / 3 - distance));
                if (hungerLength >= 0) {
                    entity.addStatusEffect(new StatusEffectInstance(StatusEffects.HUNGER, hungerLength, 0));
                }
                if (poisonLength >= 0) {
                    IC2Potion.applyTo(entity, poisonLength, 0);
                }
            }
        }

        playEffect();

        boolean doDrops = worldObj.getGameRules().getBoolean(GameRules.DO_TILE_DROPS);
        Map<XZposition, Map<StackKey, DropData>> blocksToDrop = new HashMap<>();

        for (int yIndex = 0; yIndex < destroyedBlockPositions.length; yIndex++) {
            long[] bitSet = destroyedBlockPositions[yIndex];
            if (bitSet == null) continue;

            int y = yIndex + bottomY;
            int index = -2;
            while ((index = nextSetIndex(index + 2, bitSet, BIT_SET_ELEMENT_SIZE)) != -1) {
                int realIndex = index / 2;
                int z = realIndex / areaSize;
                int x = realIndex - z * areaSize;
                x += areaX;
                z += areaZ;
                tmpPos.set(x, y, z);
                BlockState state = worldObj.getBlockState(tmpPos);
                if (state.isAir()) continue;
                Block block = state.getBlock();
                if (worldObj.getBlockEntity(tmpPos) instanceof ic2.core.block.TileEntityBlock tile && tile.onExploded(this)) {
                    continue;
                }

                if (doDrops && block.shouldDropItemsOnExplosion(this) && getAtIndex(index, bitSet, BIT_SET_ELEMENT_SIZE) == 1
                        && worldObj instanceof ServerWorld serverWorld) {
                    BlockEntity be = worldObj.getBlockEntity(tmpPos);
                    for (ItemStack stack : Block.getDroppedStacks(state, serverWorld, tmpPos, be, exploder, ItemStack.EMPTY)) {
                        if (rng.nextFloat() > explosionDropRate || stack.isEmpty()) continue;

                        XZposition column = new XZposition(x / 2, z / 2);
                        Map<StackKey, DropData> map = blocksToDrop.computeIfAbsent(column, k -> new HashMap<>());
                        StackKey key = new StackKey(stack);
                        DropData data = map.get(key);
                        if (data == null) {
                            map.put(new StackKey(stack.copyWithCount(1)), new DropData(stack.getCount(), y));
                        } else {
                            data.add(stack.getCount(), y);
                        }
                    }
                }

                // what Forge's onBlockExploded did: clear the block, then tell it
                worldObj.setBlockState(tmpPos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
                block.onDestroyedByExplosion(worldObj, tmpPos.toImmutable(), this);
            }
        }

        for (Map.Entry<XZposition, Map<StackKey, DropData>> entry : blocksToDrop.entrySet()) {
            XZposition column = entry.getKey();

            for (Map.Entry<StackKey, DropData> entry2 : entry.getValue().entrySet()) {
                ItemStack template = entry2.getKey().stack;
                int count = entry2.getValue().n;

                while (count > 0) {
                    int stackSize = Math.min(count, template.getMaxCount());
                    ItemEntity item = new ItemEntity(worldObj,
                            (column.x + worldObj.random.nextFloat()) * 2.0F,
                            entry2.getValue().maxY + 0.5,
                            (column.z + worldObj.random.nextFloat()) * 2.0F,
                            template.copyWithCount(stackSize));
                    item.setToDefaultPickupDelay();
                    worldObj.spawnEntity(item);
                    count -= stackSize;
                }
            }
        }
    }

    /**
     * The sound and the cloud, as NetworkManagerClient played them on the explosion effect
     * packet: the vanilla explosion sound for the normal kind, the fizz for heat, the machine
     * overload for electrical and the nuke's own bang.
     */
    private void playEffect() {
        SoundEvent sound;
        float volume = 4.0F;
        float pitch = (1.0F + (worldObj.random.nextFloat() - worldObj.random.nextFloat()) * 0.2F) * 0.7F;
        switch (type) {
            case Heat -> sound = SoundEvents.BLOCK_FIRE_EXTINGUISH;
            case Electrical -> {
                sound = IC2Sounds.MACHINES_MACHINE_OVERLOAD;
                volume = 1.0F;
                pitch = 1.0F;
            }
            case Nuclear -> {
                sound = IC2Sounds.TOOLS_NUKE_EXPLOSION;
                volume = 1.0F;
                pitch = 1.0F;
            }
            default -> sound = SoundEvents.ENTITY_GENERIC_EXPLODE.value();
        }
        worldObj.playSound(null, explosionX, explosionY, explosionZ, sound, SoundCategory.BLOCKS, volume, pitch);
        if (worldObj instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, explosionX, explosionY, explosionZ, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    /** Marks a block to go; {@code noDrop} keeps it from dropping anything. */
    public void destroy(int x, int y, int z, boolean noDrop) {
        destroyUnchecked(x, y, z, noDrop);
    }

    private void destroyUnchecked(int x, int y, int z, boolean noDrop) {
        int index = (z - areaZ) * areaSize + (x - areaX);
        index *= 2;
        int yIndex = y - bottomY;
        long[] array = destroyedBlockPositions[yIndex];
        if (array == null) {
            array = makeArray(areaSize * areaSize, BIT_SET_ELEMENT_SIZE);
            destroyedBlockPositions[yIndex] = array;
        }

        setAtIndex(index, array, noDrop ? 3 : 1);
    }

    private void shootRay(double x, double y, double z, double phi, double theta, double power1, boolean killEntities, BlockPos.Mutable tmpPos) {
        double deltaX = Math.sin(theta) * Math.cos(phi);
        double deltaY = Math.cos(theta);
        double deltaZ = Math.sin(theta) * Math.sin(phi);
        int step = 0;

        while (true) {
            int blockY = MathHelper.floor(y);
            if (blockY < bottomY || blockY >= bottomY + mapHeight) {
                break;
            }

            int blockX = MathHelper.floor(x);
            int blockZ = MathHelper.floor(z);
            // the area is a square around the centre; a split ray can wander out of it
            if (blockX < areaX || blockX >= areaX + areaSize || blockZ < areaZ || blockZ >= areaZ + areaSize) {
                break;
            }

            tmpPos.set(blockX, blockY, blockZ);
            BlockState state = worldObj.getBlockState(tmpPos);
            double absorption = getAbsorption(state);
            if (absorption < 0.0) {
                break;
            }

            if (absorption > 1000.0 && !ExplosionWhitelist.isBlockWhitelisted(state.getBlock())) {
                absorption = 0.5;
            } else {
                if (absorption > power1) {
                    break;
                }

                if (!state.isAir()) {
                    destroyUnchecked(blockX, blockY, blockZ, power1 > DROP_POWER_LIMIT);
                }
            }

            if (killEntities && (step + 4) % 8 == 0 && !entitiesInRange.isEmpty() && power1 >= 0.25) {
                damageEntities(x, y, z, step, power1);
            }

            if (absorption > 10.0) {
                for (int i = 0; i < SECONDARY_RAY_COUNT; i++) {
                    shootRay(x, y, z, rng.nextDouble() * 2.0 * Math.PI, rng.nextDouble() * Math.PI, absorption * 0.4, false, tmpPos);
                }
            }

            power1 -= absorption;
            x += deltaX;
            y += deltaY;
            z += deltaZ;
            step++;
        }
    }

    private double getAbsorption(BlockState state) {
        double ret = 0.5;
        if (state.isAir()) {
            return ret;
        }

        if ((state.isOf(Blocks.WATER)) && type != Type.Normal) {
            ret++;
        } else {
            float resistance = getBlastResistance(state);
            if (resistance < 0.0F) {
                return resistance;
            }

            double extra = (resistance + 4.0F) * 0.3;
            if (type != Type.Heat) {
                ret += extra;
            } else {
                ret += extra * 6.0;
            }
        }

        return ret;
    }

    /**
     * The block's blast resistance, except for the blocks IC2 made weaker in 1.12.2 (with
     * {@code setResistance} in {@code IC2.init}): the enchanting table, the ender chest and the
     * anvil, which would otherwise stop a nuke, and water and lava. The original changed the
     * blocks themselves, for every explosion; the port only applies it to its own.
     */
    private static float getBlastResistance(BlockState state) {
        if (state.isOf(Blocks.WATER) || state.isOf(Blocks.LAVA)) {
            return 18.0F;
        }
        if (state.isOf(Blocks.ENCHANTING_TABLE) || state.isOf(Blocks.ENDER_CHEST) || state.isIn(net.minecraft.registry.tag.BlockTags.ANVIL)) {
            return 36.0F;
        }
        return state.getBlock().getBlastResistance();
    }

    private void damageEntities(double x, double y, double z, int step, double power) {
        int index;
        if (step != 4) {
            int distanceMin = (int) Util.square(step - 5);
            int indexStart = 0;
            int indexEnd = entitiesInRange.size() - 1;

            do {
                index = (indexStart + indexEnd) / 2;
                int distance = entitiesInRange.get(index).distance;
                if (distance < distanceMin) {
                    indexStart = index + 1;
                } else if (distance > distanceMin) {
                    indexEnd = index - 1;
                } else {
                    indexEnd = index;
                }
            } while (indexStart < indexEnd);
        } else {
            index = 0;
        }

        int distanceMax = (int) Util.square(step + 5);

        for (int i = Math.max(index, 0); i < entitiesInRange.size(); i++) {
            EntityDamage entry = entitiesInRange.get(i);
            if (entry.distance >= distanceMax) {
                break;
            }

            Entity entity = entry.entity;
            if (Util.square(entity.getX() - x) + Util.square(entity.getY() - y) + Util.square(entity.getZ() - z) <= 25.0) {
                double damage = 4.0 * power;
                entry.damage += damage;
                entry.health -= damage;
                double dx = entity.getX() - explosionX;
                double dy = entity.getY() - explosionY;
                double dz = entity.getZ() - explosionZ;
                double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
                entry.motionX += dx / distance * 0.0875 * power;
                entry.motionY += dy / distance * 0.0875 * power;
                entry.motionZ += dz / distance * 0.0875 * power;
                if (entry.health <= 0.0) {
                    entity.damage(damageSource, (float) entry.damage);
                    if (!entity.isAlive()) {
                        entitiesInRange.remove(i);
                        i--;
                    }
                }
            }
        }
    }

    /** Who set it off, as {@code getExplosivePlacedBy} answered in 1.12.2. */
    @Override
    public @Nullable LivingEntity getCausingEntity() {
        return igniter != null ? igniter : super.getCausingEntity();
    }

    public Type getIc2Type() {
        return type;
    }

    public float getDropRate() {
        return explosionDropRate;
    }

    private boolean isNuclear() {
        return type == Type.Nuclear;
    }

    private static double getEntityHealth(Entity entity) {
        return entity instanceof ItemEntity ? 5.0 : Double.POSITIVE_INFINITY;
    }

    // ---- a bit set packing a few bits per column, as the original kept it ----

    private static long[] makeArray(int size, int step) {
        return new long[(size * step + 8 - step) / 8];
    }

    private static int nextSetIndex(int start, long[] array, int step) {
        int offset = start % 8;

        for (int i = start / 8; i < array.length; i++) {
            long aval = array[i];

            for (int j = offset; j < 8; j += step) {
                int val = (int) (aval >> j & ((1L << step) - 1));
                if (val != 0) {
                    return i * 8 + j;
                }
            }

            offset = 0;
        }

        return -1;
    }

    private static int getAtIndex(int index, long[] array, int step) {
        return (int) (array[index / 8] >>> index % 8 & ((1L << step) - 1));
    }

    private static void setAtIndex(int index, long[] array, int value) {
        array[index / 8] = array[index / 8] | (long) value << index % 8;
    }

    private static final class DropData {
        int n;
        int maxY;

        DropData(int n, int y) {
            this.n = n;
            this.maxY = y;
        }

        void add(int n1, int y) {
            n += n1;
            if (y > maxY) {
                maxY = y;
            }
        }
    }

    private static final class EntityDamage {
        final Entity entity;
        final int distance;
        double health;
        double damage;
        double motionX;
        double motionY;
        double motionZ;

        EntityDamage(Entity entity, int distance, double health) {
            this.entity = entity;
            this.distance = distance;
            this.health = health;
        }
    }

    private record XZposition(int x, int z) {}

    /** An item stack compared by item and components, ignoring the count. */
    private static final class StackKey {
        final ItemStack stack;

        StackKey(ItemStack stack) {
            this.stack = stack;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof StackKey other && ItemStack.areItemsAndComponentsEqual(stack, other.stack);
        }

        @Override
        public int hashCode() {
            return ItemStack.hashCode(stack);
        }
    }
}
