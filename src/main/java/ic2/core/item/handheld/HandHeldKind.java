package ic2.core.item.handheld;

import java.util.function.Predicate;

import ic2.api.item.ItemWrapper;
import ic2.core.item.ItemRadioactive;
import ic2.core.item.reactor.ItemReactorUranium;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.StringIdentifiable;

/**
 * The hand-held containers IC2 has: what fits in them, how many slots they have and where those
 * slots sit on their background.
 *
 * <p>1.12.2 gave each one its own {@code HandHeldInventory}, {@code Container} and {@code Gui}
 * subclass that differed in exactly these three things, so here they are one enum and one set of
 * classes.
 */
public enum HandHeldKind implements StringIdentifiable {
    /**
     * The toolbox: nine slots in a row, holding whatever says it is boxable.
     */
    TOOLBOX("tool_box", 9, 1, 8, 41, Identifier.of("ic2", "textures/gui/guitoolbox.png"),
            ItemWrapper::canBeStoredInToolbox),

    /**
     * The containment box: twelve slots in three rows of four, holding nuclear material only -
     * which is also what makes it a containment box, since what sits inside a stack does not tick
     * and so does not irradiate anyone.
     */
    CONTAINMENT_BOX("containment_box", 12, 4, 53, 19, Identifier.of("ic2", "textures/gui/guicontainmentbox.png"),
            HandHeldKind::isRadioactive);

    private final String id;
    private final int size;
    private final int columns;
    private final int originX;
    private final int originY;
    private final Identifier texture;
    private final Predicate<ItemStack> filter;

    HandHeldKind(String id, int size, int columns, int originX, int originY, Identifier texture,
                 Predicate<ItemStack> filter) {
        this.id = id;
        this.size = size;
        this.columns = columns;
        this.originX = originX;
        this.originY = originY;
        this.texture = texture;
        this.filter = filter;
    }

    public String getId() {
        return id;
    }

    public int getSize() {
        return size;
    }

    public int getColumns() {
        return columns;
    }

    public int getOriginX() {
        return originX;
    }

    public int getOriginY() {
        return originY;
    }

    public Identifier getTexture() {
        return texture;
    }

    public boolean accepts(ItemStack stack) {
        return !stack.isEmpty() && filter.test(stack);
    }

    @Override
    public String asString() {
        return id;
    }

    private static boolean isRadioactive(ItemStack stack) {
        return stack.getItem() instanceof ItemRadioactive || stack.getItem() instanceof ItemReactorUranium;
    }
}
