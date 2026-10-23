package ic2.core.crop;

import ic2.api.crops.CropCard;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.Identifier;

/**
 * Fabric port of ic2.core.crop.IC2CropCard - the base every crop IC2 ships uses.
 *
 * <p>It only fills in what all of them share: the owner, the translation key and the textures,
 * which are named after the crop and its size.
 */
public abstract class IC2CropCard extends CropCard {
    @Override
    public String getOwner() {
        return "ic2";
    }

    @Override
    public String getUnlocalizedName() {
        return "ic2.crop." + getId();
    }

    @Override
    public String getDiscoveredBy() {
        return "IC2 Team";
    }

    @Override
    public List<Identifier> getTexturesLocation() {
        List<Identifier> ret = new ArrayList<>(getMaxSize());
        for (int size = 1; size <= getMaxSize(); size++) {
            ret.add(Identifier.of("ic2", "blocks/crop/" + getId() + "_" + size));
        }
        return ret;
    }
}
