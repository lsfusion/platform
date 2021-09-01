package lsfusion.client.classes.data;

import lsfusion.base.BaseUtils;
import lsfusion.client.ClientResourceBundle;
import lsfusion.client.classes.ClientTypeClass;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.classes.controller.DateTimePropertyEditor;
import lsfusion.client.form.property.cell.classes.controller.PropertyEditor;
import lsfusion.client.form.property.cell.classes.view.DateTimePropertyRenderer;
import lsfusion.client.form.property.cell.view.PropertyRenderer;
import lsfusion.client.form.property.table.view.AsyncChangeInterface;
import lsfusion.client.view.MainFrame;
import lsfusion.interop.classes.DataType;

import java.awt.*;
import java.text.DateFormat;
import java.text.Format;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;

import static lsfusion.base.DateConverter.*;
import static lsfusion.client.form.property.cell.EditBindingMap.EditEventFilter;

public class ClientDateTimeClass extends ClientFormatClass<SimpleDateFormat> implements ClientTypeClass {
    public final static ClientDateTimeClass instance = new ClientDateTimeClass();

    public byte getTypeId() {
        return DataType.DATETIME;
    }

    @Override
    protected Object getDefaultWidthValue() {
        return MainFrame.wideFormattableDateTime;
    }

    @Override
    public int getFullWidthString(String widthString, FontMetrics fontMetrics, ClientPropertyDraw propertyDraw) {
        int result = super.getFullWidthString(widthString, fontMetrics, propertyDraw);
        if(propertyDraw.isEditableChangeAction()) // добавляем кнопку если не readonly
            result += MainFrame.getIntUISize(18);
        return result;
    }

    public Format getDefaultFormat() {
        return MainFrame.dateTimeFormat;
    }

    @Override
    public SimpleDateFormat createUserFormat(String pattern) {
        return new SimpleDateFormat(pattern);
    }

    public PropertyRenderer getRendererComponent(ClientPropertyDraw property) {
        return new DateTimePropertyRenderer(property);
    }

    @Override
    protected SimpleDateFormat getEditFormat(Format format, boolean width) {
        SimpleDateFormat result;
        if (!(format instanceof SimpleDateFormat)) {
            //use default pattern
            result = new SimpleDateFormat("dd.MM.yy HH:mm:ss");
        } else {
            if (!width) {
                String pattern = BaseUtils.getValidEditDateFormat(((SimpleDateFormat) format).toPattern(), true);
                format = pattern != null ? new SimpleDateFormat(pattern) : DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM);
            }
            result = createDateEditFormat((SimpleDateFormat) format);
            result.set2DigitYearStart(((SimpleDateFormat) format).get2DigitYearStart());
        }

        return result;
    }

    @Override
    public PropertyEditor getDataClassEditorComponent(Object value, ClientPropertyDraw property, AsyncChangeInterface asyncChange) {
        return new DateTimePropertyEditor(value, getEditFormat(property), property);
    }

    public Object parseString(String s) throws ParseException {
        try {
            return sqlTimestampToLocalDateTime(dateToStamp((Date) MainFrame.dateTimeFormat.parseObject(s)));
        } catch (Exception e) {
            throw new ParseException(s + ClientResourceBundle.getString("logics.classes.can.not.be.converted.to.date"), 0);
        }
    }

    @Override
    public String formatString(Object obj) {
        return obj != null ? MainFrame.dateTimeFormat.format(localDateTimeToSqlTimestamp((LocalDateTime) obj)) : "";
    }

    @Override
    public String toString() {
        return ClientResourceBundle.getString("logics.classes.date.with.time");
    }

    @Override
    public EditEventFilter getEditEventFilter() {
        return ClientIntegralClass.numberEditEventFilter;
    }
}
