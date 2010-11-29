package platform.client.logics;

import platform.client.descriptor.editor.ComponentEditor;
import platform.client.serialization.ClientSerializationPool;
import platform.base.context.ApplicationContext;

import javax.swing.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ClientShowType extends ClientComponent {

    public ClientShowType() {
    }

    public ClientGroupObject groupObject;

    public ClientShowType(ApplicationContext context) {
        super(context);
    }

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
    public String getCaption() {
        return "Вид";
    }

    @Override
    public String toString() {
        return getCaption() + " (" + groupObject.toString() + ")";
    }

    @Override
    public JComponent getPropertiesEditor() {
        return new ComponentEditor(this);
    }
}
