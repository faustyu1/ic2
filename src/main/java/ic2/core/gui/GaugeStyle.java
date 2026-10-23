package ic2.core.gui;

import net.minecraft.util.Identifier;

/**
 * Gauge geometry, ported from Gauge.GaugeStyle and EnergyGauge.EnergyGaugeStyle.
 *
 * <p>A gauge is drawn in two parts: an optional static background and the filled part, which grows
 * from the side the orientation names. Most styles live on the common atlas; the ones a machine
 * draws from its own background image carry their texture, and their background UV is simply the
 * spot the unfilled gauge occupies in that image.
 */
public record GaugeStyle(int u, int v, int width, int height, Orientation orientation,
                         int backgroundOffsetX, int backgroundOffsetY,
                         int backgroundWidth, int backgroundHeight,
                         int backgroundU, int backgroundV,
                         Identifier texture) {

    /** The direction a gauge fills towards. */
    public enum Orientation { UP, DOWN, LEFT, RIGHT }

    public boolean hasBackground() {
        return backgroundWidth > 0 && backgroundHeight > 0;
    }

    /** Style whose art is on the common atlas. */
    private static GaugeStyle common(int u, int v, int width, int height, Orientation orientation,
                                     int bgOffsetX, int bgOffsetY, int bgWidth, int bgHeight, int bgU, int bgV) {
        return new GaugeStyle(u, v, width, height, orientation,
                bgOffsetX, bgOffsetY, bgWidth, bgHeight, bgU, bgV, GuiTextures.COMMON);
    }

    /** Style drawn from a machine's own GUI image. */
    private static GaugeStyle custom(String texture, int u, int v, int width, int height, Orientation orientation,
                                     int bgOffsetX, int bgOffsetY, int bgWidth, int bgHeight, int bgU, int bgV) {
        return new GaugeStyle(u, v, width, height, orientation,
                bgOffsetX, bgOffsetY, bgWidth, bgHeight, bgU, bgV, GuiTextures.of("ic2:textures/gui/" + texture));
    }

    public static GaugeStyle of(String name) {
        return switch (name.toLowerCase()) {
            case "progressarrow" -> common(165, 16, 22, 15, Orientation.RIGHT, -5, 0, 32, 16, 160, 0);
            case "progressarrowmodern" -> common(86, 234, 16, 10, Orientation.RIGHT, 0, 0, 16, 10, 70, 234);
            case "progressarrowmodernreversed" -> common(70, 244, 16, 10, Orientation.LEFT, 0, 0, 16, 10, 86, 244);
            case "progresscrush" -> common(165, 52, 21, 11, Orientation.RIGHT, -5, -3, 32, 16, 160, 32);
            case "progresstriangle" -> common(165, 80, 22, 15, Orientation.RIGHT, -5, 0, 32, 16, 160, 64);
            case "progressdrop" -> common(165, 112, 22, 15, Orientation.RIGHT, -5, 0, 32, 16, 160, 96);
            case "progressrecycler" -> common(133, 80, 18, 15, Orientation.RIGHT, -5, 0, 32, 16, 128, 64);
            case "progressmetalformer" -> common(200, 19, 46, 9, Orientation.RIGHT, -8, -3, 64, 16, 192, 0);
            case "progresscentrifuge" -> common(252, 33, 3, 28, Orientation.UP, -1, -1, 5, 30, 246, 32);
            case "heatcentrifuge" -> common(225, 54, 20, 4, Orientation.RIGHT, -1, -1, 22, 6, 224, 47);
            case "progresswind" -> common(242, 91, 13, 13, Orientation.UP, 0, 0, 13, 13, 242, 63);
            case "fuel" -> common(112, 80, 13, 13, Orientation.UP, 0, 0, 16, 16, 96, 80);
            case "bucket" -> common(110, 111, 14, 16, Orientation.UP, 0, 0, 14, 16, 96, 111);
            case "progressorewasher" -> custom("GUIOreWashingPlant", 177, 118, 18, 18, Orientation.RIGHT,
                    -1, -1, 20, 19, 102, 38);
            case "progressblockcutter" -> custom("GUIBlockCutter", 176, 15, 46, 17, Orientation.RIGHT,
                    0, 0, 46, 17, 55, 33);
            case "progresslongarrow" -> custom("GUI_Canner_Classic", 176, 15, 34, 13, Orientation.RIGHT,
                    0, 0, 34, 13, 74, 36);
            case "progresscondenser" -> custom("GUICondenser", 1, 185, 82, 7, Orientation.RIGHT, 0, 0, 0, 0, 0, 0);
            case "progresscanner" -> custom("GUICanner", 233, 0, 23, 14, Orientation.RIGHT, 0, 0, 0, 0, 0, 0);
            case "progressfermenter" -> custom("GUIFermenter", 177, 1, 40, 7, Orientation.RIGHT, 0, 0, 0, 0, 0, 0);
            case "heatfermenter" -> custom("GUIFermenter", 177, 10, 40, 3, Orientation.RIGHT, 0, 0, 0, 0, 0, 0);
            case "heatnuclearreactor" -> custom("GUINuclearReactor", 0, 243, 100, 13, Orientation.RIGHT, 0, 0, 0, 0, 0, 0);
            case "progressbottler" -> custom("GUIBottler", 198, 0, 16, 13, Orientation.RIGHT, 0, 0, 0, 0, 0, 0);
            case "electrolyzer1" -> custom("GUIElectrolyzer", 57, 232, 12, 23, Orientation.DOWN, 0, 0, 0, 0, 0, 0);
            case "electrolyzer2" -> custom("GUIElectrolyzer", 1, 232, 54, 23, Orientation.DOWN, 0, 0, 0, 0, 0, 0);
            case "electrolyzer3" -> custom("GUIElectrolyzer", 41, 159, 54, 23, Orientation.DOWN, 0, 0, 0, 0, 0, 0);
            case "electrolyzer4" -> custom("GUIElectrolyzer", 1, 208, 96, 23, Orientation.DOWN, 0, 0, 0, 0, 0, 0);
            case "electrolyzer5" -> custom("GUIElectrolyzer", 1, 184, 96, 23, Orientation.DOWN, 0, 0, 0, 0, 0, 0);
            case "progressuuscanner" -> custom("GUIScanner", 176, 14, 66, 43, Orientation.RIGHT, 0, 0, 0, 0, 0, 0);
            case "heatsteamgenerator" -> custom("GUISteamGenerator", 177, 1, 7, 76, Orientation.UP, 0, 0, 0, 0, 0, 0);
            case "calcificationsteamgenerator" -> custom("GUISteamGenerator", 187, 1, 7, 58, Orientation.UP, 0, 0, 0, 0, 0, 0);
            default -> null;
        };
    }

    /**
     * The style as drawn while the gauge is active. Only the wind gauge has a separate lit
     * background; every other style looks the same either way.
     */
    public static GaugeStyle ofActive(String name) {
        if ("progresswind".equalsIgnoreCase(name)) {
            return common(242, 91, 13, 13, Orientation.UP, 0, 0, 13, 13, 242, 77);
        }
        return of(name);
    }

    public static GaugeStyle ofEnergy(String name) {
        return switch (name.toLowerCase()) {
            case "bar" -> common(132, 43, 24, 9, Orientation.RIGHT, -4, -11, 32, 32, 128, 0);
            case "stirlingbar" -> custom("GUIStirlingGenerator", 176, 15, 58, 14, Orientation.RIGHT,
                    0, 0, 58, 14, 59, 33);
            default -> common(116, 65, 7, 13, Orientation.UP, -4, -1, 16, 16, 96, 64);
        };
    }
}
