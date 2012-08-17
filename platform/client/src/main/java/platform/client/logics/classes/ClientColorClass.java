package platform.client.logics.classes;

import platform.client.ClientResourceBundle;
import platform.client.form.PropertyEditorComponent;
import platform.client.form.PropertyRendererComponent;
import platform.client.form.editor.ColorPropertyEditor;
import platform.client.form.renderer.ColorPropertyRenderer;
import platform.client.logics.ClientPropertyDraw;
import platform.interop.Data;

import java.awt.*;
import java.text.Format;
import java.text.ParseException;

public class ClientColorClass extends ClientDataClass implements ClientTypeClass {

    public final static ClientColorClass instance = new ClientColorClass();

    private final String sID = "TimeClass";

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
    public PropertyRendererComponent getRendererComponent(ClientPropertyDraw property) {
        return new ColorPropertyRenderer(property);
    }

    @Override
    protected PropertyEditorComponent getDataClassEditorComponent(Object value, ClientPropertyDraw property) {
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
        return ClientResourceBundle.getString("logics.classes.time");
    }

    @Override
    public int getPreferredWidth(int prefCharWidth, FontMetrics fontMetrics) {
        return 50;
    }
}
