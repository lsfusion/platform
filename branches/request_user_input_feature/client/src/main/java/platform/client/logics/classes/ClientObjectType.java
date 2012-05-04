package platform.client.logics.classes;

import platform.client.ClientResourceBundle;
import platform.client.Main;
import platform.client.form.ClientFormController;
import platform.client.form.PropertyEditorComponent;
import platform.client.form.PropertyRendererComponent;
import platform.client.form.cell.CellView;
import platform.client.form.cell.DataCellView;
import platform.client.form.editor.IntegerPropertyEditor;
import platform.client.form.editor.ObjectPropertyEditor;
import platform.client.form.renderer.IntegerPropertyRenderer;
import platform.client.logics.ClientGroupObjectValue;
import platform.client.logics.ClientPropertyDraw;
import platform.gwt.view.classes.GObjectType;
import platform.gwt.view.classes.GType;
import platform.interop.Compare;
import platform.interop.Data;
import platform.interop.form.RemoteDialogInterface;

import java.awt.*;
import java.io.IOException;
import java.rmi.RemoteException;
import java.text.Format;
import java.text.NumberFormat;
import java.text.ParseException;

import static platform.client.ClientResourceBundle.getString;
import static platform.interop.Compare.*;

public class ClientObjectType implements ClientType, ClientTypeClass {

    public ClientTypeClass getTypeClass() {
        return this;
    }

    public ClientClass getDefaultClass(ClientObjectClass baseClass) {
        return Main.getBaseClass(); // пока так подебильному
    }

    public byte getTypeId() {
        return Data.OBJECT;
    }

    public int getMinimumWidth(int minCharWidth, FontMetrics fontMetrics) {
        return fontMetrics.stringWidth("999 999") + 8;
    }

    public int getPreferredWidth(int prefCharWidth, FontMetrics fontMetrics) {
        return fontMetrics.stringWidth("9 999 999") + 8;
    }

    public int getMaximumWidth(int maxCharWidth, FontMetrics fontMetrics) {
        return getPreferredWidth(0, fontMetrics);
    }

    public int getPreferredHeight(FontMetrics fontMetrics) {
        return fontMetrics.getHeight() + 1;
    }

    @Override
    public int getMaximumHeight(FontMetrics fontMetrics) {
        return getPreferredHeight(fontMetrics);
    }

    public Format getDefaultFormat() {
        return NumberFormat.getInstance();
    }

    public PropertyRendererComponent getRendererComponent(String caption, ClientPropertyDraw property) {
        return new IntegerPropertyRenderer(property);
    }

    public CellView getPanelComponent(ClientPropertyDraw key, ClientGroupObjectValue columnKey, ClientFormController form) {
        return new DataCellView(form, key, columnKey);
    }

    public PropertyEditorComponent getChangeEditorComponent(Component ownerComponent, ClientFormController form, ClientPropertyDraw property, Object value) {
        assert false:"shouldn't be used anymore";
        try {
            return new ObjectPropertyEditor(ownerComponent, form.createChangeEditorDialog(property), true);
        } catch (RemoteException e) {
            throw new RuntimeException(getString("errors.error.getting.editing.value"), e);
        }
    }

    public PropertyEditorComponent getObjectEditorComponent(Component ownerComponent, ClientFormController form, ClientPropertyDraw property, Object value) throws IOException, ClassNotFoundException {
        assert false:"shouldn't be used anymore";
        RemoteDialogInterface remoteDialog = form.createObjectEditorDialog(property);
        return remoteDialog == null
               ? null
               : new ObjectPropertyEditor(ownerComponent, remoteDialog, false);
    }

    public PropertyEditorComponent getValueEditorComponent(ClientFormController form, ClientPropertyDraw property, Object value) {
        return new IntegerPropertyEditor(value, (NumberFormat) ClientIntegerClass.instance.getDefaultFormat(), null, Integer.class);
    }

    public boolean shouldBeDrawn(ClientFormController form) {
        return true;
    }

    public Object parseString(String s) throws ParseException {
        throw new ParseException(ClientResourceBundle.getString("logics.classes.objectclass.doesnt.support.convertation.from.string"), 0);
    }

    @Override
    public String formatString(Object obj) {
        return obj.toString();
    }

    public String getConfirmMessage() {
        return ClientResourceBundle.getString("logics.classes.do.you.really.want.to.edit.property");
    }

    @Override
    public String toString() {
        return ClientResourceBundle.getString("logics.object");
    }

    public ClientType getDefaultType() {
        return this;
    }

    @Override
    public Compare[] getFilterCompares() {
        return new Compare[] {EQUALS, GREATER, LESS, GREATER_EQUALS, LESS_EQUALS, NOT_EQUALS};
    }

    @Override
    public Compare getDefaultCompare() {
        return EQUALS;
    }

    @Override
    public GType getGwtType() {
        return GObjectType.instance;
    }
}
