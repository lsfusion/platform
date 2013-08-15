package lsfusion.client.logics;

import lsfusion.base.context.ApplicationContext;
import lsfusion.client.ClientResourceBundle;
import lsfusion.client.descriptor.editor.GridEditor;
import lsfusion.client.serialization.ClientSerializationPool;
import lsfusion.interop.form.layout.SimplexConstraints;

import javax.swing.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ClientGrid extends ClientComponent {

    public byte minRowCount;
    public boolean tabVertical = false;
    public boolean autoHide;

    public ClientGroupObject groupObject;

    public ClientGrid() {
    }

    public ClientGrid(ApplicationContext context) {
        super(context);
    }

    @Override
    public SimplexConstraints<ClientComponent> getDefaultConstraints() {
        return SimplexConstraints.getGridDefaultConstraints(super.getDefaultConstraints());
    }

    @Override
    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        super.customSerialize(pool, outStream, serializationType);

        outStream.writeByte(minRowCount);
        outStream.writeBoolean(tabVertical);
        outStream.writeBoolean(autoHide);

        pool.serializeObject(outStream, groupObject);
    }

    @Override
    public void customDeserialize(ClientSerializationPool pool, DataInputStream inStream) throws IOException {
        super.customDeserialize(pool, inStream);

        minRowCount = inStream.readByte();
        tabVertical = inStream.readBoolean();
        autoHide = inStream.readBoolean();

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

    public void setMinRowCount(byte minRowCount) {
        this.minRowCount = minRowCount;
        updateDependency(this, "minRowCount");
    }

    public byte getMinRowCount() {
        return minRowCount;
    }

    @Override
    public boolean shouldBeDeclared() {
        return true;
    }
}
