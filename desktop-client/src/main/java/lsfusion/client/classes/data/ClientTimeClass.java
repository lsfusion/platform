package lsfusion.client.classes.data;

import lsfusion.base.TimeConverter;
import lsfusion.client.ClientResourceBundle;
import lsfusion.client.classes.ClientTypeClass;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.classes.controller.PropertyEditor;
import lsfusion.client.form.property.cell.classes.controller.TimePropertyEditor;
import lsfusion.client.form.property.cell.classes.view.TimePropertyRenderer;
import lsfusion.client.form.property.cell.view.PropertyRenderer;
import lsfusion.client.form.property.table.view.AsyncChangeInterface;
import lsfusion.client.view.MainFrame;
import lsfusion.interop.classes.DataType;

import java.sql.Date;
import java.sql.Time;
import java.text.Format;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

import static lsfusion.base.DateConverter.createDateEditFormat;
import static lsfusion.base.TimeConverter.localTimeToSqlTime;
import static lsfusion.base.TimeConverter.sqlTimeToLocalTime;
import static lsfusion.client.form.property.cell.EditBindingMap.EditEventFilter;

public class ClientTimeClass extends ClientFormatClass<SimpleDateFormat> implements ClientTypeClass {

    public final static ClientTimeClass instance = new ClientTimeClass();

    @Override
    protected Object getDefaultWidthValue() {
        return MainFrame.wideFormattableDateTime;
    }

    @Override
    protected SimpleDateFormat getEditFormat(Format format, boolean width) {
        if (!(format instanceof SimpleDateFormat)) {
            //use default pattern
            return new SimpleDateFormat("HH:mm:ss");
        }
        return createDateEditFormat((SimpleDateFormat) format);
    }

    @Override
    protected PropertyEditor getDataClassEditorComponent(Object value, ClientPropertyDraw property, AsyncChangeInterface asyncChange) {
        return new TimePropertyEditor(value, getEditFormat(property), property);
    }

    public Format getDefaultFormat() {
        return MainFrame.tFormats.time;
    }

    @Override
    public SimpleDateFormat createUserFormat(String pattern) {
        return new SimpleDateFormat(pattern);
    }

    public PropertyRenderer getRendererComponent(ClientPropertyDraw property) {
        return new TimePropertyRenderer(property);
    }

    public LocalTime parseString(String s) throws ParseException {
        try {
            try {
                return LocalTime.parse(s, MainFrame.tFormats.timeParser);
            } catch (Exception ignored) {
            }
            return TimeConverter.smartParse(s);
        } catch (Exception e) {
            throw new ParseException(s + ClientResourceBundle.getString("logics.classes.can.not.be.converted.to.time"), 0);
        }
    }

    public String formatString(Object obj) {
        return obj != null ? ((LocalTime) obj).format(MainFrame.tFormats.timeFormatter) : "";
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
