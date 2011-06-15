package platform.client.logics.classes;

import platform.client.form.PropertyEditorComponent;
import platform.client.form.PropertyRendererComponent;
import platform.client.form.editor.DateTimePropertyEditor;
import platform.client.form.renderer.DateTimePropertyRenderer;
import platform.gwt.view.classes.GDateType;
import platform.gwt.view.classes.GType;
import platform.interop.ComponentDesign;
import platform.interop.Data;

import java.text.Format;
import java.text.ParseException;
import java.text.SimpleDateFormat;

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

    public PropertyRendererComponent getRendererComponent(Format format, String caption, ComponentDesign design) {
        return new DateTimePropertyRenderer(format, design);
    }

    public PropertyEditorComponent getComponent(Object value, Format format, ComponentDesign design) {
        return new DateTimePropertyEditor(value, (SimpleDateFormat) format, design);
    }

    public Object parseString(String s) throws ParseException {
        try {
            return new SimpleDateFormat().parse(s);
        } catch (Exception e) {
            throw new ParseException(s + "не может быть конвертированно в Date.", 0);
        }
    }

    @Override
    public String toString() {
        return "Дата со временем";
    }

    @Override
    public GType getGwtType() {
        return GDateType.instance;
    }
}
