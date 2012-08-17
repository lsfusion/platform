package platform.client.logics.classes;

import platform.client.ClientResourceBundle;
import platform.client.form.ClientFormController;
import platform.client.form.PropertyEditorComponent;
import platform.client.form.PropertyRendererComponent;
import platform.client.form.cell.ActionPanelView;
import platform.client.form.cell.PanelView;
import platform.client.form.editor.ActionPropertyEditor;
import platform.client.form.renderer.ActionPropertyRenderer;
import platform.client.logics.ClientGroupObjectValue;
import platform.client.logics.ClientPropertyDraw;
import platform.interop.Data;

import java.awt.*;
import java.io.DataInputStream;
import java.io.IOException;
import java.text.Format;
import java.text.ParseException;

public class ClientActionClass extends ClientDataClass implements ClientTypeClass {

    private final String sID = "ActionClass";

    @Override
    public String getSID() {
        return sID;
    }

    public ClientActionClass(DataInputStream inStream) throws IOException {
        super(inStream);
    }

    public byte getTypeId() {
        return Data.ACTION;
    }

    public String getPreferredMask() {
        return "1234567";
    }

    @Override
    public int getMaximumWidth(int maxCharWidth, FontMetrics fontMetrics) {
        return getPreferredWidth(0, fontMetrics);
    }

    public Format getDefaultFormat() {
        return null;
    }

    public PropertyRendererComponent getRendererComponent(ClientPropertyDraw property) {
        return new ActionPropertyRenderer(property);
    }

    public PanelView getPanelView(ClientPropertyDraw key, ClientGroupObjectValue columnKey, ClientFormController form) {
        return new ActionPanelView(key, columnKey, form);
    }

    protected PropertyEditorComponent getDataClassEditorComponent(Object value, ClientPropertyDraw property) {
        return new ActionPropertyEditor(property);
    }

    public Object parseString(String s) throws ParseException {
        throw new ParseException(ClientResourceBundle.getString("logics.classes.actionclass.doesnt.support.convertation.from.string"), 0);
    }

    @Override
    public String formatString(Object obj) throws ParseException {
        throw new ParseException(ClientResourceBundle.getString("logics.classes.actionclass.doesnt.support.convertation.from.string"), 0);
    }

    @Override
    public String getConfirmMessage() {
        return ClientResourceBundle.getString("logics.classes.do.you.really.want.to.take.action");
    }

    @Override
    public String toString() {
        return ClientResourceBundle.getString("logics.classes.actionclass");
    }
}
