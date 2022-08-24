package lsfusion.client.classes;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.base.view.SwingDefaults;
import lsfusion.client.classes.data.ClientLongClass;
import lsfusion.client.form.controller.ClientFormController;
import lsfusion.client.form.design.view.flex.LinearCaptionContainer;
import lsfusion.client.form.object.ClientGroupObjectValue;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.EditBindingMap;
import lsfusion.client.form.property.cell.classes.controller.IntegerPropertyEditor;
import lsfusion.client.form.property.cell.classes.controller.PropertyEditor;
import lsfusion.client.form.property.cell.classes.view.IntegralPropertyRenderer;
import lsfusion.client.form.property.cell.view.PropertyRenderer;
import lsfusion.client.form.property.panel.view.DataPanelView;
import lsfusion.client.form.property.panel.view.PanelView;
import lsfusion.client.form.property.table.view.AsyncChangeInterface;
import lsfusion.interop.classes.DataType;
import lsfusion.interop.form.property.Compare;

import java.awt.*;
import java.text.ParseException;

import static lsfusion.interop.form.property.Compare.*;

public class ClientObjectType implements ClientType, ClientTypeClass {

    public ClientTypeClass getTypeClass() {
        return this;
    }

    public byte getTypeId() {
        return DataType.OBJECT;
    }

    @Override
    public int getFullWidthString(String widthString, FontMetrics fontMetrics, ClientPropertyDraw propertyDraw) {
        Insets insets = SwingDefaults.getTableCellMargins();
        return fontMetrics.stringWidth(widthString) + insets.left + insets.right;
    }

    @Override
    public int getDefaultWidth(FontMetrics fontMetrics, ClientPropertyDraw property) {
        return getFullWidthString("0000000", fontMetrics, property);
    }

    @Override
    public int getDefaultCharHeight() {
        return 1;
    }

    public PropertyRenderer getRendererComponent(ClientPropertyDraw property) {
        return new IntegralPropertyRenderer(property);
    }

    public PanelView getPanelView(ClientPropertyDraw key, ClientGroupObjectValue columnKey, ClientFormController form, LinearCaptionContainer captionContainer) {
        return new DataPanelView(form, key, columnKey, captionContainer);
    }

    @Override
    public PropertyEditor getChangeEditorComponent(Component ownerComponent, ClientFormController form, ClientPropertyDraw property, AsyncChangeInterface asyncChange, Object value) {
        assert false:"shouldn't be used anymore";
        return null;
    }

    @Override
    public PropertyEditor getValueEditorComponent(ClientFormController form, ClientPropertyDraw property, AsyncChangeInterface asyncChange, Object value) {
        return new IntegerPropertyEditor(value, ClientLongClass.instance.getDefaultFormat(), null, Long.class);
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
    public EditBindingMap.EditEventFilter getEditEventFilter() {
        return null;
    }
}
