package lsfusion.client.logics.classes;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.form.PropertyEditor;
import lsfusion.client.form.PropertyRenderer;
import lsfusion.client.form.property.classes.editor.DatePropertyEditor;
import lsfusion.client.form.property.classes.renderer.DatePropertyRenderer;
import lsfusion.client.logics.ClientPropertyDraw;
import lsfusion.interop.form.property.DataType;

import java.awt.*;
import java.text.DateFormat;
import java.text.Format;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import static lsfusion.base.DateConverter.createDateEditFormat;
import static lsfusion.base.DateConverter.safeDateToSql;
import static lsfusion.client.Main.*;
import static lsfusion.client.form.EditBindingMap.EditEventFilter;

public class ClientDateClass extends ClientFormatClass<SimpleDateFormat> implements ClientTypeClass {

    public final static ClientDateClass instance = new ClientDateClass();

    public byte getTypeId() {
        return DataType.DATE;
    }

    @Override
    protected Object getDefaultWidthValue() {
        return wideFormattableDate;
    }

    @Override
    public int getFullWidthString(String widthString, FontMetrics fontMetrics, ClientPropertyDraw propertyDraw) {
        int result = super.getFullWidthString(widthString, fontMetrics, propertyDraw);
        if(propertyDraw.isEditableChangeAction()) // добавляем кнопку если не readonly
            result += 21;
        return result;
    }

    public Format getDefaultFormat() {
        return dateFormat;
    }

    @Override
    public SimpleDateFormat createUserFormat(String pattern) {
        return new SimpleDateFormat(pattern);
    }

    public PropertyRenderer getRendererComponent(ClientPropertyDraw property) {
        return new DatePropertyRenderer(property);
    }

    @Override
    protected SimpleDateFormat getEditFormat(Format format) {
        return createDateEditFormat((DateFormat) format);
    }

    public PropertyEditor getDataClassEditorComponent(Object value, ClientPropertyDraw property) {
        return new DatePropertyEditor(value, getEditFormat(property), property.design);
    }

    public Object parseString(String s) throws ParseException {
        try {
            return safeDateToSql(dateFormat.parse(s));
        } catch (Exception e) {
            throw new ParseException(s +  ClientResourceBundle.getString("logics.classes.can.not.be.converted.to.date"), 0);
        }
    }

    @Override
    public String formatString(Object obj) throws ParseException {
        if (obj != null) {
            return dateFormat.format(obj);
        }
        else return "";
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
