package ic2.integration.jade;

import ic2.core.block.machine.tileentity.TileEntityStandardMachine;
import java.util.List;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import snownee.jade.api.Accessor;
import snownee.jade.api.view.ClientViewGroup;
import snownee.jade.api.view.IClientExtensionProvider;
import snownee.jade.api.view.IServerExtensionProvider;
import snownee.jade.api.view.ProgressView;
import snownee.jade.api.view.ViewGroup;

/** How far a processing machine is through its current operation, shown while it works. */
public enum ProgressProvider implements IServerExtensionProvider<NbtCompound>, IClientExtensionProvider<NbtCompound, ProgressView> {
    INSTANCE;

    private static final Identifier UID = Identifier.of("ic2", "progress");

    @Override
    public List<ViewGroup<NbtCompound>> getGroups(Accessor<?> accessor) {
        if (!(accessor.getTarget() instanceof TileEntityStandardMachine<?, ?, ?> machine) || machine.getProgress() <= 0.0F) {
            return null;
        }
        return List.of(new ViewGroup<>(List.of(ProgressView.create(machine.getProgress()))));
    }

    @Override
    public List<ClientViewGroup<ProgressView>> getClientGroups(Accessor<?> accessor, List<ViewGroup<NbtCompound>> groups) {
        return ClientViewGroup.map(groups, ProgressView::read, null);
    }

    @Override
    public Identifier getUid() {
        return UID;
    }
}
