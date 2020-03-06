package lsfusion.gwt.client.form.property.cell.classes.view;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.view.FileBasedGridCellRenderer;

import java.util.function.Consumer;

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
    protected void setFilePath(Object value, Consumer<String> modifier) {
        GwtClientUtils.setThemeImage(value == null ? ICON_EMPTY : ICON_FILE, modifier, true);
    }
}
