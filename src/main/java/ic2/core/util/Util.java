package ic2.core.util;

import ic2.api.energy.tile.IEnergyTile;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class Util {
    public static boolean hasAssertions() { return false; }
    public static boolean inDev() { return false; }
    public static String toString(IEnergyTile tile, World world, BlockPos pos) {
        return tile + "@" + pos;
    }
    public static String formatPosition(BlockPos pos) { return pos.toString(); }
    public static String formatPosition(World world, BlockPos pos) { return pos.toString(); }
    public static boolean checkInterfaces(Class<?> clazz) { return true; }
    public static double lerp(double a, double b, double t) { return a + (b - a) * t; }
    public static double square(double v) { return v * v; }
    public static String toString(Object obj) { return String.valueOf(obj); }

    /** The SI prefixes {@link #toSiString} picks from, by how many groups of three digits. */
    private static final String[] BIG = {"", "k", "M", "G", "T", "P", "E", "Z", "Y"};
    private static final String[] SMALL = {"", "m", "\u00b5", "n", "p", "f", "a", "z", "y"};

    /**
     * A number with an SI prefix and {@code digits} significant digits, the way the EU reader and
     * the machine screens show energy: {@code 2.05k}, {@code 512}, {@code 30.0m}.
     *
     * <p>Port of {@code ic2.core.util.Util.toSiString}. The trailing space the original left on
     * the special cases is kept, because its callers glue the unit straight onto the result.
     */
    public static String toSiString(double value, int digits) {
        if (value == 0.0) return "0 ";
        if (Double.isNaN(value)) return "NaN ";

        String sign = "";
        if (value < 0.0) {
            sign = "-";
            value = -value;
        }
        if (Double.isInfinite(value)) return sign + "\u221e ";

        double log = Math.log10(value);
        double mul;
        String si;
        if (log >= 0.0) {
            int reduce = (int) Math.floor(log / 3.0);
            mul = 1.0 / Math.pow(10.0, reduce * 3);
            si = reduce < BIG.length ? BIG[reduce] : "E" + reduce * 3;
        } else {
            int expand = (int) Math.ceil(-log / 3.0);
            mul = Math.pow(10.0, expand * 3);
            si = expand < SMALL.length ? SMALL[expand] : "E-" + expand * 3;
        }

        value *= mul;
        int whole = (int) Math.floor(value);
        double fraction = value - whole;

        int wholeDigits = whole > 0 ? 1 + (int) Math.floor(Math.log10(whole)) : 1;
        double scale = Math.pow(10.0, digits - wholeDigits);
        int fractionDigits = (int) Math.round(fraction * scale);
        if (fractionDigits >= scale) {
            // rounding up the fraction carried into the whole part
            whole++;
            fractionDigits -= (int) scale;
            wholeDigits = whole > 0 ? 1 + (int) Math.floor(Math.log10(whole)) : 1;
        }

        StringBuilder ret = new StringBuilder(sign).append(whole);
        if (digits > wholeDigits && fractionDigits != 0) {
            ret.append(String.format(".%0" + (digits - wholeDigits) + "d", fractionDigits));
        }
        return ret.append(si).toString();
    }
}
