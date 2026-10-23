package ic2.core.item;

import net.minecraft.item.ItemStack;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ItemCrafting extends ItemIC2 {

    public enum Type {
        CIRCUIT("circuit"),
        ADV_CIRCUIT("adv_circuit"),
        CARBON_PLATE("carbon_plate"),
        ADV_ALLOY("adv_alloy");

        private final String id;

        Type(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }
    }

    private static final Map<String, Type> BY_ID;
    static {
        Map<String, Type> m = new HashMap<>();
        for (Type t : Type.values()) m.put(t.getId(), t);
        BY_ID = Collections.unmodifiableMap(m);
    }

    public final Type type;

    public ItemCrafting(String name) {
        super(name);
        this.type = BY_ID.get(name);
    }

    public ItemCrafting(Type type) {
        super(type.getId());
        this.type = type;
    }

    public static ItemStack get(String type) {
        return ItemStack.EMPTY;
    }

    public static Type byId(String id) {
        return BY_ID.get(id);
    }

    public static Map<String, Type> getMap() {
        return BY_ID;
    }
}
