package lsfusion.client.classes.data;

import lsfusion.base.DateConverter;
import lsfusion.client.ClientResourceBundle;
import lsfusion.client.classes.ClientTypeClass;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.classes.controller.PropertyEditor;
import lsfusion.client.form.property.cell.classes.controller.TimePropertyEditor;
import lsfusion.client.form.property.cell.classes.view.TimePropertyRenderer;
import lsfusion.client.form.property.cell.view.PropertyRenderer;
import lsfusion.client.view.MainFrame;
import lsfusion.interop.classes.DataType;

import java.sql.Time;
import java.text.DateFormat;
import java.text.Format;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static lsfusion.client.form.property.cell.EditBindingMap.EditEventFilter;

public class ClientTimeClass extends ClientFormatClass<SimpleDateFormat> implements ClientTypeClass {

    public final static ClientTimeClass instance = new ClientTimeClass();

    @Override
    protected Object getDefaultWidthValue() {
        return MainFrame.wideFormattableDateTime;
    }

    @Override
    protected SimpleDateFormat getEditFormat(Format format) {
        return DateConverter.createTimeEditFormat((DateFormat)format);
    }

    protected PropertyEditor getDataClassEditorComponent(Object value, ClientPropertyDraw property) {
        return new TimePropertyEditor(value, getEditFormat(property), property);
    }

    public Format getDefaultFormat() {
        return MainFrame.timeFormat;
    }

    @Override
    public SimpleDateFormat createUserFormat(String pattern) {
        return new SimpleDateFormat(pattern);
    }

    public PropertyRenderer getRendererComponent(ClientPropertyDraw property) {
        return new TimePropertyRenderer(property);
    }

    public Time parseString(String s) throws ParseException {
        try {
            return new Time(((Date) MainFrame.timeFormat.parseObject(s)).getTime());
        } catch (Exception e) {
            throw new ParseException(s + ClientResourceBundle.getString("logics.classes.can.not.be.converted.to.time"), 0);
        }
    }

    public String formatString(Object obj) {
        if (obj != null) {
            return MainFrame.timeFormat.format(obj);
        }
        else return "";
    }

    public byte getTypeId() {
        return DataType.TIME;
    }

    public String toString() {
        return ClientResourceBundle.getString("logics.classes.time");
    }

    @Override
    public EditEventFilter getEditEventFilter() {
        return ClientIntegralClass.numberEditEventFilter;
    }
}
