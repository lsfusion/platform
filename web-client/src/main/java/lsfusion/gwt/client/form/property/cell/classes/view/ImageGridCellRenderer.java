package lsfusion.gwt.client.form.property.cell.classes.view;

import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.classes.data.GImageType;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.view.FileBasedGridCellRenderer;

import java.util.function.Consumer;

public class ImageGridCellRenderer extends FileBasedGridCellRenderer {
    protected GPropertyDraw property;

    public ImageGridCellRenderer(GPropertyDraw property) {
        super(property);
    }

    @Override
    protected void setFilePath(Object value, Consumer<String> modifier) {
        modifier.accept( value instanceof String && !value.equals("null") ?
                GwtClientUtils.getDownloadURL((String) value, null, ((GImageType) property.baseType).extension, false) :
                GwtClientUtils.getModuleImagePath(ICON_EMPTY));
    }
}
