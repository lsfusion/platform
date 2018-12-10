package lsfusion.gwt.shared.view.classes;

import com.google.gwt.i18n.client.NumberFormat;
import lsfusion.gwt.client.form.MainFrameMessages;
import lsfusion.gwt.shared.view.GPropertyDraw;
import lsfusion.gwt.client.form.grid.EditManager;
import lsfusion.gwt.client.form.grid.editor.AbstractGridCellEditor;
import lsfusion.gwt.client.form.grid.editor.GridCellEditor;
import lsfusion.gwt.client.form.grid.renderer.DoubleGridCellRenderer;
import lsfusion.gwt.client.form.grid.renderer.GridCellRenderer;

import java.text.ParseException;

public class GDoubleType extends GIntegralType {
    public static GDoubleType instance = new GDoubleType();

    protected static String defaultPattern = "#,###.##########";

    @Override
    public GridCellRenderer createGridCellRenderer(GPropertyDraw property) {
        return new DoubleGridCellRenderer(property);
    }

    @Override
    public GridCellEditor visit(GTypeVisitor visitor) {
        return (GridCellEditor) visitor.visit(this);
    }

    @Override
    public GridCellEditor createGridCellEditor(EditManager editManager, GPropertyDraw editProperty) {
        return AbstractGridCellEditor.createGridCellEditor(this, editManager, editProperty);
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
