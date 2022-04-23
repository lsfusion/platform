package lsfusion.client.form.property.cell.classes.view;

import lsfusion.base.file.FileData;
import lsfusion.base.file.NamedFileData;
import lsfusion.client.base.SwingUtils;
import lsfusion.client.form.property.ClientPropertyDraw;

public class DynamicFormatFileRenderer extends FilePropertyRenderer {

    public DynamicFormatFileRenderer(ClientPropertyDraw property) {
        super(property);
    }

    public void setValue(Object value) {
        super.setValue(value);
        
        if (value != null) {
            String extension = value instanceof NamedFileData ? ((NamedFileData) value).getExtension() : ((FileData) value).getExtension();
            getComponent().setIcon(SwingUtils.getSystemIcon(extension));
        }
    }
}