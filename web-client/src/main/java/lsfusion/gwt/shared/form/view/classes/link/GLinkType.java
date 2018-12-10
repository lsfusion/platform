package lsfusion.gwt.form.shared.view.classes.link;

import lsfusion.gwt.form.shared.view.GFont;
import lsfusion.gwt.form.shared.view.GPropertyDraw;
import lsfusion.gwt.form.shared.view.GWidthStringProcessor;
import lsfusion.gwt.form.shared.view.classes.GDataType;
import lsfusion.gwt.form.shared.view.filter.GCompare;
import lsfusion.gwt.form.client.grid.EditManager;
import lsfusion.gwt.form.client.grid.editor.AbstractGridCellEditor;
import lsfusion.gwt.form.client.grid.editor.GridCellEditor;
import lsfusion.gwt.form.client.grid.renderer.FileGridCellRenderer;
import lsfusion.gwt.form.client.grid.renderer.GridCellRenderer;

import java.text.ParseException;

import static lsfusion.gwt.form.shared.view.filter.GCompare.EQUALS;
import static lsfusion.gwt.form.shared.view.filter.GCompare.NOT_EQUALS;

public abstract class GLinkType extends GDataType {
    public boolean multiple;
    public String description;

    public GLinkType() {
    }

    public GLinkType(boolean multiple) {
        this.multiple = multiple;
    }

    @Override
    public GCompare[] getFilterCompares() {
        return new GCompare[] {EQUALS, NOT_EQUALS};
    }

    @Override
    public Object parseString(String s, String pattern) throws ParseException {
        return s;
    }

    @Override
    public GridCellEditor createGridCellEditor(EditManager editManager, GPropertyDraw editProperty) {
        return AbstractGridCellEditor.createGridCellEditor(this, editManager, editProperty);
    }

    @Override
    public GridCellRenderer createGridCellRenderer(GPropertyDraw property) {
        return new FileGridCellRenderer(property);
    }

    @Override
    public int getDefaultWidth(GFont font, GPropertyDraw propertyDraw, GWidthStringProcessor widthStringProcessor) {
        return 18;
    }

}