package lsfusion.gwt.form.shared.view.classes;

import lsfusion.gwt.form.client.MainFrameMessages;
import lsfusion.gwt.form.shared.view.GFont;
import lsfusion.gwt.form.shared.view.GPropertyDraw;
import lsfusion.gwt.form.shared.view.GWidthStringProcessor;
import lsfusion.gwt.form.shared.view.grid.EditManager;
import lsfusion.gwt.form.shared.view.grid.editor.AbstractGridCellEditor;
import lsfusion.gwt.form.shared.view.grid.editor.GridCellEditor;
import lsfusion.gwt.form.shared.view.grid.renderer.ColorGridCellRenderer;
import lsfusion.gwt.form.shared.view.grid.renderer.GridCellRenderer;

import java.text.ParseException;

public class GColorType extends GDataType {
    public static GColorType instance = new GColorType();

    @Override
    public GridCellEditor createGridCellEditor(EditManager editManager, GPropertyDraw editProperty) {
        return AbstractGridCellEditor.createGridCellEditor(this, editManager, editProperty);
    }

    @Override
    public GridCellRenderer createGridCellRenderer(GPropertyDraw property) {
        return new ColorGridCellRenderer();
    }

    @Override
    public GridCellEditor visit(GTypeVisitor visitor) {
        return (GridCellEditor) visitor.visit(this);
    }

    @Override
    public int getDefaultWidth(GFont font, GPropertyDraw propertyDraw, GWidthStringProcessor widthStringProcessor) {
        return 40;
    }

    @Override
    public Object parseString(String s, String pattern) throws ParseException {
        throw new ParseException("Color class doesn't support conversion from string", 0);
    }

    @Override
    public String toString() {
        return MainFrameMessages.Instance.get().typeColorCaption();
    }
}
