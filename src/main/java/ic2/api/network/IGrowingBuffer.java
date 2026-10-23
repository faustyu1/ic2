package ic2.api.network;

import java.io.DataInput;
import java.io.DataOutput;

public interface IGrowingBuffer extends DataInput, DataOutput {
    void writeVarInt(int value);
    void writeString(String value);
    int readVarInt();
    String readString();
}
