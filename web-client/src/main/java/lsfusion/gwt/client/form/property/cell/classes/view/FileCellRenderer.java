package lsfusion.gwt.client.form.property.cell.classes.view;

import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.view.FileBasedCellRenderer;

import java.util.function.Consumer;

public class FileCellRenderer extends FileBasedCellRenderer {
    public FileCellRenderer(GPropertyDraw property) {
        super(property);
    }

    @Override
    protected void setImage(Object value, Consumer<String> consumer) {
        GwtClientUtils.setThemeImage(value == null ? ICON_EMPTY : ICON_FILE, consumer);
    }

    @Override
    public boolean isAutoDynamicHeight() {
        return false;
    }
}
