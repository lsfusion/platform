package platform.client.logics;

import platform.base.context.ApplicationContext;
import platform.client.ClientResourceBundle;
import platform.client.descriptor.FormDescriptor;
import platform.client.descriptor.editor.ComponentEditor;
import platform.client.serialization.ClientSerializationPool;
import platform.interop.form.layout.SimplexConstraints;

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

    public String getCodeClass() {
        return "ShowTypeView";
    }

    @Override
    public String getCodeConstructor() {
        return "design.createShowType()";
    }

    @Override
    public String getVariableName(FormDescriptor form) {
        StringBuilder result = new StringBuilder("");
        for (ClientObject obj : groupObject.objects) {
            result.append(obj.baseClass.getSID());
        }
        return result + "ShowType";
    }

    @Override
    public boolean shouldBeDeclared() {
        return true;
    }
}
