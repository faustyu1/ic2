package ic2.core.gui;

import java.util.Locale;
import net.minecraft.util.Identifier;

/** Resolves the texture names the guidef files use. */
public final class GuiTextures {
    /** Shared IC2 GUI atlas holding the frame, slots and gauges. */
    public static final Identifier COMMON = Identifier.of("ic2", "textures/gui/common.png");
    /** The atlas is authored at 256x256; guidef {@code basesize} may override it per image. */
    public static final int DEFAULT_SIZE = 256;

    private GuiTextures() {}

    /**
     * Resolves a guidef {@code src}, which may omit the namespace and the file extension.
     *
     * <p>The 1.12.2 files still spell texture names in mixed case ({@code GUITermalCentrifuge});
     * identifiers have to be lower case since 1.13 and the extracted assets were renamed
     * accordingly, so the path is folded here rather than editing the shipped guidef files.
     */
    public static Identifier of(String src) {
        String path = src.contains(":") ? src.substring(src.indexOf(':') + 1) : src;
        String namespace = src.contains(":") ? src.substring(0, src.indexOf(':')) : "ic2";
        path = path.toLowerCase(Locale.ROOT);
        if (!path.endsWith(".png")) {
            path = path + ".png";
        }
        return Identifier.of(namespace, path);
    }
}
