package lsfusion.interop.navigator;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Objects;

public class NavigatorScheduler implements Serializable {
    public int period;
    public boolean fixed;

    public NavigatorScheduler(int period, boolean fixed) {
        this.period = period;
        this.fixed = fixed;
    }

    public void serialize(DataOutputStream outStream) throws IOException {
        outStream.writeInt(period);
        outStream.writeBoolean(fixed);
    }

    public static NavigatorScheduler deserialize(DataInputStream inStream) throws IOException {
        return new NavigatorScheduler(inStream.readInt(), inStream.readBoolean());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NavigatorScheduler that = (NavigatorScheduler) o;
        return period == that.period && fixed == that.fixed;
    }

    @Override
    public int hashCode() {
        return Objects.hash(period, fixed);
    }
}