package lsfusion.client.logics.classes;

import lsfusion.base.BaseUtils;
import lsfusion.client.ClientResourceBundle;
import lsfusion.client.form.ClientFormController;
import lsfusion.client.form.EditBindingMap;
import lsfusion.client.form.PropertyEditor;
import lsfusion.client.form.cell.DataPanelView;
import lsfusion.client.form.cell.PanelView;
import lsfusion.client.logics.ClientGroupObjectValue;
import lsfusion.client.logics.ClientPropertyDraw;
import lsfusion.interop.Compare;

import java.awt.*;
import java.io.DataOutputStream;
import java.io.IOException;

import static lsfusion.interop.Compare.*;

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

    public int getMinimumWidth(int minCharWidth, FontMetrics fontMetrics) {
        String minMask = minCharWidth != 0
                      ? BaseUtils.replicate('0', minCharWidth)
                      : getMinimumMask();

        return fontMetrics.stringWidth(minMask) + 8;
    }

    public int getPreferredWidth(int prefCharWidth, FontMetrics fontMetrics) {
        String prefMask = prefCharWidth != 0
                      ? BaseUtils.replicate('0', prefCharWidth)
                      : getPreferredMask();

        return fontMetrics.stringWidth(prefMask) + 8;
    }

    public int getMaximumWidth(int maxCharWidth, FontMetrics fontMetrics) {
        if (maxCharWidth != 0)
            return fontMetrics.stringWidth(BaseUtils.replicate('0', maxCharWidth)) + 8;
        else
            return Integer.MAX_VALUE;
    }

    @Override
    public int getPreferredHeight(FontMetrics fontMetrics) {
        return fontMetrics.getHeight() + 1;
    }

    @Override
    public int getMaximumHeight(FontMetrics fontMetrics) {
        return getPreferredHeight(fontMetrics);
    }

    public String getMinimumMask() {
        return getPreferredMask();
    }

    public abstract String getPreferredMask();

    public PanelView getPanelView(ClientPropertyDraw key, ClientGroupObjectValue columnKey, ClientFormController form) {
        return new DataPanelView(form, key, columnKey);
    }

    public PropertyEditor getChangeEditorComponent(Component ownerComponent, ClientFormController form, ClientPropertyDraw property, Object value) {
        return getDataClassEditorComponent(value, property);
    }

    public PropertyEditor getObjectEditorComponent(Component ownerComponent, ClientFormController form, ClientPropertyDraw property, Object value) throws IOException, ClassNotFoundException {
        return getDataClassEditorComponent(value, property);
    }

    public PropertyEditor getValueEditorComponent(ClientFormController form, ClientPropertyDraw property, Object value) {
        return getDataClassEditorComponent(value, property);
    }

    protected abstract PropertyEditor getDataClassEditorComponent(Object value, ClientPropertyDraw property);

    public boolean shouldBeDrawn(ClientFormController form) {
        return true;
    }

    @Override
    public Object transformServerValue(Object obj) {
        return obj;
    }

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
