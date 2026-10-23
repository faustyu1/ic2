package ic2.api.info;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffect;

public class Info {
    public static IInfoProvider itemInfo;
    public static Object ic2ModInstance;
    public static DamageSource DMG_ELECTRIC;
    public static DamageSource DMG_NUKE_EXPLOSION;
    public static DamageSource DMG_RADIATION;
    public static StatusEffect POTION_RADIATION;
    public static String MOD_ID = "ic2";

    private static Boolean ic2Available = null;

    public static boolean isIc2Available() {
        if (ic2Available != null) return ic2Available;
        boolean loaded = FabricLoader.getInstance().isModLoaded(MOD_ID);
        ic2Available = loaded;
        return loaded;
    }
}
