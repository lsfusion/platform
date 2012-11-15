package platform.client.logics;

import platform.base.context.ApplicationContext;
import platform.client.ClientResourceBundle;
import platform.client.descriptor.FormDescriptor;
import platform.client.descriptor.editor.FilterEditor;
import platform.client.serialization.ClientSerializationPool;
import platform.interop.form.layout.SimplexConstraints;

import javax.swing.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ClientFilter extends ClientComponent {
    public boolean visible = true;

    public ClientFilter() {
    }

    public ClientFilter(ApplicationContext context) {
        super(context);
    }

    @Override
    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        super.customSerialize(pool, outStream, serializationType);

        outStream.writeBoolean(visible);
    }

    @Override
    public void customDeserialize(ClientSerializationPool pool, DataInputStream inStream) throws IOException {
        super.customDeserialize(pool, inStream);

        visible = inStream.readBoolean();
    }

    public boolean getVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
        updateDependency(this, "visible");
    }

    @Override
    public SimplexConstraints<ClientComponent> getDefaultConstraints() {
        return SimplexConstraints.getShowTypeDefaultConstraints(super.getDefaultConstraints());
    }

    @Override
    public String getCaption() {
        return ClientResourceBundle.getString("logics.filter");
    }

    @Override
    public String toString() {
        return getCaption() + "[sid:" + getSID() + "]";
    }

    @Override
    public JComponent getPropertiesEditor() {
        return new FilterEditor(this);
    }

    public String getCodeClass() {
        return "Filter";
    }

    @Override
    public String getCodeConstructor() {
        return "design.createFilter()";
    }

    @Override
    public String getVariableName(FormDescriptor form) {
        return getSID() + "Filter";
    }

    @Override
    public boolean shouldBeDeclared() {
        return true;
    }
}
