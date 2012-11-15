package platform.client.logics.classes;

import platform.client.ClientResourceBundle;
import platform.client.form.PropertyEditorComponent;
import platform.client.form.PropertyRendererComponent;
import platform.client.form.editor.TimePropertyEditor;
import platform.client.form.renderer.TimePropertyRenderer;
import platform.client.logics.ClientPropertyDraw;
import platform.interop.Data;

import java.awt.*;
import java.text.Format;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class ClientTimeClass extends ClientDataClass implements ClientTypeClass {

    public final static ClientTimeClass instance = new ClientTimeClass();

    private final String sID = "TimeClass";

    public String getPreferredMask() {
        return "00:00:00";
    }

    protected PropertyEditorComponent getDataClassEditorComponent(Object value, ClientPropertyDraw property) {
        return new TimePropertyEditor(value, (SimpleDateFormat) property.getFormat(), property.design);
    }

    public String getSID() {
        return sID;
    }

    public Format getDefaultFormat() {
        return new SimpleDateFormat("HH:mm:ss");
    }

    public PropertyRendererComponent getRendererComponent(ClientPropertyDraw property) {
        return new TimePropertyRenderer(property);
    }

    public Object parseString(String s) throws ParseException {
        try {
            return getDefaultFormat().parseObject(s);
        } catch (Exception e) {
            throw new ParseException(s + ClientResourceBundle.getString("logics.classes.can.not.be.converted.to.time"), 0);
        }
    }

    public String formatString(Object obj) throws ParseException {
        if (obj != null) {
            return getDefaultFormat().format(obj);
        }
        else return "";
    }

    public byte getTypeId() {
        return Data.TIME;
    }

    public String toString() {
        return ClientResourceBundle.getString("logics.classes.time");
    }

    @Override
    public int getPreferredWidth(int prefCharWidth, FontMetrics fontMetrics) {
        return 65;
    }
}
