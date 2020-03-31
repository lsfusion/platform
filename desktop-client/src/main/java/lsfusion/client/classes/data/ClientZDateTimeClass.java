package lsfusion.client.classes.data;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.classes.controller.PropertyEditor;
import lsfusion.client.form.property.cell.classes.controller.ZDateTimePropertyEditor;
import lsfusion.client.form.property.cell.classes.view.ZDateTimePropertyRenderer;
import lsfusion.client.form.property.cell.view.PropertyRenderer;
import lsfusion.client.view.MainFrame;
import lsfusion.interop.classes.DataType;

import java.text.DateFormat;
import java.text.Format;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;

import static lsfusion.base.DateConverter.*;

public class ClientZDateTimeClass extends ClientDateTimeClass {
    public final static ClientZDateTimeClass instance = new ClientZDateTimeClass();

    public byte getTypeId() {
        return DataType.ZDATETIME;
    }

    public PropertyRenderer getRendererComponent(ClientPropertyDraw property) {
        return new ZDateTimePropertyRenderer(property);
    }

    @Override
    protected SimpleDateFormat getEditFormat(Format format) {
        return createDateTimeEditFormat((DateFormat) format);
    }

    public PropertyEditor getDataClassEditorComponent(Object value, ClientPropertyDraw property) {
        return new ZDateTimePropertyEditor(value, getEditFormat(property), property);
    }

    public Object parseString(String s) throws ParseException {
        try {
            return sqlTimestampToInstant(dateToStamp((Date) MainFrame.dateTimeFormat.parseObject(s)));
        } catch (Exception e) {
            throw new ParseException(s + ClientResourceBundle.getString("logics.classes.can.not.be.converted.to.date"), 0);
        }
    }

    @Override
    public String formatString(Object obj) {
        return obj != null ? MainFrame.dateTimeFormat.format(instantToSqlTimestamp((Instant) obj)) : "";
    }

    @Override
    public String toString() {
        return ClientResourceBundle.getString("logics.classes.date.with.time.with.zone");
    }
}
