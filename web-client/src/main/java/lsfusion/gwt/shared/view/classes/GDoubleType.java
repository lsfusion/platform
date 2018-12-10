package lsfusion.gwt.shared.view.classes;

import com.google.gwt.i18n.client.NumberFormat;
import lsfusion.gwt.client.MainFrameMessages;
import lsfusion.gwt.client.form.ui.grid.EditManager;
import lsfusion.gwt.client.form.ui.grid.editor.DoubleGridCellEditor;
import lsfusion.gwt.client.form.ui.grid.editor.GridCellEditor;
import lsfusion.gwt.client.form.ui.grid.renderer.DoubleGridCellRenderer;
import lsfusion.gwt.client.form.ui.grid.renderer.GridCellRenderer;
import lsfusion.gwt.shared.view.GPropertyDraw;

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
        return MainFrameMessages.Instance.get().typeDoubleCaption();
    }
    
    @Override
    public NumberFormat getFormat(String pattern) {
        return NumberFormat.getFormat(pattern != null ? pattern : defaultPattern);
    }
}
