package lsfusion.client.logics.classes;

import lsfusion.base.BaseUtils;
import lsfusion.client.ClientResourceBundle;
import lsfusion.client.form.PropertyEditor;
import lsfusion.client.form.PropertyRenderer;
import lsfusion.client.form.editor.LogicalPropertyEditor;
import lsfusion.client.form.renderer.LogicalPropertyRenderer;
import lsfusion.client.logics.ClientPropertyDraw;
import lsfusion.interop.form.property.DataType;

import java.awt.*;
import java.text.ParseException;

public class ClientLogicalClass extends ClientDataClass implements ClientTypeClass {

    public final static ClientLogicalClass instance = new ClientLogicalClass();

    public byte getTypeId() {
        return DataType.LOGICAL;
    }

    @Override
    public int getDefaultWidth(FontMetrics fontMetrics, ClientPropertyDraw property) {
        return 25;
    }

    public PropertyRenderer getRendererComponent(ClientPropertyDraw property) {
        return new LogicalPropertyRenderer(property);
    }

    public PropertyEditor getDataClassEditorComponent(Object value, ClientPropertyDraw property) {
        return new LogicalPropertyEditor(value);
    }

    public Object parseString(String s) throws ParseException {
        try {
            return BaseUtils.nullBoolean(Boolean.parseBoolean(s));
        } catch (NumberFormatException nfe) {
            throw new ParseException(s + ClientResourceBundle.getString("logics.classes.can.not.be.converted.to.boolean"), 0);
        }
    }

    @Override
    public String formatString(Object obj) {
        return obj.toString();
    }

    @Override
    public String toString() {
        return ClientResourceBundle.getString("logics.classes.boolean");
    }
}
