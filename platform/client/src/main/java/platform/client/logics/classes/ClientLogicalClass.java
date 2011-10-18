package platform.client.logics.classes;

import platform.base.BaseUtils;
import platform.client.ClientResourceBundle;
import platform.client.form.PropertyEditorComponent;
import platform.client.form.PropertyRendererComponent;
import platform.client.form.editor.LogicalPropertyEditor;
import platform.client.form.renderer.LogicalPropertyRenderer;
import platform.gwt.view.classes.GLogicalType;
import platform.gwt.view.classes.GType;
import platform.interop.ComponentDesign;
import platform.interop.Data;

import java.awt.*;
import java.text.Format;
import java.text.ParseException;

public class ClientLogicalClass extends ClientDataClass implements ClientTypeClass {

    public final static ClientLogicalClass instance = new ClientLogicalClass();

    private final String sID = "LogicalClass";

    @Override
    public String getSID() {
        return sID;
    }

    public byte getTypeId() {
        return Data.LOGICAL;
    }

    @Override
    public int getMinimumWidth(int minCharWidth, FontMetrics fontMetrics) {
        return 25;
    }

    public int getPreferredWidth(int prefCharWidth, FontMetrics fontMetrics) {
        return 25;
    }

    public String getPreferredMask() {
        return "";
    }

    public Format getDefaultFormat() {
        return null;
    }

    public PropertyRendererComponent getRendererComponent(Format format, String caption, ComponentDesign design) {
        return new LogicalPropertyRenderer();
    }

    public PropertyEditorComponent getComponent(Object value, Format format, ComponentDesign design) {
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

    @Override
    public GType getGwtType() {
        return GLogicalType.instance;
    }
}
