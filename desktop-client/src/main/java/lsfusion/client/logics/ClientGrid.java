package lsfusion.client.logics;

import lsfusion.base.context.ApplicationContext;
import lsfusion.client.ClientResourceBundle;
import lsfusion.client.descriptor.editor.GridEditor;
import lsfusion.client.serialization.ClientSerializationPool;
import lsfusion.interop.form.layout.FlexAlignment;

import javax.swing.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ClientGrid extends ClientComponent {

    public boolean tabVertical = false;
    public boolean autoHide;
    public boolean quickSearch;

    public ClientGroupObject groupObject;

    public ClientGrid() {
    }

    public ClientGrid(ApplicationContext context) {
        super(context);
    }

    @Override
    protected void initDefaultConstraints() {
        flex = 1;
        alignment = FlexAlignment.STRETCH;
    }

    @Override
    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        super.customSerialize(pool, outStream, serializationType);

        outStream.writeBoolean(tabVertical);
        outStream.writeBoolean(autoHide);
        outStream.writeBoolean(quickSearch);

        pool.serializeObject(outStream, groupObject);
    }

    @Override
    public void customDeserialize(ClientSerializationPool pool, DataInputStream inStream) throws IOException {
        super.customDeserialize(pool, inStream);

        tabVertical = inStream.readBoolean();
        autoHide = inStream.readBoolean();
        quickSearch = inStream.readBoolean();

        groupObject = pool.deserializeObject(inStream);
    }

    public String getCaption() {
        return ClientResourceBundle.getString("logics.grid");
    }

    @Override
    public String toString() {
        return getCaption() + " (" + groupObject.toString() + ")" + "[sid:" + getSID() + "]";
    }

    @Override
    public JComponent getPropertiesEditor() {
        return new GridEditor(this);
    }

    public void setTabVertical(boolean tabVertical) {
        this.tabVertical = tabVertical;
        updateDependency(this, "tabVertical");
    }

    public boolean getTabVertical() {
        return tabVertical;
    }

    public void setAutoHide(boolean autoHide) {
        this.autoHide = autoHide;
        updateDependency(this, "autoHide");
    }

    public boolean getAutoHide() {
        return autoHide;
    }
}
