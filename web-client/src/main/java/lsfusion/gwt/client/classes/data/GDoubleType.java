package lsfusion.gwt.client.classes.data;

import com.google.gwt.i18n.client.NumberFormat;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.classes.controller.DoubleGridCellEditor;
import lsfusion.gwt.client.form.property.cell.classes.view.DoubleGridCellRenderer;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.controller.GridCellEditor;
import lsfusion.gwt.client.form.property.cell.view.GridCellRenderer;

import java.text.ParseException;

public class GDoubleType extends GIntegralType {
    public static GDoubleType instance = new GDoubleType();

    protected static String defaultPattern = "#,###.##########";

    @Override
    public GridCellRenderer createGridCellRenderer(GPropertyDraw property) {
        return new DoubleGridCellRenderer(property);
    }

    @Override
    public GridCellEditor createGridCellEditor(EditManager editManager, GPropertyDraw editProperty) {
        return new DoubleGridCellEditor(editManager, editProperty, getEditFormat(editProperty));
    }

    @Override
    protected int getLength() {
        return 10;
    }

    @Override
    public Object parseString(String s, String pattern) throws ParseException {
        return parseToDouble(s, pattern);
    }

    @Override
    public String toString() {
        return ClientMessages.Instance.get().typeDoubleCaption();
    }
    
    @Override
    public NumberFormat getFormat(String pattern) {
        return NumberFormat.getFormat(pattern != null ? pattern : defaultPattern);
    }
}
