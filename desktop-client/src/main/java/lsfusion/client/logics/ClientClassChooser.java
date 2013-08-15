package lsfusion.client.logics;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.descriptor.editor.ComponentEditor;
import lsfusion.base.context.ApplicationContext;
import lsfusion.client.serialization.ClientSerializationPool;
import lsfusion.interop.form.layout.SimplexConstraints;

import javax.swing.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ClientClassChooser extends ClientComponent {

    public ClientClassChooser() {
    }

    public ClientObject object;
    public boolean visible = true;

    public ClientClassChooser(int ID, ApplicationContext context) {
        super(ID, context);
    }

    @Override
    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        super.customSerialize(pool, outStream, serializationType);

        pool.serializeObject(outStream, object);
        outStream.writeBoolean(visible);
    }

    @Override
    public void customDeserialize(ClientSerializationPool pool, DataInputStream inStream) throws IOException {
        super.customDeserialize(pool, inStream);

        object = pool.deserializeObject(inStream);
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
    public String getCaption() {
        return ClientResourceBundle.getString("logics.classtree");
    }

    @Override
    public String toString() {
        return getCaption() + " (" + object.toString() + ")" + "[sid:" + getSID() + "]";
    }

    @Override
    public JComponent getPropertiesEditor() {
        return new ComponentEditor(this);
    }

    @Override
    public SimplexConstraints<ClientComponent> getDefaultConstraints() {
        return SimplexConstraints.getClassChooserDefaultConstraints(super.getDefaultConstraints());
    }

    @Override
    public boolean shouldBeDeclared() {
        return true;
    }
}
