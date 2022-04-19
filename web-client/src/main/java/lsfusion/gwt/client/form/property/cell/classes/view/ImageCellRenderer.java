package lsfusion.gwt.client.form.property.cell.classes.view;

import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.classes.data.GImageType;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.view.FileBasedCellRenderer;

import java.util.function.Consumer;

public class ImageCellRenderer extends FileBasedCellRenderer {

    public ImageCellRenderer(GPropertyDraw property) {
        super(property);
    }

    @Override
    protected String getDefaultVertAlignment() {
        return "stretch";
    }

    @Override
    protected void setImage(Object value, Consumer<String> consumer) {
        String extension = ((GImageType) property.baseType).extension;
        if (value instanceof String)
            consumer.accept(GwtClientUtils.getAppDownloadURL((String) value, null, extension));
        else
            GwtClientUtils.setThemeImage(ICON_EMPTY, consumer);
    }
}
