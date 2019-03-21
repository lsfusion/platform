package lsfusion.gwt.shared.classes.data.link;

import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.controller.GridCellEditor;
import lsfusion.gwt.client.form.property.cell.classes.controller.LinkGridCellEditor;
import lsfusion.gwt.client.form.property.cell.classes.view.FileGridCellRenderer;
import lsfusion.gwt.client.form.property.cell.view.GridCellRenderer;
import lsfusion.gwt.shared.form.design.GFont;
import lsfusion.gwt.shared.form.property.GPropertyDraw;
import lsfusion.gwt.shared.form.design.GWidthStringProcessor;
import lsfusion.gwt.shared.classes.data.GDataType;
import lsfusion.gwt.shared.form.filter.user.GCompare;

import java.text.ParseException;

import static lsfusion.gwt.shared.form.filter.user.GCompare.EQUALS;
import static lsfusion.gwt.shared.form.filter.user.GCompare.NOT_EQUALS;

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
        return new LinkGridCellEditor(editManager, editProperty);
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