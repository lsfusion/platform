package lsfusion.client.logics.classes;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.form.PropertyEditor;
import lsfusion.client.form.PropertyRenderer;
import lsfusion.client.form.editor.ColorPropertyEditor;
import lsfusion.client.form.renderer.ColorPropertyRenderer;
import lsfusion.client.logics.ClientPropertyDraw;
import lsfusion.interop.Data;

import java.awt.*;
import java.text.Format;
import java.text.ParseException;

public class ClientColorClass extends ClientDataClass implements ClientTypeClass {

    public final static ClientColorClass instance = new ClientColorClass();

    private final String sID = "ColorClass";

    @Override
    public String getPreferredMask() {
        return "";
    }

    @Override
    public String getSID() {
        return sID;
    }

    @Override
    public Format getDefaultFormat() {
        return null;
    }

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
        return Data.COLOR;
    }

    public String toString() {
        return ClientResourceBundle.getString("logics.classes.color");
    }

    @Override
    public int getPreferredWidth(int prefCharWidth, FontMetrics fontMetrics) {
        return 50;
    }
}
