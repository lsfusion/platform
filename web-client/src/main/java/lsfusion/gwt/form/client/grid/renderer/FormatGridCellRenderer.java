package lsfusion.gwt.form.client.grid.renderer;

import com.google.gwt.dom.client.Style;
import lsfusion.gwt.form.shared.view.GPropertyDraw;

public abstract class FormatGridCellRenderer<T, F> extends TextBasedGridCellRenderer<T> {
    protected F format;

    public FormatGridCellRenderer(GPropertyDraw property) {
        super(property, Style.TextAlign.RIGHT);

        updateFormat();
    }

    public void updateFormat() {
        this.format = (F) property.getFormat();
    }

}
