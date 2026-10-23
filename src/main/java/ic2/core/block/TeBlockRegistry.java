package ic2.core.block;

import ic2.core.block.state.IIdProvider;
import ic2.core.ref.TeBlock;
import java.util.*;
import net.minecraft.block.MapColor;
import net.minecraft.util.Identifier;

/**
 * Fabric port of TeBlockRegistry.
 * Original used Forge GameRegistry and TeBlockFinalCallEvent.
 * Fabric version holds the same maps but delegates actual block registration to IC2Mod via Registry.register.
 */
public final class TeBlockRegistry {

    private static final Map<String, IIdProvider> NAME_REGISTRY = new HashMap<>();
    private static final Map<Class<? extends TileEntityBlock>, IIdProvider> CLASS_REGISTRY = new IdentityHashMap<>();
    private static final Map<String, Class<? extends TileEntityBlock>> OLD_REGISTRY = new HashMap<>();
    private static final Map<Identifier, TeBlockInfo<?>> RESOURCE_REGISTRY = new HashMap<>(5);
    private static boolean blocksBuilt = false;

    public static <E extends Enum<E> & IIdProvider> void add(E block) {
        if (!canBuildBlocks()) {
            throw new IllegalStateException("Cannot register additional ITeBlocks once block map built!");
        }
        if (block == null) throw new NullPointerException("Cannot register null ITeBlock!");
        // For Fabric, we use a single TeBlock enum; use its identifier as key
        Identifier loc = Identifier.of("ic2", "te");
        TeBlockInfo<E> info;
        if (!RESOURCE_REGISTRY.containsKey(loc)) {
            RESOURCE_REGISTRY.put(loc, info = new TeBlockInfo<>(block));
        } else {
            @SuppressWarnings("unchecked")
            TeBlockInfo<E> existing = (TeBlockInfo<E>) RESOURCE_REGISTRY.get(loc);
            info = existing;
        }
        info.register(block);
    }

    public static <E extends Enum<E> & IIdProvider> void addAll(Class<E> enumClass, Identifier identifier) {
        if (!canBuildBlocks()) {
            throw new IllegalStateException("Cannot register additional ITeBlocks once block map built!");
        }
        if (EnumSet.allOf(enumClass).isEmpty()) throw new IllegalArgumentException("Cannot register empty enum!");
        if (identifier == null) throw new NullPointerException("Cannot register a null identifier!");
        if (RESOURCE_REGISTRY.containsKey(identifier)) throw new IllegalArgumentException("Already registered an enum for " + identifier);
        TeBlockInfo<E> info = new TeBlockInfo<>(enumClass);
        RESOURCE_REGISTRY.put(identifier, info);
        info.registerAll(enumClass);
    }

    public static void buildBlocks() {
        if (!canBuildBlocks()) throw new IllegalStateException("Cannot build blocks twice!");
        // In original, this posted TeBlockFinalCallEvent and created BlockTileEntity via BlockName.
        // For Fabric, actual block creation is done in IC2Mod; we just mark as built.
        blocksBuilt = true;
    }

    public static boolean canBuildBlocks() {
        return !blocksBuilt;
    }

    public static IIdProvider get(String name) {
        IIdProvider ret = NAME_REGISTRY.get(name);
        return ret != null ? ret : null;
    }

    public static Class<? extends TileEntityBlock> getOld(String name) {
        return OLD_REGISTRY.get(name);
    }

    public static IIdProvider get(Identifier identifier, int id) {
        if (id >= 0 && RESOURCE_REGISTRY.containsKey(identifier)) {
            List<IIdProvider> items = RESOURCE_REGISTRY.get(identifier).getIdMap();
            if (id < items.size()) return items.get(id);
        }
        return null;
    }

    public static IIdProvider get(Class<? extends TileEntityBlock> cls) {
        return CLASS_REGISTRY.get(cls);
    }

    public static BlockTileEntity get(Identifier identifier) {
        return RESOURCE_REGISTRY.containsKey(identifier) ? RESOURCE_REGISTRY.get(identifier).getBlock() : null;
    }

    public static Iterable<Map.Entry<Identifier, Set<? extends IIdProvider>>> getAll() {
        List<Map.Entry<Identifier, Set<? extends IIdProvider>>> list = new ArrayList<>();
        for (Map.Entry<Identifier, TeBlockInfo<?>> e : RESOURCE_REGISTRY.entrySet()) {
            list.add(new AbstractMap.SimpleImmutableEntry<>(e.getKey(), e.getValue().getTeBlocks()));
        }
        return list;
    }

    public static Collection<BlockTileEntity> getAllBlocks() {
        List<BlockTileEntity> list = new ArrayList<>();
        for (TeBlockInfo<?> info : RESOURCE_REGISTRY.values()) {
            if (info.getBlock() != null) list.add(info.getBlock());
        }
        return list;
    }

    public static Set<? extends IIdProvider> getAll(Identifier identifier) {
        return RESOURCE_REGISTRY.containsKey(identifier) ? RESOURCE_REGISTRY.get(identifier).getTeBlocks() : Collections.emptySet();
    }

    static TeBlockInfo<?> getInfo(Identifier identifier) {
        return RESOURCE_REGISTRY.get(identifier);
    }

    static List<IIdProvider> getItems(Identifier identifier) {
        return RESOURCE_REGISTRY.containsKey(identifier) ? RESOURCE_REGISTRY.get(identifier).getIdMap() : Collections.emptyList();
    }

    private static void addName(IIdProvider block) {
        NAME_REGISTRY.put(block.getName(), block);
    }

    private static void addClass(IIdProvider block) {
        // Try to find TileEntity class via TeBlock if applicable
        if (block instanceof TeBlock) {
            Class<? extends TileEntityBlock> cls = ((TeBlock) block).getTeClass();
            if (cls != null) CLASS_REGISTRY.put(cls, block);
        }
    }

    private TeBlockRegistry() {}

    public static class TeBlockInfo<E extends Enum<E> & IIdProvider> {
        private BlockTileEntity block;
        private MapColor defaultMaterial = MapColor.STONE_GRAY;
        private final Set<E> teBlocks;
        private final List<IIdProvider> idMap = new ArrayList<>();

        TeBlockInfo(E universe) {
            this((Class<E>) universe.getClass());
        }

        TeBlockInfo(Class<E> universe) {
            this.teBlocks = EnumSet.noneOf(universe);
        }

        void register(E block) {
            if (!teBlocks.add(block)) throw new IllegalStateException("ITeBlock already registered!");
            TeBlockRegistry.addName(block);
            TeBlockRegistry.addClass(block);
            int id = block.getId();
            if (id > -1) {
                while (idMap.size() < id) idMap.add(null);
                if (idMap.size() == id) {
                    idMap.add(block);
                } else {
                    if (idMap.get(id) != null) throw new IllegalStateException("The id " + id + " for " + block + " is already in use by " + idMap.get(id));
                    idMap.set(id, block);
                }
            }
        }

        void registerAll(Class<E> universe) {
            for (E block : EnumSet.allOf(universe)) register(block);
        }

        void setBlock(BlockTileEntity block) {
            if (hasBlock()) throw new IllegalStateException("Already has block set (" + this.block + ") when adding " + block);
            this.block = block;
        }

        public boolean hasBlock() { return block != null; }
        public BlockTileEntity getBlock() { return block; }
        public MapColor getDefaultMaterial() { return defaultMaterial; }
        public void setDefaultMaterial(MapColor mat) { this.defaultMaterial = mat; }
        public Set<? extends IIdProvider> getTeBlocks() { return Collections.unmodifiableSet(teBlocks); }
        public List<IIdProvider> getIdMap() { return Collections.unmodifiableList(idMap); }
        public boolean hasSpecialModels() { return false; }
    }
}
