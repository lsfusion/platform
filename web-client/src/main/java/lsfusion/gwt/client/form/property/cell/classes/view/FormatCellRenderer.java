package lsfusion.gwt.client.form.property.cell.classes.view;

import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.base.EscapeUtils;
import lsfusion.gwt.client.form.property.GPropertyDraw;

public abstract class FormatCellRenderer<T, F> extends TextBasedCellRenderer<T> {
    protected F format;

    public FormatCellRenderer(GPropertyDraw property) {
        super(property);
        updateFormat();
    }

    public void updateFormat() {
        this.format = (F) property.getFormat();
    }
}
