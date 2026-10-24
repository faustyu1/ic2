package ic2.core.mixin.client;

import ic2.core.client.audio.ToolSoundHandler;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Silences the break sound of a block the local player breaks with a drill, which plays its own. */
@Mixin(World.class)
public abstract class WorldBreakSoundMixin {
    @Inject(method = "playSoundAtBlockCenter", at = @At("HEAD"), cancellable = true)
    private void ic2$drillReplacesBreakSound(BlockPos pos, SoundEvent sound, SoundCategory category, float volume, float pitch,
                                             boolean useDistance, CallbackInfo ci) {
        if (ToolSoundHandler.suppressesBreakSound((World) (Object) this, pos, category)) {
            ci.cancel();
        }
    }
}
