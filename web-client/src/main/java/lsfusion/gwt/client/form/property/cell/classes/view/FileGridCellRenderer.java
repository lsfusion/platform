package lsfusion.gwt.client.form.property.cell.classes.view;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.view.FileBasedGridCellRenderer;

public class FileGridCellRenderer extends FileBasedGridCellRenderer {
    public FileGridCellRenderer(GPropertyDraw property) {
        super(property);
    }

    @Override
    public void renderStatic(Element element, GFont font, boolean isSingle) {
        super.renderStatic(element, font, isSingle);
        element.getStyle().setHeight(100, Style.Unit.PCT);
    }

    @Override
    protected String getFilePath(Object value) {
        StringBuilder sb = new StringBuilder();
        GwtClientUtils.setThemeImage(value == null ? ICON_EMPTY : ICON_FILE, sb::append);
        return sb.toString();
    }
}
