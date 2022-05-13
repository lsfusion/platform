package lsfusion.gwt.client.classes.data;

import com.google.gwt.i18n.client.NumberFormat;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.async.GInputList;
import lsfusion.gwt.client.form.property.cell.classes.controller.DoubleCellEditor;
import lsfusion.gwt.client.form.property.cell.classes.view.DoubleCellRenderer;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.controller.CellEditor;
import lsfusion.gwt.client.form.property.cell.view.CellRenderer;

public class GDoubleType extends GIntegralType {
    public static GDoubleType instance = new GDoubleType();

    protected static String defaultPattern = "#,###.##########";

    @Override
    public CellRenderer createGridCellRenderer(GPropertyDraw property) {
        return new DoubleCellRenderer(property);
    }

    @Override
    public CellEditor createGridCellEditor(EditManager editManager, GPropertyDraw editProperty, GInputList inputList) {
        return new DoubleCellEditor(editManager, editProperty);
    }

    @Override
    protected int getPrecision() {
        return 10;
    }

    @Override
    public Object convertDouble(Double doubleValue) {
        return doubleValue;
    }

    @Override
    public String toString() {
        return ClientMessages.Instance.get().typeDoubleCaption();
    }
    
    @Override
    protected NumberFormat getDefaultFormat() {
        return NumberFormat.getFormat(defaultPattern);
    }
}
