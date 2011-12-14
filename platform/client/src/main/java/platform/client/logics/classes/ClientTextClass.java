package platform.client.logics.classes;

import platform.client.ClientResourceBundle;
import platform.client.form.ClientFormController;
import platform.client.form.PropertyEditorComponent;
import platform.client.form.PropertyRendererComponent;
import platform.client.form.editor.TextPropertyEditor;
import platform.client.form.renderer.TextPropertyRenderer;
import platform.client.logics.ClientPropertyDraw;
import platform.gwt.view.classes.GTextType;
import platform.gwt.view.classes.GType;
import platform.interop.ComponentDesign;
import platform.interop.Data;

import java.awt.*;
import java.io.IOException;
import java.text.Format;
import java.text.ParseException;

public class ClientTextClass extends ClientDataClass implements ClientTypeClass {

    public final static ClientTextClass instance = new ClientTextClass();

    private final String sID = "TextClass";

    @Override
    public String getSID() {
        return sID;
    }

    public byte getTypeId() {
        return Data.TEXT;
    }

    public String getMinimumMask() {
        return "999 999";
    }

    public String getPreferredMask() {
        return "9 999 999";
    }

    public Format getDefaultFormat() {
        return null;
    }

    @Override
    public int getPreferredHeight(FontMetrics fontMetrics) {
        return 4 * (fontMetrics.getHeight() + 1);
    }

    @Override
    public int getMaximumHeight(FontMetrics fontMetrics) {
        return Integer.MAX_VALUE;
    }

    @Override
    public int getPreferredWidth(int prefCharWidth, FontMetrics fontMetrics) {
        return fontMetrics.charWidth('0') * 25;//stringWidth(getPreferredMask()) + 8;
    }

    public PropertyRendererComponent getRendererComponent(Format format, String caption, ComponentDesign design) {
        return new TextPropertyRenderer(format, design);
    }


    @Override
    public PropertyEditorComponent getEditorComponent(Component ownerComponent, ClientFormController form, ClientPropertyDraw property, Object value) throws IOException, ClassNotFoundException {
        return new TextPropertyEditor(ownerComponent, value, property.design);
    }

    public PropertyEditorComponent getComponent(Object value, ClientPropertyDraw property) {
        return new TextPropertyEditor(value, property.design);
    }

    public Object parseString(String s) throws ParseException {
        return s;
    }

    @Override
    public String formatString(Object obj) {
        return obj.toString();
    }

    @Override
    public String toString() {
        return ClientResourceBundle.getString("logics.classes.text");
    }

    @Override
    public GType getGwtType() {
        return GTextType.instance;
    }
}
