package lsfusion.gwt.form.shared.view.classes;

import com.google.gwt.i18n.client.NumberFormat;
import lsfusion.gwt.form.shared.view.GPropertyDraw;
import lsfusion.gwt.form.shared.view.grid.EditManager;
import lsfusion.gwt.form.shared.view.grid.editor.DoubleGridCellEditor;
import lsfusion.gwt.form.shared.view.grid.editor.GridCellEditor;
import lsfusion.gwt.form.shared.view.grid.renderer.DoubleGridCellRenderer;
import lsfusion.gwt.form.shared.view.grid.renderer.GridCellRenderer;

import java.text.ParseException;

public class GDoubleType extends GIntegralType {
    public static GDoubleType instance = new GDoubleType();

    protected String formatPattern = "#,###.##########";

    @Override
    public GridCellRenderer createGridCellRenderer(GPropertyDraw property) {
        return new DoubleGridCellRenderer(property, getFormat());
    }

    @Override
    public GridCellEditor createGridCellEditor(EditManager editManager, GPropertyDraw editProperty) {
        return new DoubleGridCellEditor(editManager, editProperty, getFormat());
    }

    @Override
    public Object parseString(String s) throws ParseException {
        return parseToDouble(s);
    }

    @Override
    public String toString() {
        return "Вещественное число";
    }
    
    @Override
    public NumberFormat getFormat() {
        return NumberFormat.getFormat(formatPattern);
    }
}
