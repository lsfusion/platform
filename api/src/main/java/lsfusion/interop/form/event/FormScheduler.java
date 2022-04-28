package lsfusion.interop.form.event;

import java.io.*;
import java.util.Objects;

public class FormScheduler implements Serializable {
    public int period;
    public boolean fixed;

    public FormScheduler(int period, boolean fixed) {
        this.period = period;
        this.fixed = fixed;
    }

    public void serialize(DataOutputStream outStream) throws IOException {
        outStream.writeInt(period);
        outStream.writeBoolean(fixed);
    }

    public static FormScheduler deserialize(DataInputStream inStream) throws IOException {
        return new FormScheduler(inStream.readInt(), inStream.readBoolean());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FormScheduler that = (FormScheduler) o;
        return period == that.period && fixed == that.fixed;
    }

    @Override
    public int hashCode() {
        return Objects.hash(period, fixed);
    }
}