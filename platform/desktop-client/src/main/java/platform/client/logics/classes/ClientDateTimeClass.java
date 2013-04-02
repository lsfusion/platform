package platform.client.logics.classes;

import platform.base.DateConverter;
import platform.client.ClientResourceBundle;
import platform.client.form.PropertyEditor;
import platform.client.form.PropertyRenderer;
import platform.client.form.editor.DateTimePropertyEditor;
import platform.client.form.renderer.DateTimePropertyRenderer;
import platform.client.logics.ClientPropertyDraw;
import platform.interop.Data;

import java.awt.*;
import java.sql.Timestamp;
import java.text.Format;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ClientDateTimeClass extends ClientDataClass implements ClientTypeClass {

    public final static ClientDateTimeClass instance = new ClientDateTimeClass();

    private final String sID = "DateTimeClass";

    @Override
    public String getSID() {
        return sID;
    }

    public byte getTypeId() {
        return Data.DATETIME;
    }

    @Override
    public String getPreferredMask() {
        return "01.01.2001 00:00:00"; // пока так, хотя надо будет переделать в зависимости от Locale
    }

    public Format getDefaultFormat() {
        return new SimpleDateFormat("dd.MM.yy HH:mm:ss");
    }

    public PropertyRenderer getRendererComponent(ClientPropertyDraw property) {
        return new DateTimePropertyRenderer(property);
    }

    public PropertyEditor getDataClassEditorComponent(Object value, ClientPropertyDraw property) {
        return new DateTimePropertyEditor(value, (SimpleDateFormat) property.getFormat(), property.design);
    }

    public Object parseString(String s) throws ParseException {
        try {
            return DateConverter.dateToStamp((Date) getDefaultFormat().parseObject(s));
        } catch (Exception e) {
            throw new ParseException(s + ClientResourceBundle.getString("logics.classes.can.not.be.converted.to.date"), 0);
        }
    }

    @Override
    public String formatString(Object obj) {
        if (obj != null) {
            return getDefaultFormat().format(DateConverter.stampToDate((Timestamp) obj));
        }
        else return "";
    }

    @Override
    public String toString() {
        return ClientResourceBundle.getString("logics.classes.date.with.time");
    }

    @Override
    public int getPreferredWidth(int prefCharWidth, FontMetrics fontMetrics) {
        return 115;
    }
}
