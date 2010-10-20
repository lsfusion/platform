package platform.client.logics;

import platform.client.descriptor.editor.logics.ClientComponentEditor;
import platform.client.descriptor.editor.logics.ClientGridEditor;
import platform.client.descriptor.increment.IncrementDependency;
import platform.client.serialization.ClientSerializationPool;

import javax.swing.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collection;

public class ClientGrid extends ClientComponent {

    public boolean showFind;
    public boolean showFilter;

    public byte minRowCount;
    public boolean tabVertical = true;
    public boolean autoHide;

    public ClientGroupObject groupObject;

    public ClientGrid() {
        
    }

    @Override
    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        super.customSerialize(pool, outStream, serializationType);
        outStream.writeBoolean(showFind);
        outStream.writeBoolean(showFilter);

        outStream.writeByte(minRowCount);
        outStream.writeBoolean(tabVertical);
        outStream.writeBoolean(autoHide);

        pool.serializeObject(outStream, groupObject);
    }

    @Override
    public void customDeserialize(ClientSerializationPool pool, int iID, DataInputStream inStream) throws IOException {
        super.customDeserialize(pool, iID, inStream);

        showFind = inStream.readBoolean();
        showFilter = inStream.readBoolean();

        minRowCount = inStream.readByte();
        tabVertical = inStream.readBoolean();
        autoHide = inStream.readBoolean();

        groupObject = pool.deserializeObject(inStream);
    }

    @Override
    public String toString() {
        return "Таблица (" + groupObject.toString() + ")";
    }

    public JComponent getPropertiesEditor() {
        return new ClientGridEditor(this);
    }

    public void setShowFind(boolean showFind) {
        this.showFind = showFind;
        IncrementDependency.update(this, "showFind");
    }

    public boolean getShowFind() {
        return showFind;
    }

    public void setShowFilter(boolean showFilter) {
        this.showFilter = showFilter;
        IncrementDependency.update(this, "showFilter");
    }

    public boolean getShowFilter() {
        return showFilter;
    }

    public void setTabVertical(boolean tabVertical) {
        this.tabVertical = tabVertical;
        IncrementDependency.update(this, "tabVertical");
    }

    public boolean getTabVertical() {
        return tabVertical;
    }

    public void setAutoHide(boolean autoHide) {
        this.autoHide = autoHide;
        IncrementDependency.update(this, "autoHide");
    }

    public boolean getAutoHide() {
        return autoHide;
    }

    public void setMinRowCount(byte minRowCount) {
        this.minRowCount = minRowCount;
        IncrementDependency.update(this, "minRowCount");
    }

    public byte getMinRowCount() {
        return minRowCount;
    }
}
