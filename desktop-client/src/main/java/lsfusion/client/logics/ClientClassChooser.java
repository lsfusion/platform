package lsfusion.client.logics;

import lsfusion.base.context.ApplicationContext;
import lsfusion.client.ClientResourceBundle;
import lsfusion.client.descriptor.editor.ComponentEditor;
import lsfusion.client.serialization.ClientSerializationPool;
import lsfusion.interop.form.layout.FlexAlignment;

import javax.swing.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ClientClassChooser extends ClientComponent {

    public ClientObject object;

    public boolean visible = true;

    public ClientClassChooser() {
    }

    public ClientClassChooser(int ID, ApplicationContext context) {
        super(ID, context);
    }

    @Override
    protected void initDefaultConstraints() {
        flex = 0.2;
        alignment = FlexAlignment.STRETCH;
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
}
