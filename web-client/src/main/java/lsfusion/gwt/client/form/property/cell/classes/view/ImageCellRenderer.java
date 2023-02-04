package lsfusion.gwt.client.form.property.cell.classes.view;

import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.classes.data.GImageType;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.view.FileBasedCellRenderer;

public class ImageCellRenderer extends FileBasedCellRenderer {

    public ImageCellRenderer(GPropertyDraw property) {
        super(property);
    }

    @Override
    protected Element createImage(Object value) {
        return GwtClientUtils.createAppDownloadImage(value);
    }
}
