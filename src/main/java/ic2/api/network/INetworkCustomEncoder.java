package ic2.api.network;

import java.io.IOException;

public interface INetworkCustomEncoder {
    void encode(IGrowingBuffer buffer, Object value) throws IOException;
    Object decode(IGrowingBuffer buffer) throws IOException;
    boolean isThreadSafe();
}
