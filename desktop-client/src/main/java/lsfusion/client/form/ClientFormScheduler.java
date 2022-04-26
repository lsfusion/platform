package lsfusion.client.form;

import java.io.DataInputStream;
import java.io.IOException;

public class ClientFormScheduler {
    public int period;
    public boolean fixed;

    public ClientFormScheduler(int period, boolean fixed) {
        this.period = period;
        this.fixed = fixed;
    }

    public static ClientFormScheduler deserialize(DataInputStream inStream) throws IOException {
        return new ClientFormScheduler(inStream.readInt(), inStream.readBoolean());
    }
}