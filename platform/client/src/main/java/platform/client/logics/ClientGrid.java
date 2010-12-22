package platform.client.logics;

import platform.client.descriptor.editor.GridEditor;
import platform.base.context.ApplicationContext;
import platform.client.serialization.ClientSerializationPool;
import platform.interop.form.layout.SimplexConstraints;

import javax.swing.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ClientGrid extends ClientComponent {

    public boolean showFind;
    public boolean showFilter;

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
        outStream.writeBoolean(showFind);
        outStream.writeBoolean(showFilter);

        outStream.writeByte(minRowCount);
        outStream.writeBoolean(tabVertical);
        outStream.writeBoolean(autoHide);

        pool.serializeObject(outStream, groupObject);
    }

    @Override
    public void customDeserialize(ClientSerializationPool pool, DataInputStream inStream) throws IOException {
        super.customDeserialize(pool, inStream);

        showFind = inStream.readBoolean();
        showFilter = inStream.readBoolean();

        minRowCount = inStream.readByte();
        tabVertical = inStream.readBoolean();
        autoHide = inStream.readBoolean();

        groupObject = pool.deserializeObject(inStream);
    }

    public String getCaption() {
        return "Таблица";
    }

    @Override
    public String toString() {
        return getCaption() + " (" + groupObject.toString() + ")";
    }

    @Override
    public JComponent getPropertiesEditor() {
        return new GridEditor(this);
    }

    public void setShowFind(boolean showFind) {
        this.showFind = showFind;
        updateDependency(this, "showFind");
    }

    public boolean getShowFind() {
        return showFind;
    }

    public void setShowFilter(boolean showFilter) {
        this.showFilter = showFilter;
        updateDependency(this, "showFilter");
    }

    public boolean getShowFilter() {
        return showFilter;
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
    public String getCodeConstructor(String name) {
        return "GridView " + name + " = design.createGrid(" + groupObject.getID() + ");";
    }
}
