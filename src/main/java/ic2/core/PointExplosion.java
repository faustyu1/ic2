package ic2.core;

import ic2.api.event.ExplosionEvent;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import org.jetbrains.annotations.Nullable;

/**
 * Fabric port of {@code ic2.core.PointExplosion}: the dynamite's blast.
 *
 * <p>It breaks the 3x3x3 cube around it - every block whose blast resistance is below ten times
 * the power - and deals a fixed damage to everything within two blocks, without the falloff and
 * the knockback of a vanilla explosion. Everything it breaks drops (the original set the size to
 * {@code 1 / dropRate} before the second phase, with a drop rate of 1), and it sets fires.
 *
 * <p>The block breaking, drops and fire are the vanilla explosion's second phase, handed the list
 * of blocks; the clients get the same packet a vanilla explosion sends.
 */
public class PointExplosion {
    private final World world;
    @Nullable private final Entity entity;
    @Nullable private final LivingEntity exploder;
    private final double x;
    private final double y;
    private final double z;
    private final float power;
    private final int entityDamage;

    /**
     * @param entity the thing that blew up
     * @param exploder who set it off, if anyone
     * @param entityDamage the damage dealt to everything within two blocks
     */
    public PointExplosion(World world, @Nullable Entity entity, @Nullable LivingEntity exploder, double x, double y, double z,
                          float power, int entityDamage) {
        this.world = world;
        this.entity = entity;
        this.exploder = exploder;
        this.x = x;
        this.y = y;
        this.z = z;
        this.power = power;
        this.entityDamage = entityDamage;
    }

    public void explode() {
        if (!(world instanceof ServerWorld serverWorld)) {
            return;
        }

        Vec3d center = new Vec3d(x, y, z);
        if (!ExplosionEvent.post(new ExplosionEvent(world, entity, center, power, exploder, 0, 1.0))) {
            return;
        }

        List<BlockPos> affected = new ArrayList<>();
        int bx = MathHelper.floor(x);
        int by = MathHelper.floor(y);
        int bz = MathHelper.floor(z);
        for (int px = bx - 1; px <= bx + 1; px++) {
            for (int py = by - 1; py <= by + 1; py++) {
                for (int pz = bz - 1; pz <= bz + 1; pz++) {
                    BlockPos pos = new BlockPos(px, py, pz);
                    BlockState state = world.getBlockState(pos);
                    if (!state.isAir() && state.getBlock().getBlastResistance() < power * 10.0F) {
                        affected.add(pos);
                    }
                }
            }
        }

        Explosion explosion = new Explosion(world, exploder, x, y, z, power, true, Explosion.DestructionType.DESTROY, affected);

        for (Entity hit : world.getOtherEntities(exploder, new Box(x - 2.0, y - 2.0, z - 2.0, x + 2.0, y + 2.0, z + 2.0))) {
            hit.damage(world.getDamageSources().explosion(explosion), entityDamage);
        }

        explosion.affectWorld(false);

        for (ServerPlayerEntity player : serverWorld.getPlayers()) {
            if (player.squaredDistanceTo(x, y, z) < 4096.0) {
                player.networkHandler.sendPacket(new ExplosionS2CPacket(x, y, z, power, affected, Vec3d.ZERO,
                        Explosion.DestructionType.DESTROY, ParticleTypes.EXPLOSION, ParticleTypes.EXPLOSION_EMITTER,
                        SoundEvents.ENTITY_GENERIC_EXPLODE));
            }
        }
    }
}
