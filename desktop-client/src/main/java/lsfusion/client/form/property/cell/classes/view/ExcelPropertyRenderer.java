package lsfusion.client.form.property.cell.classes.view;

import lsfusion.base.file.RawFileData;
import lsfusion.client.base.SwingUtils;
import lsfusion.client.form.property.ClientPropertyDraw;
import org.apache.poi.poifs.filesystem.DocumentFactoryHelper;

import java.io.IOException;

public class ExcelPropertyRenderer extends FilePropertyRenderer {

    public ExcelPropertyRenderer(ClientPropertyDraw property) {
        super(property);
    }

    public void setValue(Object value) {
        super.setValue(value);
        
        if (value != null) {
            String extension = "xls";
            try {
                if (DocumentFactoryHelper.hasOOXMLHeader(((RawFileData) value).getInputStream())) {
                    extension = "xlsx";
                }
            } catch (IOException ignored) {}
            getComponent().setIcon(SwingUtils.getSystemIcon(extension));
        }
    }
}
