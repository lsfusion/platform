package platform.client.logics;

import platform.client.descriptor.editor.ComponentEditor;
import platform.client.serialization.ClientSerializationPool;

import javax.swing.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ClientClassChooser extends ClientComponent {

    public ClientClassChooser() {
        
    }

    public ClientObject object;

    @Override
    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        super.customSerialize(pool, outStream, serializationType);

        pool.serializeObject(outStream, object);
    }

    @Override
    public void customDeserialize(ClientSerializationPool pool, DataInputStream inStream) throws IOException {
        super.customDeserialize(pool, inStream);

        object = pool.deserializeObject(inStream);
    }

    @Override
    public String toString() {
        return "Дерево классов (" + object.toString() + ")";
    }

    @Override
    public JComponent getPropertiesEditor() {
        return new ComponentEditor("Компонент выбора класса", this);
    }
}
