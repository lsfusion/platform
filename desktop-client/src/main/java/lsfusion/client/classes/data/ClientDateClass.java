package lsfusion.client.classes.data;

import lsfusion.base.BaseUtils;
import lsfusion.base.DateConverter;
import lsfusion.client.ClientResourceBundle;
import lsfusion.client.classes.ClientTypeClass;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.classes.controller.DatePropertyEditor;
import lsfusion.client.form.property.cell.classes.controller.PropertyEditor;
import lsfusion.client.form.property.cell.classes.view.DatePropertyRenderer;
import lsfusion.client.form.property.cell.view.PropertyRenderer;
import lsfusion.client.form.property.table.view.AsyncChangeInterface;
import lsfusion.client.view.MainFrame;
import lsfusion.interop.classes.DataType;

import java.awt.*;
import java.text.DateFormat;
import java.text.Format;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static lsfusion.base.DateConverter.*;
import static lsfusion.client.form.property.cell.EditBindingMap.EditEventFilter;

public class ClientDateClass extends ClientFormatClass<SimpleDateFormat> implements ClientTypeClass {

    public final static ClientDateClass instance = new ClientDateClass();

    public byte getTypeId() {
        return DataType.DATE;
    }

    @Override
    protected Object getDefaultWidthValue() {
        return MainFrame.wideFormattableDate;
    }

    @Override
    public int getFullWidthString(String widthString, FontMetrics fontMetrics, ClientPropertyDraw propertyDraw) {
        int result = super.getFullWidthString(widthString, fontMetrics, propertyDraw);
        if(propertyDraw.isEditableChangeAction()) // добавляем кнопку если не readonly
            result += MainFrame.getIntUISize(18);
        return result;
    }

    public SimpleDateFormat getDefaultFormat() {
        return MainFrame.tFormats.date;
    }

    @Override
    public SimpleDateFormat createUserFormat(String pattern) {
        return new SimpleDateFormat(pattern);
    }

    public PropertyRenderer getRendererComponent(ClientPropertyDraw property) {
        return new DatePropertyRenderer(property);
    }

    @Override
    protected SimpleDateFormat getEditFormat(Format format, boolean width) {
        SimpleDateFormat result;
        if (!(format instanceof SimpleDateFormat)) {
            //use default pattern
            result = new SimpleDateFormat("dd.MM.yy");
        } else {
            if (!width) {
                String pattern = BaseUtils.getValidEditDateFormat(((SimpleDateFormat) format).toPattern(), false);
                format = pattern != null ? new SimpleDateFormat(pattern) : DateFormat.getDateInstance(DateFormat.SHORT);
            }
            result = createDateEditFormat((SimpleDateFormat) format);
            result.set2DigitYearStart(((SimpleDateFormat) format).get2DigitYearStart());
        }

        return result;
    }

    @Override
    public PropertyEditor getDataClassEditorComponent(Object value, ClientPropertyDraw property, AsyncChangeInterface asyncChange) {
        return new DatePropertyEditor(value, getEditFormat(property), property);
    }

    public LocalDate parseString(String s) throws ParseException {
        try {
            try {
                return LocalDate.parse(s, MainFrame.tFormats.dateParser);
            } catch (Exception ignored) {
            }
            LocalDateTime result = DateConverter.smartParse(s);
            return result != null ? result.toLocalDate() : null;
        } catch (Exception e) {
            throw new ParseException(s +  ClientResourceBundle.getString("logics.classes.can.not.be.converted.to.date"), 0);
        }
    }

    @Override
    public String formatString(Object obj) {
        return obj != null ? ((LocalDate) obj).format(MainFrame.tFormats.dateFormatter) : "";
    }

    @Override
    public String toString() {
        return ClientResourceBundle.getString("logics.classes.date");
    }

    @Override
    public EditEventFilter getEditEventFilter() {
        return ClientIntegralClass.numberEditEventFilter;
    }
}
