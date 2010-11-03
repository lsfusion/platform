package platform.client.logics;

import platform.client.descriptor.editor.ComponentEditor;
import platform.client.descriptor.increment.IncrementDependency;
import platform.client.serialization.ClientSerializationPool;

import javax.swing.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ClientClassChooser extends ClientComponent {

    public ClientClassChooser() {
        
    }

    public ClientObject object;
    public boolean show = true;

    @Override
    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        super.customSerialize(pool, outStream, serializationType);

        pool.serializeObject(outStream, object);
        outStream.writeBoolean(show);
    }

    @Override
    public void customDeserialize(ClientSerializationPool pool, DataInputStream inStream) throws IOException {
        super.customDeserialize(pool, inStream);

        object = pool.deserializeObject(inStream);
        show = inStream.readBoolean();

    }

    public boolean getShow() {
        return show;
    }

    public void setShow(boolean show) {
        this.show = show;
        IncrementDependency.update(this, "show");
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
