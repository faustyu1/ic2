package ic2.core.crop;

import ic2.api.crops.CropCard;
import net.minecraft.util.Identifier;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Simple singleton registry for CropCards on Fabric.
 * Mirrors the role of {@code ic2.api.crops.Crops} but without Forge event bus.
 * Mods call {@link #register(CropCard)} during init.
 */
public final class CropManager {

    private static final CropManager INSTANCE = new CropManager();

    private final Map<Identifier, CropCard> crops = new LinkedHashMap<>();

    private CropManager() {}

    public static CropManager getInstance() { return INSTANCE; }

    /** Register via owner's id pair (id = owner:id). */
    public void register(CropCard card) {
        if (card == null) throw new NullPointerException("card");
        Identifier key = Identifier.of(card.getOwner(), card.getId());
        register(key, card);
    }

    public void register(Identifier key, CropCard card) {
        if (key == null || card == null) throw new NullPointerException();
        crops.put(key, card);
    }

    public CropCard getCrop(Identifier key) {
        return crops.get(key);
    }

    public CropCard getCrop(String owner, String id) {
        return getCrop(Identifier.of(owner, id));
    }

    public Collection<CropCard> getCrops() {
        return Collections.unmodifiableCollection(crops.values());
    }

    public Map<Identifier, CropCard> getMap() {
        return Collections.unmodifiableMap(crops);
    }

    public boolean contains(Identifier key) { return crops.containsKey(key); }

    public int size() { return crops.size(); }

    public void clear() { crops.clear(); }
}
