package lsfusion.client.logics.classes;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.form.PropertyEditor;
import lsfusion.client.form.PropertyRenderer;
import lsfusion.client.form.editor.TimePropertyEditor;
import lsfusion.client.form.renderer.TimePropertyRenderer;
import lsfusion.client.logics.ClientPropertyDraw;
import lsfusion.interop.Data;

import java.awt.*;
import java.sql.Time;
import java.text.Format;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static lsfusion.base.DateConverter.safeDateToSql;
import static lsfusion.client.Main.timeFormat;
import static lsfusion.client.form.EditBindingMap.EditEventFilter;

public class ClientTimeClass extends ClientDataClass implements ClientTypeClass {

    public final static ClientTimeClass instance = new ClientTimeClass();

    private final String sID = "TimeClass";

    public String getPreferredMask() {
        try {
            return formatString(safeDateToSql(new java.util.Date()));
        } catch (ParseException pe) {
            throw new IllegalStateException("shouldn't happen", pe);
        }
    }

    protected PropertyEditor getDataClassEditorComponent(Object value, ClientPropertyDraw property) {
        return new TimePropertyEditor(value, (SimpleDateFormat) property.getFormat(), property.design);
    }

    public String getSID() {
        return sID;
    }

    public Format getDefaultFormat() {
        return timeFormat;
    }

    public PropertyRenderer getRendererComponent(ClientPropertyDraw property) {
        return new TimePropertyRenderer(property);
    }

    public Time parseString(String s) throws ParseException {
        try {
            return new Time(((Date) timeFormat.parseObject(s)).getTime());
        } catch (Exception e) {
            throw new ParseException(s + ClientResourceBundle.getString("logics.classes.can.not.be.converted.to.time"), 0);
        }
    }

    public String formatString(Object obj) throws ParseException {
        if (obj != null) {
            return timeFormat.format(obj);
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

    @Override
    public EditEventFilter getEditEventFilter() {
        return ClientIntegralClass.numberEditEventFilter;
    }
}
