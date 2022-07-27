package lsfusion.interop.form.event;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;

public abstract class FormEvent implements Serializable {

    public abstract int getType();

    public abstract Object getFireEvent();

    public void serialize(DataOutputStream outStream) throws IOException {
        outStream.writeInt(getType());
    }

    public static FormEvent deserialize(DataInputStream inStream) throws IOException {
        int type = inStream.readInt();
        if(type == 0) {
            return FormScheduler.deserialize(inStream);
        } else if(type == 1) {
            return FormEventClose.deserialize(inStream);
        } else {
            throw new UnsupportedOperationException("Unsupported FormEvent " + type);
        }
    }
}