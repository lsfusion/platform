package lsfusion.client.logics;

import lsfusion.base.context.ApplicationContext;
import lsfusion.client.ClientResourceBundle;
import lsfusion.client.descriptor.editor.ToolbarEditor;
import lsfusion.client.serialization.ClientSerializationPool;
import lsfusion.interop.form.layout.SimplexConstraints;

import javax.swing.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ClientToolbar extends ClientComponent {

    public boolean visible = true;

    public boolean showGroupChange = true;
    public boolean showCountRows = true;
    public boolean showCalculateSum = true;
    public boolean showGroupReport = true;
    public boolean showPrint = true;
    public boolean showXls = true;
    public boolean showSettings = true;

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
        outStream.writeBoolean(showCountRows);
        outStream.writeBoolean(showCalculateSum);
        outStream.writeBoolean(showGroupReport);
        outStream.writeBoolean(showPrint);
        outStream.writeBoolean(showXls);
        outStream.writeBoolean(showSettings);
    }

    @Override
    public void customDeserialize(ClientSerializationPool pool, DataInputStream inStream) throws IOException {
        super.customDeserialize(pool, inStream);

        visible = inStream.readBoolean();

        showGroupChange = inStream.readBoolean();
        showCountRows = inStream.readBoolean();
        showCalculateSum = inStream.readBoolean();
        showGroupReport = inStream.readBoolean();
        showPrint = inStream.readBoolean();
        showXls = inStream.readBoolean();
        showSettings = inStream.readBoolean();
    }

    public boolean getVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
        updateDependency(this, "visible");
    }

    public boolean getShowPrint() {
        return showPrint;
    }

    public void setShowPrint(boolean showPrint) {
        this.showPrint = showPrint;
        updateDependency(this, "showPrint");
    }

    public boolean getShowXls() {
        return showXls;
    }

    public void setShowXls(boolean showXls) {
        this.showXls = showXls;
        updateDependency(this, "showXls");
    }

    public boolean getShowSettings() {
        return showSettings;
    }

    public void setShowSettings(boolean showSettings) {
        this.showSettings = showSettings;
        updateDependency(this, "showSettings");
    }

    public boolean getShowCountRows() {
        return showCountRows;
    }

    public void setShowCountRows(boolean showCountRows) {
        this.showCountRows = showCountRows;
        updateDependency(this, "showCountRows");
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

    public boolean getShowGroupReport() {
        return showGroupReport;
    }

    public void setShowGroupReport(boolean showGroupButton) {
        this.showGroupReport = showGroupButton;
        updateDependency(this, "showGroupReport");
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

}
