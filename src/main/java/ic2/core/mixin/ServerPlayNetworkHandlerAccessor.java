package ic2.core.mixin;

import net.minecraft.server.network.ServerPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Lets the jetpack clear the counter that would otherwise get the flying player kicked, which is
 * what {@code IC2.platform.resetPlayerInAirTime} did on Forge.
 */
@Mixin(ServerPlayNetworkHandler.class)
public interface ServerPlayNetworkHandlerAccessor {
    @Accessor("floatingTicks")
    void ic2$setFloatingTicks(int ticks);
}
