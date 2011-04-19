package platform.client.logics.classes;

import platform.client.form.ClientFormController;
import platform.client.form.PropertyEditorComponent;
import platform.client.form.PropertyRendererComponent;
import platform.client.form.cell.CellView;
import platform.client.form.cell.TableCellView;
import platform.client.form.editor.IntegerPropertyEditor;
import platform.client.form.editor.ObjectPropertyEditor;
import platform.client.form.renderer.IntegerPropertyRenderer;
import platform.client.logics.ClientGroupObjectValue;
import platform.client.logics.ClientPropertyDraw;
import platform.client.Main;
import platform.interop.Compare;
import platform.interop.ComponentDesign;
import platform.interop.Data;
import platform.interop.form.RemoteDialogInterface;

import java.awt.*;
import java.io.IOException;
import java.text.Format;
import java.text.NumberFormat;
import java.text.ParseException;

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

    public Format getDefaultFormat() {
        return NumberFormat.getInstance();
    }

    public PropertyRendererComponent getRendererComponent(Format format, String caption, ComponentDesign design) {
        return new IntegerPropertyRenderer(format, design);
    }

    public CellView getPanelComponent(ClientPropertyDraw key, ClientGroupObjectValue columnKey, ClientFormController form) {
        return new TableCellView(key, columnKey, form);
    }

    public PropertyEditorComponent getEditorComponent(Component ownerComponent, ClientFormController form, ClientPropertyDraw property, Object value, Format format, ComponentDesign design) throws IOException, ClassNotFoundException {
        return new ObjectPropertyEditor(ownerComponent, property.createEditorForm(form.remoteForm));
    }

    public PropertyEditorComponent getObjectEditorComponent(Component ownerComponent, ClientFormController form, ClientPropertyDraw property, Object value, Format format, ComponentDesign design) throws IOException, ClassNotFoundException {
        RemoteDialogInterface remoteDialog = form.remoteForm.createObjectEditorDialog(property.ID);
        return remoteDialog == null
               ? null
               : new ObjectPropertyEditor(ownerComponent, remoteDialog);
    }

    public PropertyEditorComponent getClassComponent(ClientFormController form, ClientPropertyDraw property, Object value, Format format) throws IOException, ClassNotFoundException {
        return new IntegerPropertyEditor(value, (NumberFormat) ClientIntegerClass.instance.getDefaultFormat(), null, Integer.class);
//        return new ObjectPropertyEditor(form.getComponent(), property.createClassForm(form.remoteForm, (Integer) value));
    }

    public boolean shouldBeDrawn(ClientFormController form) {
        return true;
    }

    public Object parseString(String s) throws ParseException {
        throw new ParseException("ObjectClass не поддерживает конвертацию из строки.", 0);
    }

    public String getConformedMessage() {
        return "Вы действительно хотите редактировать свойство";
    }

    @Override
    public String toString() {
        return "Объект";
    }

    public ClientType getDefaultType() {
        return this;
    }

    @Override
    public Compare[] getFilerCompares() {
        return new Compare[] {EQUALS, GREATER, LESS, GREATER_EQUALS, LESS_EQUALS, NOT_EQUALS};
    }

    @Override
    public Compare getDefaultCompare() {
        return EQUALS;
    }
}
