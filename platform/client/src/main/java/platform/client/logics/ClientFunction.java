package platform.client.logics;

import platform.client.descriptor.FormDescriptor;
import platform.client.descriptor.editor.FunctionEditor;
import platform.client.serialization.ClientSerializationPool;
import platform.base.context.ApplicationContext;
import platform.interop.form.layout.AbstractFunction;
import platform.interop.form.layout.SimplexConstraints;

import javax.swing.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ClientFunction extends ClientComponent implements AbstractFunction<ClientContainer, ClientComponent> {

    public String caption;
    public String type;
    public boolean visible = true;

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public void setType(String type) {
        this.type = type;
    }

    @SuppressWarnings({"UnusedDeclaration"})
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
        outStream.writeBoolean(visible);
    }

    @Override
    public void customDeserialize(ClientSerializationPool pool, DataInputStream inStream) throws IOException {
        super.customDeserialize(pool, inStream);

        caption = pool.readString(inStream);
        type = pool.readString(inStream);
        visible = inStream.readBoolean();
    }

    @Override
    public String getCaption() {
        return caption;
    }

    @Override
    public String toString() {
        return getCaption() + "[sid:" + getSID() + "]";
    }

    @Override
    public JComponent getPropertiesEditor() {
        return new FunctionEditor(this);
    }

    public String getCodeClass() {
        return "FunctionView";
    }

    @Override
    public String getCodeConstructor() {
        return "design.create" + type + "Function(\"" + caption + "\")";
    }

    @Override
    public String getVariableName(FormDescriptor form) {
        return type.toLowerCase() + getCodeClass();
    }

    @Override
    public SimplexConstraints<ClientComponent> getDefaultConstraints() {
        return SimplexConstraints.getFunctionDefaultConstraints(super.getDefaultConstraints());
    }
}
