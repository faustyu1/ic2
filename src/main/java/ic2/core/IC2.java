package ic2.core;

import ic2.core.util.Log;
import ic2.core.util.LogCategory;
import ic2.core.util.PriorityExecutor;

public class IC2 {
    public static final Log log = Log.INSTANCE;
    public static Platform platform = new Platform();
    public static ic2.core.profile.Version version = new ic2.core.profile.Version();
    private static IC2 instance = new IC2();
    public static TickHandler tickHandler;

    /**
     * The keys every player holds, as their client last reported them. The client entrypoint
     * swaps this for {@code ic2.core.client.KeyboardClient}, which also reads and sends them.
     */
    public static ic2.core.util.Keyboard keyboard = new ic2.core.util.Keyboard();
    public PriorityExecutor threadPool = new PriorityExecutor();
    public static java.util.Random random = new java.util.Random();
    /** The running server, for lookups that need its registries or recipes outside a world; null without one. */
    public static net.minecraft.server.MinecraftServer server;

    public static IC2 getInstance() { return instance; }
    public static IC2 getInstanceStatic() { return instance; }

    public enum Type { CLIENT, SERVER }
    public static Type getType() { return Type.SERVER; }
}
