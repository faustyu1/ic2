package ic2.core.mixin;

import ic2.core.entity.EntityIC2Boat;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.tag.TagKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * The two things the IC2 boats do differently from a vanilla boat, which 1.12.2's EntityIC2Boat
 * got by copying the boat's update: how strongly paddling speeds the boat up, and which fluids it
 * floats on (the electric boat floats on lava as well).
 */
@Mixin(BoatEntity.class)
public abstract class BoatEntityMixin {

    @ModifyArg(method = "updatePaddles",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/Vec3d;add(DDD)Lnet/minecraft/util/math/Vec3d;"), index = 0)
    private double ic2$accelerateX(double x) {
        return (Object) this instanceof EntityIC2Boat boat ? x * boat.getAccelerationFactor() : x;
    }

    @ModifyArg(method = "updatePaddles",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/Vec3d;add(DDD)Lnet/minecraft/util/math/Vec3d;"), index = 2)
    private double ic2$accelerateZ(double z) {
        return (Object) this instanceof EntityIC2Boat boat ? z * boat.getAccelerationFactor() : z;
    }

    @Redirect(method = {"getWaterHeightBelow", "checkBoatInWater", "getUnderWaterLocation"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/fluid/FluidState;isIn(Lnet/minecraft/registry/tag/TagKey;)Z"))
    private boolean ic2$floatsOn(FluidState fluid, TagKey<Fluid> tag) {
        return (Object) this instanceof EntityIC2Boat boat ? boat.isWater(fluid) : fluid.isIn(tag);
    }
}
