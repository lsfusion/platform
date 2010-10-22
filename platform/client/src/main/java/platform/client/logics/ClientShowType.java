package platform.client.logics;

import platform.client.descriptor.editor.ComponentEditor;
import platform.client.serialization.ClientSerializationPool;

import javax.swing.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ClientShowType extends ClientComponent {

    public ClientShowType() {

    }

    public ClientGroupObject groupObject;

    @Override
    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        super.customSerialize(pool, outStream, serializationType);

        pool.serializeObject(outStream, groupObject);
    }

    @Override
    public void customDeserialize(ClientSerializationPool pool, DataInputStream inStream) throws IOException {
        super.customDeserialize(pool, inStream);

        groupObject = pool.deserializeObject(inStream);
    }

    @Override
    public String toString() {
        return "Вид (" + groupObject.toString() + ")";
    }

    public JComponent getPropertiesEditor() {
        return new ComponentEditor("Компонент изменения вида", this);
    }
}
