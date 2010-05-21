package platform.client.logics.classes;

import platform.client.form.*;
import platform.client.form.renderer.StringPropertyRenderer;
import platform.client.form.editor.StringPropertyEditor;
import platform.base.BaseUtils;
import platform.interop.CellDesign;

import java.io.DataInputStream;
import java.io.IOException;
import java.text.Format;

public class ClientStringClass extends ClientDataClass {

    private int length;

    public ClientStringClass(DataInputStream inStream) throws IOException {
        super(inStream);

        length = inStream.readInt();
    }

    @Override
    public String getMinimumMask() {
        return BaseUtils.replicate('0', length / 5);
    }

    public String getPreferredMask() {
        return BaseUtils.replicate('0', length);
    }

    public Format getDefaultFormat() {
        return null;
    }

    public PropertyRendererComponent getRendererComponent(Format format, String caption, CellDesign design) { return new StringPropertyRenderer(format, design); }
    public PropertyEditorComponent getComponent(Object value, Format format, CellDesign design) { return new StringPropertyEditor(length, value, design); }
}
