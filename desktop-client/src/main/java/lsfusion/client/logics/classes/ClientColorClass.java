package lsfusion.client.logics.classes;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.form.PropertyEditor;
import lsfusion.client.form.PropertyRenderer;
import lsfusion.client.form.property.classes.editor.ColorPropertyEditor;
import lsfusion.client.form.property.classes.renderer.ColorPropertyRenderer;
import lsfusion.client.logics.ClientPropertyDraw;
import lsfusion.interop.form.property.DataType;

import java.awt.*;
import java.text.ParseException;

public class ClientColorClass extends ClientDataClass implements ClientTypeClass {

    public final static ClientColorClass instance = new ClientColorClass();

    public static Color getDefaultValue() {
        return Color.WHITE;
    }

    @Override
    public PropertyRenderer getRendererComponent(ClientPropertyDraw property) {
        return new ColorPropertyRenderer(property);
    }

    @Override
    protected PropertyEditor getDataClassEditorComponent(Object value, ClientPropertyDraw property) {
        return new ColorPropertyEditor(value);
    }

    @Override
    public Object parseString(String s) throws ParseException {
        try {
            return Color.decode("#" + s.substring(s.length() - 6, s.length()));
        } catch (Exception e) {
            throw new RuntimeException("error parsing color");
        }
    }

    @Override
    public String formatString(Object obj) throws ParseException {
        return "#" + Integer.toHexString(((Color) obj).getRGB()).substring(2, 8);
    }

    @Override
    public byte getTypeId() {
        return DataType.COLOR;
    }

    public String toString() {
        return ClientResourceBundle.getString("logics.classes.color");
    }

    @Override
    public int getDefaultWidth(FontMetrics fontMetrics, ClientPropertyDraw property) {
        return 40;
    }
}
