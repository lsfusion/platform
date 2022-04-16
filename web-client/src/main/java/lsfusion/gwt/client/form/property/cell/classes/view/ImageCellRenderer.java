package lsfusion.gwt.client.form.property.cell.classes.view;

import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.classes.data.GImageType;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.view.FileBasedCellRenderer;

public class ImageCellRenderer extends FileBasedCellRenderer {

    public ImageCellRenderer(GPropertyDraw property) {
        super(property);
    }

    @Override
    protected String getDefaultVertAlignment() {
        return "stretch";
    }

    @Override
    protected String getFilePath(Object value) {
        String extension = ((GImageType) property.baseType).extension;
        return value instanceof String ?
                GwtClientUtils.getAppDownloadURL((String) value, null, extension) :
                GwtClientUtils.getStaticImageURL(ICON_EMPTY);
    }
}
