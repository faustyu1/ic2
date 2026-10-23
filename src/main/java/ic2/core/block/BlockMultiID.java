package ic2.core.block;

import ic2.core.block.state.IIdProvider;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;

/**
 * Fabric 1.21.1 port of BlockMultiID.
 * Original (1.12) used IIdProvider enum + BlockStateContainer (Forge) with 16 мета-значений.
 * Fabric/Yarn 1.21.1: generic + StateManager + EnumProperty.of().
 *
 * Notes:
 * - Yarn 1.21.1 maps StateDefinition -> StateManager and EnumProperty -> net.minecraft.state.property.EnumProperty.
 *   MojMap names are net.minecraft.world.level.block.state.StateDefinition / properties.EnumProperty
 *   — в проекте используется Yarn (см. mappings "net.fabricmc:yarn"), поэтому импорты — net.minecraft.state.* .
 * - IIdProvider теперь extends StringIdentifiable (asString() -> getName()), чтобы удовлетворять
 *   границе EnumProperty<T extends Enum<T> & StringIdentifiable> в Yarn.
 */
public class BlockMultiID<T extends Enum<T> & IIdProvider> extends Block {

    protected final EnumProperty<T> typeProperty;

    public BlockMultiID(Settings settings, EnumProperty<T> typeProperty) {
        super(settings);
        this.typeProperty = typeProperty;
        // default state will be set после инициализации StateManager; если нужно — раскомментировано:
        // this.setDefaultState(this.getDefaultState().with(this.typeProperty, this.typeProperty.getValues().iterator().next()));
    }

    /**
     * Создает EnumProperty с именем "type" для заданного enum-класса.
     * Проверяет ограничение в 16 значений (как в оригинале — мета-значения).
     */
    public static <T extends Enum<T> & IIdProvider> EnumProperty<T> createTypeProperty(Class<T> typeClass) {
        EnumProperty<T> ret = EnumProperty.of("type", typeClass);
        if (ret.getValues().size() > 16) {
            throw new IllegalArgumentException("Too many values to fit in 16 meta values for " + typeClass);
        }
        return ret;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(typeProperty);
    }

    /**
     * Alias для MojMap названия — в Yarn метод называется appendProperties,
     * в MojMap — createBlockStateDefinition(StateDefinition.Builder).
     * Оставлен для совместимости с документацией/тулами, проверяющими наличие метода.
     */
    protected void createBlockStateDefinition(StateManager.Builder<Block, BlockState> builder) {
        appendProperties(builder);
    }

    public EnumProperty<T> getTypeProperty() {
        return typeProperty;
    }
}
