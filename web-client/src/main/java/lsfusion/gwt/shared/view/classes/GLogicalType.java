package lsfusion.gwt.shared.view.classes;

import lsfusion.gwt.shared.base.GwtSharedUtils;
import lsfusion.gwt.client.form.MainFrameMessages;
import lsfusion.gwt.shared.view.GFont;
import lsfusion.gwt.shared.view.GPropertyDraw;
import lsfusion.gwt.shared.view.GWidthStringProcessor;
import lsfusion.gwt.client.form.grid.EditManager;
import lsfusion.gwt.client.form.grid.editor.AbstractGridCellEditor;
import lsfusion.gwt.client.form.grid.editor.GridCellEditor;
import lsfusion.gwt.client.form.grid.renderer.GridCellRenderer;
import lsfusion.gwt.client.form.grid.renderer.LogicalGridCellRenderer;

import java.text.ParseException;

public class GLogicalType extends GDataType {
    public static GLogicalType instance = new GLogicalType();

    @Override
    public GridCellRenderer createGridCellRenderer(GPropertyDraw property) {
        return new LogicalGridCellRenderer();
    }

    @Override
    public GridCellEditor visit(GTypeVisitor visitor) {
        return (GridCellEditor) visitor.visit(this);
    }

    @Override
    public GridCellEditor createGridCellEditor(EditManager editManager, GPropertyDraw editProperty) {
        return AbstractGridCellEditor.createGridCellEditor(this, editManager, null);
    }

    @Override
    public int getDefaultWidth(GFont font, GPropertyDraw propertyDraw, GWidthStringProcessor widthStringProcessor) {
        return 30;
    }

    @Override
    public Object parseString(String s, String pattern) throws ParseException {
        try {
            return GwtSharedUtils.nullBoolean(Boolean.parseBoolean(s));
        } catch (NumberFormatException nfe) {
            throw new ParseException("string " + s + "can not be converted to logical", 0);
        }
    }

    @Override
    public String toString() {
        return MainFrameMessages.Instance.get().typeLogicalCaption();
    }
}
