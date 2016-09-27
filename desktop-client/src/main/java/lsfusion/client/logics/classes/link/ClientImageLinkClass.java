package lsfusion.client.logics.classes.link;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.form.PropertyEditor;
import lsfusion.client.form.PropertyRenderer;
import lsfusion.client.form.editor.LinkPropertyEditor;
import lsfusion.client.form.renderer.link.ImageLinkPropertyRenderer;
import lsfusion.client.logics.ClientPropertyDraw;
import lsfusion.interop.Data;

import java.awt.*;

public class ClientImageLinkClass extends ClientStaticFormatLinkClass {

    public final static ClientImageLinkClass instance = new ClientImageLinkClass(false);

    public ClientImageLinkClass(boolean multiple) {
        super(multiple);
    }

    public byte getTypeId() {
        return Data.IMAGELINK;
    }

    @Override
    public int getMaximumHeight(FontMetrics fontMetrics) {
        return Integer.MAX_VALUE;
    }

    @Override
    public int getMaximumWidth(int maxCharWidth, FontMetrics fontMetrics) {
        return Integer.MAX_VALUE;
    }

    public PropertyRenderer getRendererComponent(ClientPropertyDraw property) {
        return new ImageLinkPropertyRenderer(property);
    }

    @Override
    public PropertyEditor getDataClassEditorComponent(Object value, ClientPropertyDraw property) {
        return new LinkPropertyEditor(property, value);
    }

    @Override
    public String toString() {
        return ClientResourceBundle.getString("logics.classes.image.link");
    }
}