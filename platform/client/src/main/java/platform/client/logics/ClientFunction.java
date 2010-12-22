package platform.client.logics;

import platform.client.descriptor.editor.FunctionEditor;
import platform.client.serialization.ClientSerializationPool;
import platform.base.context.ApplicationContext;
import platform.interop.form.layout.AbstractFunction;

import javax.swing.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ClientFunction extends ClientComponent implements AbstractFunction<ClientContainer, ClientComponent> {

    public String caption;
    public String type;

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public void setType(String type) {
        this.type = type;
    }

    public ClientFunction() {
    }

    public ClientFunction(ApplicationContext context) {
        super(context);
    }

    @Override
    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        super.customSerialize(pool, outStream, serializationType);

        pool.writeString(outStream, caption);
        pool.writeString(outStream, type);
    }

    @Override
    public void customDeserialize(ClientSerializationPool pool, DataInputStream inStream) throws IOException {
        super.customDeserialize(pool, inStream);

        caption = pool.readString(inStream);
        type = pool.readString(inStream);
    }

    @Override
    public String getCaption() {
        return caption;
    }

    @Override
    public String toString() {
        return getCaption();
    }

    @Override
    public JComponent getPropertiesEditor() {
        return new FunctionEditor(this);
    }

    @Override
    public String getCodeConstructor(String name) {
        return "FunctionView " + name + " = design.createFunction(\"" + caption + "\");\n" +
                "\t   design.set" + type + "Function(" + name + ");";
    }
}
