package platform.client.logics;

import platform.base.context.ApplicationContext;
import platform.client.ClientResourceBundle;
import platform.client.descriptor.FormDescriptor;
import platform.client.descriptor.editor.ToolbarEditor;
import platform.client.serialization.ClientSerializationPool;
import platform.interop.form.layout.SimplexConstraints;

import javax.swing.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ClientToolbar extends ClientComponent {

    public boolean visible = true;

    public boolean showGroupChange = true;
    public boolean showCountQuantity = true;
    public boolean showCalculateSum = true;
    public boolean showGroup = true;
    public boolean showPrintGroupButton = true;
    public boolean showPrintGroupXlsButton = true;
    public boolean showHideSettings = true;

    public ClientToolbar() {
    }

    public ClientToolbar(ApplicationContext context) {
        super(context);
    }

    @Override
    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        super.customSerialize(pool, outStream, serializationType);

        outStream.writeBoolean(visible);

        outStream.writeBoolean(showGroupChange);
        outStream.writeBoolean(showCountQuantity);
        outStream.writeBoolean(showCalculateSum);
        outStream.writeBoolean(showGroup);
        outStream.writeBoolean(showPrintGroupButton);
        outStream.writeBoolean(showPrintGroupXlsButton);
        outStream.writeBoolean(showHideSettings);
    }

    @Override
    public void customDeserialize(ClientSerializationPool pool, DataInputStream inStream) throws IOException {
        super.customDeserialize(pool, inStream);

        visible = inStream.readBoolean();

        showGroupChange = inStream.readBoolean();
        showCountQuantity = inStream.readBoolean();
        showCalculateSum = inStream.readBoolean();
        showGroup = inStream.readBoolean();
        showPrintGroupButton = inStream.readBoolean();
        showPrintGroupXlsButton = inStream.readBoolean();
        showHideSettings = inStream.readBoolean();
    }

//    public void setShowFind(boolean showFind) {
//        this.showFind = showFind;
//        updateDependency(this, "showFind");
//    }
//
//    public boolean getShowFind() {
//        return showFind;
//    }
//
//    public void setShowFilter(boolean showFilter) {
//        this.showFilter = showFilter;
//        updateDependency(this, "showFilter");
//    }
//
//    public boolean getShowFilter() {
//        return showFilter;
//    }


    public boolean getVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
        updateDependency(this, "visible");
    }

    public boolean getShowPrintGroupButton() {
        return showPrintGroupButton;
    }

    public void setShowPrintGroupButton(boolean showPrintGroupButton) {
        this.showPrintGroupButton = showPrintGroupButton;
        updateDependency(this, "showPrintGroupButton");
    }

    public boolean getShowPrintGroupXlsButton() {
        return showPrintGroupXlsButton;
    }

    public void setShowPrintGroupXlsButton(boolean showPrintGroupXlsButton) {
        this.showPrintGroupXlsButton = showPrintGroupXlsButton;
        updateDependency(this, "showPrintGroupXlsButton");
    }

    public boolean getShowHideSettings() {
        return showHideSettings;
    }

    public void setShowHideSettings(boolean showHideSettings) {
        this.showHideSettings = showHideSettings;
        updateDependency(this, "showHideSettings");
    }

    public boolean getShowCountQuantity() {
        return showCountQuantity;
    }

    public void setShowCountQuantity(boolean showCountQuantity) {
        this.showCountQuantity = showCountQuantity;
        updateDependency(this, "showCountQuantity");
    }

    public boolean getShowGroupChange() {
        return showGroupChange;
    }

    public void setShowGroupChange(boolean showGroupChange) {
        this.showGroupChange = showGroupChange;
        updateDependency(this, "showGroupChange");
    }

    public boolean getShowCalculateSum() {
        return showCalculateSum;
    }

    public void setShowCalculateSum(boolean showCalculateSum) {
        this.showCalculateSum = showCalculateSum;
        updateDependency(this, "showCalculateSum");
    }

    public boolean getShowGroup() {
        return showGroup;
    }

    public void setShowGroup(boolean showGroupButton) {
        this.showGroup = showGroupButton;
        updateDependency(this, "showGroup");
    }

    @Override
    public SimplexConstraints<ClientComponent> getDefaultConstraints() {
        return SimplexConstraints.getShowTypeDefaultConstraints(super.getDefaultConstraints());
    }

    @Override
    public String getCaption() {
        return ClientResourceBundle.getString("logics.toolbar");
    }

    @Override
    public String toString() {
        return getCaption() + "[sid:" + getSID() + "]";
    }

    @Override
    public JComponent getPropertiesEditor() {
        return new ToolbarEditor(this);
    }

    public String getCodeClass() {
        return "ToolbarView";
    }

    @Override
    public String getCodeConstructor() {
        return "design.createToolbar()";
    }

    @Override
    public String getVariableName(FormDescriptor form) {
        return getSID() + "Toolbar";
    }

    @Override
    public boolean shouldBeDeclared() {
        return true;
    }
}
