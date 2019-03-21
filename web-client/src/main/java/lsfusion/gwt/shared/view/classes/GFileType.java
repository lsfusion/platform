package lsfusion.gwt.shared.view.classes;

import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.classes.controller.FileGridCellEditor;
import lsfusion.gwt.client.form.property.cell.controller.GridCellEditor;
import lsfusion.gwt.client.form.property.cell.classes.FileGridCellRenderer;
import lsfusion.gwt.client.form.property.cell.GridCellRenderer;
import lsfusion.gwt.shared.view.GFont;
import lsfusion.gwt.shared.view.GPropertyDraw;
import lsfusion.gwt.shared.view.GWidthStringProcessor;
import lsfusion.gwt.shared.view.filter.GCompare;

import java.text.ParseException;
import java.util.ArrayList;

import static lsfusion.gwt.shared.view.filter.GCompare.EQUALS;
import static lsfusion.gwt.shared.view.filter.GCompare.NOT_EQUALS;

public abstract class GFileType extends GDataType {
    public boolean multiple;
    public boolean storeName;
    public String description;
    public ArrayList<String> validContentTypes;

    public GFileType() {
    }

    public GFileType(boolean multiple, boolean storeName) {
        this.multiple = multiple;
        this.storeName = storeName;
    }

    @Override
    public GCompare[] getFilterCompares() {
        return new GCompare[] {EQUALS, NOT_EQUALS};
    }

    @Override
    public Object parseString(String s, String pattern) throws ParseException {
        throw new ParseException("File class doesn't support conversion from string", 0);
    }

    @Override
    public GridCellEditor createGridCellEditor(EditManager editManager, GPropertyDraw editProperty) {
        return new FileGridCellEditor(editManager, editProperty, description, multiple, storeName, validContentTypes);
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
