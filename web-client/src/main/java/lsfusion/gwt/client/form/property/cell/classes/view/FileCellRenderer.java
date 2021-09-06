package lsfusion.gwt.client.form.property.cell.classes.view;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.view.FileBasedCellRenderer;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;

public class FileCellRenderer extends FileBasedCellRenderer {
    public FileCellRenderer(GPropertyDraw property) {
        super(property);
    }

    @Override
    protected String getFilePath(Object value) {
        StringBuilder sb = new StringBuilder();
        GwtClientUtils.setThemeImage(value == null ? ICON_EMPTY : ICON_FILE, sb::append);
        return sb.toString();
    }

    @Override
    public boolean isAutoDynamicHeight() {
        return false;
    }
}
