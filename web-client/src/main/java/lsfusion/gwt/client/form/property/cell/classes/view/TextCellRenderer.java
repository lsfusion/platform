package lsfusion.gwt.client.form.property.cell.classes.view;

import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.form.property.GPropertyDraw;

public class TextCellRenderer extends StringBasedCellRenderer<Object> {

    public TextCellRenderer(GPropertyDraw property) {
        super(property);
    }

    @Override
    protected boolean isMultiLine() {
        return true;
    }

    @Override
    public String format(Object value) {
        return (String) value;
    }
}