package lsfusion.client.logics;

import lsfusion.base.context.ApplicationContext;
import lsfusion.client.ClientResourceBundle;
import lsfusion.client.descriptor.editor.FilterEditor;
import lsfusion.client.serialization.ClientSerializationPool;
import lsfusion.interop.form.layout.SimplexConstraints;

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

    @Override
    public boolean shouldBeDeclared() {
        return true;
    }
}
