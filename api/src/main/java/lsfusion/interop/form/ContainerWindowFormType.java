package lsfusion.interop.form;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ContainerWindowFormType implements WindowFormType {

    public Integer inContainerId;

    public ContainerWindowFormType(Integer inContainerId) {
        this.inContainerId = inContainerId;
    }

    @Override
    public Integer getInContainerId() {
        return inContainerId;
    }

    @Override
    public byte getType() {
        return 0;
    }

    @Override
    public void serialize(DataOutputStream outStream) throws IOException {
        serializeType(outStream);
        outStream.writeInt(inContainerId);
    }

    public static ContainerWindowFormType deserialize(DataInputStream inStream) throws IOException {
        return new ContainerWindowFormType(inStream.readInt());
    }
}