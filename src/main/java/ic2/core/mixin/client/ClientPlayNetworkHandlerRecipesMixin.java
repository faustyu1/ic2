package ic2.core.mixin.client;

import ic2.core.recipe.data.MachineRecipeSync;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.SynchronizeRecipesS2CPacket;
import net.minecraft.recipe.RecipeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * On a remote server the machine recipes reach the client with the other recipes; the client's
 * recipe managers take them from there. In single player the integrated server has filled the same
 * managers already.
 */
@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerRecipesMixin {
    @Shadow
    public abstract RecipeManager getRecipeManager();

    @Inject(method = "onSynchronizeRecipes", at = @At("TAIL"))
    private void ic2$takeMachineRecipes(SynchronizeRecipesS2CPacket packet, CallbackInfo ci) {
        if (!MinecraftClient.getInstance().isIntegratedServerRunning()) {
            MachineRecipeSync.apply(getRecipeManager());
        }
    }
}
