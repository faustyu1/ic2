package ic2.core.block.state;

import net.minecraft.util.StringIdentifiable;

public interface IIdProvider extends StringIdentifiable {
    String getName();
    int getId();
    default int getColor() { return 16777215; }
    default String getModelName() { return getName(); }

    @Override
    default String asString() {
        return getName();
    }
}
