package platform.client.logics;

import platform.base.context.ApplicationContext;
import platform.client.descriptor.FormDescriptor;
import platform.client.descriptor.editor.GridEditor;
import platform.client.serialization.ClientSerializationPool;
import platform.interop.form.layout.SimplexConstraints;

import javax.swing.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ClientGrid extends ClientComponent {

    public boolean showFind;
    public boolean showFilter;
    public boolean showGroupChange;
    public boolean showCountQuantity;
    public boolean showCalculateSum;
    public boolean showGroup;
    public boolean showPrintGroupButton;
    public boolean showPrintGroupXlsButton;

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
        outStream.writeBoolean(showGroupChange);
        outStream.writeBoolean(showCountQuantity);
        outStream.writeBoolean(showCalculateSum);
        outStream.writeBoolean(showGroup);
        outStream.writeBoolean(showPrintGroupButton);
        outStream.writeBoolean(showPrintGroupXlsButton);

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
        showGroupChange = inStream.readBoolean();
        showCountQuantity = inStream.readBoolean();
        showCalculateSum = inStream.readBoolean();
        showGroup = inStream.readBoolean();
        showPrintGroupButton = inStream.readBoolean();
        showPrintGroupXlsButton = inStream.readBoolean();

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

    public void setShowCountQuantity(boolean showCountQuantity) {
        this.showCountQuantity = showCountQuantity;
        updateDependency(this, "showCountQuantity");
    }

    public boolean getShowCountQuantity() {
        return showCountQuantity;
    }

    public void setShowGroupChange(boolean showGroupChange) {
        this.showGroupChange = showGroupChange;
        updateDependency(this, "showGroupChange");
    }

    public boolean getShowGroupChanged() {
        return showGroupChange;
    }

    public void setShowCalculateSum(boolean showCalculateSum) {
        this.showCalculateSum = showCalculateSum;
        updateDependency(this, "showCalculateSum");
    }

    public boolean getShowCalculateSum() {
        return showCalculateSum;
    }

    public void setShowGroup(boolean showGroupButton) {
        this.showGroup = showGroupButton;
        updateDependency(this, "showGroup");
    }

    public boolean getShowGroup() {
        return showGroup;
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

    public String getCodeClass() {
        return "GridView";
    }

    @Override
    public String getCodeConstructor() {
        return "design.createGrid()";
    }

    @Override
    public String getVariableName(FormDescriptor form) {
        StringBuilder result = new StringBuilder("");
        for (ClientObject obj : groupObject.objects) {
            result.append(obj.baseClass.getSID());
        }
        return result + "GridView";
    }

    @Override
    public boolean shouldBeDeclared() {
        return true;
    }
}
