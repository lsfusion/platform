package lsfusion.interop.form.event;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Objects;

public class FormEventClose extends FormEvent {
    public boolean ok;

    public FormEventClose(boolean ok) {
        this.ok = ok;
    }

    @Override
    public int getType() {
        return 1;
    }

    @Override
    public Object getFireEvent() {
        return ok ? FormEventType.QUERYOK : FormEventType.QUERYCLOSE;
    }

    public void serialize(DataOutputStream outStream) throws IOException {
        super.serialize(outStream);
        outStream.writeBoolean(ok);
    }

    public static FormEventClose deserialize(DataInputStream inStream) throws IOException {
        return new FormEventClose(inStream.readBoolean());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FormEventClose that = (FormEventClose) o;
        return ok == that.ok;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ok);
    }
}