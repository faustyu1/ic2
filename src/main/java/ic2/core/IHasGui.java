package ic2.core;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.ScreenHandler;

public interface IHasGui {
    default ScreenHandler getGuiContainer(PlayerEntity player) { return null; }
}
