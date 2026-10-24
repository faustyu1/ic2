package ic2.core.mixin.client;

import ic2.core.client.audio.ToolSoundHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Brackets the local player's block breaking, so a drill can play its own break sound. */
@Mixin(ClientPlayerInteractionManager.class)
public abstract class ClientPlayerInteractionManagerDrillMixin {
    @Shadow @Final private MinecraftClient client;

    @Inject(method = "breakBlock", at = @At("HEAD"))
    private void ic2$beginDrillBreak(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        ToolSoundHandler.beginBreak(client, pos);
    }

    @Inject(method = "breakBlock", at = @At("RETURN"))
    private void ic2$endDrillBreak(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        ToolSoundHandler.endBreak(client, cir.getReturnValue());
    }
}
