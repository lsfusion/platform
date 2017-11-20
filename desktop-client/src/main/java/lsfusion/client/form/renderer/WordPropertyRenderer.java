package lsfusion.client.form.renderer;

import lsfusion.client.SwingUtils;
import lsfusion.client.logics.ClientPropertyDraw;
import org.apache.poi.poifs.filesystem.DocumentFactoryHelper;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public class WordPropertyRenderer extends FilePropertyRenderer {
    public WordPropertyRenderer(ClientPropertyDraw property) {
        super(property);
    }

    public void setValue(Object value) {
        super.setValue(value);
        
        if (value != null) {
            String extension = "doc";
            try {
                if (DocumentFactoryHelper.hasOOXMLHeader(new ByteArrayInputStream((byte[]) value))) {
                    extension = "docx";
                }
            } catch (IOException ignored) {}
            getComponent().setIcon(SwingUtils.getSystemIcon(extension));
        }
    }
}
