package lsfusion.gwt.form.shared.view.classes;

import lsfusion.gwt.form.client.MainFrameMessages;
import lsfusion.gwt.form.shared.view.GFont;
import lsfusion.gwt.form.shared.view.GPropertyDraw;
import lsfusion.gwt.form.shared.view.GWidthStringProcessor;
import lsfusion.gwt.form.shared.view.filter.GCompare;
import lsfusion.gwt.form.client.grid.EditManager;
import lsfusion.gwt.form.client.grid.editor.AbstractGridCellEditor;
import lsfusion.gwt.form.client.grid.editor.GridCellEditor;
import lsfusion.gwt.form.client.grid.renderer.GridCellRenderer;
import lsfusion.gwt.form.client.grid.renderer.NumberGridCellRenderer;

import java.text.ParseException;

import static lsfusion.gwt.form.shared.view.filter.GCompare.*;

//import lsfusion.gwt.form.shared.view.filter.GCompare;

public class GObjectType extends GType {
    public static final GObjectType instance = new GObjectType();

    @Override
    public GridCellRenderer createGridCellRenderer(GPropertyDraw property) {
        return new NumberGridCellRenderer(property);
    }

    @Override
    public GridCellEditor visit(GTypeVisitor visitor) {
        return (GridCellEditor) visitor.visit(this);
    }

    @Override
    public int getFullWidthString(String widthString, GFont font, GWidthStringProcessor widthStringProcessor) {
        return GDataType.getFullWidthString(font, widthString, widthStringProcessor);
    }

    @Override
    public int getDefaultWidth(GFont font, GPropertyDraw propertyDraw, GWidthStringProcessor widthStringProcessor) {
        return getFullWidthString("0000000", font, widthStringProcessor);
    }

    @Override
    public GridCellEditor createValueCellEditor(EditManager editManager, GPropertyDraw editProperty) {
        return AbstractGridCellEditor.createGridCellEditor(this, editManager, editProperty);
    }

    @Override
    public GCompare[] getFilterCompares() {
        return new GCompare[] {EQUALS, GREATER, LESS, GREATER_EQUALS, LESS_EQUALS, NOT_EQUALS};
    }

    @Override
    public Object parseString(String s, String pattern) throws ParseException {
        throw new ParseException("Object class doesn't support conversion from string", 0);
    }

    @Override
    public String toString() {
        return MainFrameMessages.Instance.get().typeObjectCaption();
    }
}
