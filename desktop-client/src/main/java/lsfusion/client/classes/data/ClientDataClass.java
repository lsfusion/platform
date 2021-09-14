package lsfusion.client.classes.data;

import lsfusion.base.BaseUtils;
import lsfusion.client.ClientResourceBundle;
import lsfusion.client.base.view.SwingDefaults;
import lsfusion.client.classes.ClientClass;
import lsfusion.client.classes.ClientType;
import lsfusion.client.classes.ClientTypeClass;
import lsfusion.client.form.controller.ClientFormController;
import lsfusion.client.form.object.ClientGroupObjectValue;
import lsfusion.client.form.object.panel.controller.PropertyPanelController;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.EditBindingMap;
import lsfusion.client.form.property.cell.classes.controller.PropertyEditor;
import lsfusion.client.form.property.panel.view.DataPanelView;
import lsfusion.client.form.property.panel.view.PanelView;
import lsfusion.client.form.property.table.view.AsyncChangeInterface;
import lsfusion.interop.form.property.Compare;

import java.awt.*;
import java.io.DataOutputStream;
import java.io.IOException;

import static lsfusion.interop.form.property.Compare.*;

public abstract class ClientDataClass extends ClientClass implements ClientType {

    protected ClientDataClass() {
    }

    @Override
    public void serialize(DataOutputStream outStream) throws IOException {
        outStream.writeByte(getTypeClass().getTypeId());
    }

    public boolean hasChildren() {
        return false;
    }

    // добавляет поправку на кнопки и другие элементы 
    public int getFullWidthString(String widthString, FontMetrics fontMetrics, ClientPropertyDraw propertyDraw) {
        Insets insets = SwingDefaults.getTableCellMargins();
        return fontMetrics.stringWidth(widthString) + insets.left + insets.right;
    }
    
    public int getDefaultWidth(FontMetrics fontMetrics, ClientPropertyDraw property) {
        return getFullWidthString(getDefaultWidthString(property), fontMetrics, property);
    }

    protected int getDefaultCharWidth() {
        return 0;
    }

    protected String getDefaultWidthString(ClientPropertyDraw propertyDraw) {
        int defaultCharWidth = getDefaultCharWidth();
        if(defaultCharWidth != 0)
            return BaseUtils.replicate('0', defaultCharWidth);
        throw new UnsupportedOperationException();
    }

    @Override
    public int getDefaultCharHeight() {
        return 1;
    }

    @Override
    public PanelView getPanelView(ClientPropertyDraw key, ClientGroupObjectValue columnKey, ClientFormController form, PropertyPanelController.CaptionContainer captionContainer) {
        return new DataPanelView(form, key, columnKey, captionContainer);
    }

    @Override
    public PropertyEditor getChangeEditorComponent(Component ownerComponent, ClientFormController form, ClientPropertyDraw property, AsyncChangeInterface asyncChange, Object value) {
        return getDataClassEditorComponent(value, property, asyncChange);
    }

    @Override
    public PropertyEditor getValueEditorComponent(ClientFormController form, ClientPropertyDraw property, AsyncChangeInterface asyncChange, Object value) {
        return getDataClassEditorComponent(value, property, asyncChange);
    }

    protected abstract PropertyEditor getDataClassEditorComponent(Object value, ClientPropertyDraw property, AsyncChangeInterface asyncChange);

    public String getConfirmMessage() {
        return ClientResourceBundle.getString("logics.classes.do.you.really.want.to.edit.property");
    }

    // за исключение классов динамической ширины - так как нету множественного наследования и не хочется каждому прописывать
    @SuppressWarnings("UnusedDeclaration")
    public ClientType getDefaultType() {
        return this;
    }

    public ClientTypeClass getTypeClass() {
        return (ClientTypeClass) this;
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
