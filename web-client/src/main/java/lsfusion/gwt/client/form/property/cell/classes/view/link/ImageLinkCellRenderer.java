package lsfusion.gwt.client.form.property.cell.classes.view.link;

import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.classes.view.ImageCellRenderer;

import java.util.function.Consumer;

public class ImageLinkCellRenderer extends ImageCellRenderer {
    public ImageLinkCellRenderer(GPropertyDraw property) {
        super(property);
    }

    @Override
    protected void setImage(Object value, Consumer<String> consumer) {
        if(value instanceof String)
            consumer.accept((String) value);
        else
            GwtClientUtils.setThemeImage(ICON_EMPTY, consumer);
    }
}