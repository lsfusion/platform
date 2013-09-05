package lsfusion.client.logics.classes;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.Main;
import lsfusion.client.form.ClientFormController;
import lsfusion.client.form.EditBindingMap;
import lsfusion.client.form.PropertyEditor;
import lsfusion.client.form.PropertyRenderer;
import lsfusion.client.form.cell.DataPanelView;
import lsfusion.client.form.cell.PanelView;
import lsfusion.client.form.editor.IntegerPropertyEditor;
import lsfusion.client.form.renderer.IntegerPropertyRenderer;
import lsfusion.client.logics.ClientGroupObjectValue;
import lsfusion.client.logics.ClientPropertyDraw;
import lsfusion.interop.Compare;
import lsfusion.interop.Data;

import java.awt.*;
import java.io.IOException;
import java.text.Format;
import java.text.NumberFormat;
import java.text.ParseException;

import static lsfusion.interop.Compare.*;

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

    public PropertyRenderer getRendererComponent(ClientPropertyDraw property) {
        return new IntegerPropertyRenderer(property);
    }

    public PanelView getPanelView(ClientPropertyDraw key, ClientGroupObjectValue columnKey, ClientFormController form) {
        return new DataPanelView(form, key, columnKey);
    }

    public PropertyEditor getChangeEditorComponent(Component ownerComponent, ClientFormController form, ClientPropertyDraw property, Object value) {
        assert false:"shouldn't be used anymore";
        return null;
    }

    public PropertyEditor getObjectEditorComponent(Component ownerComponent, ClientFormController form, ClientPropertyDraw property, Object value) throws IOException, ClassNotFoundException {
        assert false:"shouldn't be used anymore";
        return null;
    }

    public PropertyEditor getValueEditorComponent(ClientFormController form, ClientPropertyDraw property, Object value) {
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

    @Override
    public Object transformServerValue(Object obj) {
        return obj;
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
    public EditBindingMap.EditEventFilter getEditEventFilter() {
        return null;
    }
}
