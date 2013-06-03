package lsfusion.client.logics;

import lsfusion.base.context.ApplicationContext;
import lsfusion.client.ClientResourceBundle;
import lsfusion.client.descriptor.editor.ComponentEditor;
import lsfusion.client.serialization.ClientSerializationPool;
import lsfusion.interop.form.layout.SimplexConstraints;

import javax.swing.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ClientShowType extends ClientComponent {

    public ClientShowType() {
    }

    public ClientGroupObject groupObject;

    @Override
    public SimplexConstraints<ClientComponent> getDefaultConstraints() {
        return SimplexConstraints.getShowTypeDefaultConstraints(super.getDefaultConstraints());
    }

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
        return ClientResourceBundle.getString("logics.view");
    }

    @Override
    public String toString() {
        return getCaption() + " (" + groupObject.toString() + ")" + "[sid:" + getSID() + "]";
    }

    @Override
    public JComponent getPropertiesEditor() {
        return new ComponentEditor(this);
    }

    @Override
    public boolean shouldBeDeclared() {
        return true;
    }
}
