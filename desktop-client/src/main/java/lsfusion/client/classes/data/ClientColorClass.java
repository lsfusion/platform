package lsfusion.client.classes.data;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.classes.ClientTypeClass;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.classes.controller.ColorPropertyEditor;
import lsfusion.client.form.property.cell.classes.controller.PropertyEditor;
import lsfusion.client.form.property.cell.classes.view.ColorPropertyRenderer;
import lsfusion.client.form.property.cell.view.PropertyRenderer;
import lsfusion.client.form.property.table.view.CellTableInterface;
import lsfusion.interop.classes.DataType;

import java.awt.*;
import java.text.ParseException;

public class ClientColorClass extends ClientDataClass implements ClientTypeClass {

    public final static ClientColorClass instance = new ClientColorClass();

    @Override
    public PropertyRenderer getRendererComponent(ClientPropertyDraw property) {
        return new ColorPropertyRenderer(property);
    }

    @Override
    protected PropertyEditor getDataClassEditorComponent(Object value, ClientPropertyDraw property, CellTableInterface table) {
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
