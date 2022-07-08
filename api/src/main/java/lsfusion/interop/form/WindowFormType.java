package lsfusion.interop.form;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public interface WindowFormType {

    default Integer getInContainerId() {
        return null;
    }

    default boolean isModal() {
        return false;
    }

    default boolean isEditing() {
        return false;
    }

    byte getType();

    void serialize(DataOutputStream outStream) throws IOException;

    default void serializeType(DataOutputStream outStream) throws IOException {
        outStream.writeByte(getType());
    }

    static WindowFormType deserialize(DataInputStream inStream) throws IOException {
        int type = inStream.readByte();
        if (type == 0) {
            return ContainerWindowFormType.deserialize(inStream);
        } else {
            return ModalityWindowFormType.deserialize(type);
        }
    }
}
