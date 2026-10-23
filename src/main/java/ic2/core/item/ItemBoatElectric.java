package ic2.core.item;

/**
 * The electric boat item. Like the boat it does not burn: dropped into lava or fire it stays,
 * which 1.12.2 did with a fireproof item entity.
 */
public class ItemBoatElectric extends ItemIC2Boat {
    public ItemBoatElectric(String name) {
        super(name, new Settings().fireproof());
    }
}
