package ic2.integration.jade;

import ic2.core.block.TileEntityBlock;
import ic2.core.block.machine.tileentity.TileEntityStandardMachine;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/**
 * Jade integration: the EU in a block's buffer as Jade's own energy bar, and the progress of a
 * machine's current operation. Jade already shows the fluids and items through the Fabric
 * Transfer API.
 *
 * <p>Only loaded through the {@code jade} entrypoint, so the mod runs without Jade installed.
 */
@WailaPlugin
public class Ic2JadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerEnergyStorage(EnergyProvider.INSTANCE, TileEntityBlock.class);
        registration.registerProgress(ProgressProvider.INSTANCE, TileEntityStandardMachine.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerEnergyStorageClient(EnergyProvider.INSTANCE);
        registration.registerProgressClient(ProgressProvider.INSTANCE);
    }
}
