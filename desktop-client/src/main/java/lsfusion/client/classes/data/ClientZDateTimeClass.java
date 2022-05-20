package lsfusion.client.classes.data;

import lsfusion.base.DateConverter;
import lsfusion.client.ClientResourceBundle;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.classes.controller.PropertyEditor;
import lsfusion.client.form.property.cell.classes.controller.ZDateTimePropertyEditor;
import lsfusion.client.form.property.cell.classes.view.ZDateTimePropertyRenderer;
import lsfusion.client.form.property.cell.view.PropertyRenderer;
import lsfusion.client.form.property.table.view.AsyncChangeInterface;
import lsfusion.client.view.MainFrame;
import lsfusion.interop.classes.DataType;

import java.text.Format;
import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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
    public PropertyEditor getDataClassEditorComponent(Object value, ClientPropertyDraw property, AsyncChangeInterface asyncChange) {
        return new ZDateTimePropertyEditor(value, getEditFormat(property), property);
    }

    @Override
    public Format getDefaultFormat() {
        return MainFrame.tFormats.zDateTime;
    }

    public Instant parseString(String s) throws ParseException {
        try {
            try {
                return ZonedDateTime.parse(s, MainFrame.tFormats.zDateTimeParser).toInstant();
            } catch (DateTimeParseException ignored) {
            }
            return DateConverter.smartParseInstant(s);
        } catch (Exception e) {
            throw new ParseException(s + ClientResourceBundle.getString("logics.classes.can.not.be.converted.to.date"), 0);
        }
    }

    @Override
    public String formatString(Object obj) {
        return obj != null ? MainFrame.tFormats.zDateTimeFormatter.format((Instant) obj) : "";
    }

    @Override
    public String toString() {
        return ClientResourceBundle.getString("logics.classes.date.with.time.with.zone");
    }
}
