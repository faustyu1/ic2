package ic2.integration.jade;

import ic2.core.block.TileEntityBlock;
import java.util.List;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import snownee.jade.api.Accessor;
import snownee.jade.api.view.ClientViewGroup;
import snownee.jade.api.view.EnergyView;
import snownee.jade.api.view.IClientExtensionProvider;
import snownee.jade.api.view.IServerExtensionProvider;
import snownee.jade.api.view.ViewGroup;

/** The EU a block holds out of its buffer, read on the server and drawn as Jade's energy bar. */
public enum EnergyProvider implements IServerExtensionProvider<NbtCompound>, IClientExtensionProvider<NbtCompound, EnergyView> {
    INSTANCE;

    private static final Identifier UID = Identifier.of("ic2", "energy");

    @Override
    public List<ViewGroup<NbtCompound>> getGroups(Accessor<?> accessor) {
        if (!(accessor.getTarget() instanceof TileEntityBlock tile)) {
            return null;
        }
        double capacity = tile.getEnergyCapacityForDisplay();
        if (capacity <= 0.0) {
            return null;
        }
        double stored = Math.max(0.0, Math.min(tile.getEnergyForDisplay(), capacity));
        return List.of(new ViewGroup<>(List.of(EnergyView.of((long) stored, (long) capacity))));
    }

    @Override
    public List<ClientViewGroup<EnergyView>> getClientGroups(Accessor<?> accessor, List<ViewGroup<NbtCompound>> groups) {
        return ClientViewGroup.map(groups, tag -> EnergyView.read(tag, "EU"), null);
    }

    @Override
    public Identifier getUid() {
        return UID;
    }
}
