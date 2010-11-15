package platform.client.logics;

import platform.client.descriptor.editor.FunctionEditor;
import platform.client.serialization.ClientSerializationPool;
import platform.interop.context.ApplicationContext;
import platform.interop.form.layout.AbstractFunction;

import javax.swing.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ClientFunction extends ClientComponent implements AbstractFunction<ClientContainer, ClientComponent> {

    public String caption;

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public ClientFunction() {
    }

    public ClientFunction(int ID, ApplicationContext context) {
        super(ID, context);
    }

    @Override
    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        super.customSerialize(pool, outStream, serializationType);

        pool.writeString(outStream, caption);
    }

    @Override
    public void customDeserialize(ClientSerializationPool pool, DataInputStream inStream) throws IOException {
        super.customDeserialize(pool, inStream);

        caption = pool.readString(inStream);
    }

    @Override
    public String toString() {
        return caption;
    }

    @Override
    public JComponent getPropertiesEditor() {
        return new FunctionEditor(this);
    }
}
