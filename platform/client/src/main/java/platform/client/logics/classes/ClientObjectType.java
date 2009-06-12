package platform.client.logics.classes;

import platform.client.form.PropertyRendererComponent;
import platform.client.form.renderer.IntegerPropertyRenderer;

import java.text.Format;
import java.text.NumberFormat;

public class ClientObjectType implements ClientType {

    public int getMinimumWidth() { return getPreferredWidth(); }
    public int getPreferredWidth() { return 45; }
    public int getMaximumWidth() { return getPreferredWidth(); }

    public Format getDefaultFormat() {
        return NumberFormat.getInstance();
    }

    public PropertyRendererComponent getRendererComponent(Format format) { return new IntegerPropertyRenderer(format); }
}
