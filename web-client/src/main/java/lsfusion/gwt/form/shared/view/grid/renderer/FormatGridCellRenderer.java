package lsfusion.gwt.form.shared.view.grid.renderer;

import com.google.gwt.dom.client.Style;
import com.google.gwt.i18n.client.NumberFormat;
import lsfusion.gwt.form.shared.view.GPropertyDraw;

public abstract class FormatGridCellRenderer<T, F> extends TextBasedGridCellRenderer<T> {
    protected F format;

    public FormatGridCellRenderer(GPropertyDraw property) {
        super(property, Style.TextAlign.RIGHT);

        this.format = (F) property.getFormat();
    }

    public void updateFormat() {
        this.format = (F) property.getFormat();
    }

}
