package lsfusion.client.logics.classes;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.form.ClientFormController;
import lsfusion.client.form.EditBindingMap;
import lsfusion.client.form.PropertyEditor;
import lsfusion.client.form.PropertyRenderer;
import lsfusion.client.form.cell.DataPanelView;
import lsfusion.client.form.cell.PanelView;
import lsfusion.client.form.editor.IntegerPropertyEditor;
import lsfusion.client.form.renderer.IntegralPropertyRenderer;
import lsfusion.client.logics.ClientGroupObjectValue;
import lsfusion.client.logics.ClientPropertyDraw;
import lsfusion.interop.Compare;
import lsfusion.interop.Data;

import java.awt.*;
import java.io.IOException;
import java.text.ParseException;

import static lsfusion.interop.Compare.*;

public class ClientObjectType implements ClientType, ClientTypeClass {

    public ClientTypeClass getTypeClass() {
        return this;
    }

    public byte getTypeId() {
        return Data.OBJECT;
    }

    @Override
    public int getFullWidthString(String widthString, FontMetrics fontMetrics, ClientPropertyDraw propertyDraw) {
        return fontMetrics.stringWidth(widthString) + 8;
    }

    @Override
    public int getDefaultWidth(FontMetrics fontMetrics, ClientPropertyDraw property) {
        return getFullWidthString("0000000", fontMetrics, property);
    }

    public int getDefaultHeight(FontMetrics fontMetrics, int numRowHeight) {
        return fontMetrics.getHeight() * numRowHeight + 1;
    }

    public PropertyRenderer getRendererComponent(ClientPropertyDraw property) {
        return new IntegralPropertyRenderer(property);
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
        return new IntegerPropertyEditor(value, ClientLongClass.instance.getDefaultFormat(), null, Long.class);
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
    public EditBindingMap.EditEventFilter getEditEventFilter() {
        return null;
    }
}
