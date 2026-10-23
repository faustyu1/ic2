package ic2.core.init;

/**
 * Translation keys as the port's language files spell them.
 *
 * <p>The 1.12.2 keys carried an {@code ic2.} prefix ({@code ic2.crop.wheat}); the converted
 * language files dropped it ({@code crop.wheat}), while the API still hands out the prefixed form
 * through {@code CropCard.getUnlocalizedName()} and {@code getSeedType()}, which addons rely on.
 */
public final class Localization {
    private Localization() {}

    /** The key the language files hold for an IC2 translation key. */
    public static String key(String unlocalized) {
        return unlocalized != null && unlocalized.startsWith("ic2.") ? unlocalized.substring(4) : unlocalized;
    }
}
