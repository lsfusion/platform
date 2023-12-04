package lsfusion.gwt.client.classes.data.link;

import lsfusion.gwt.client.base.size.GSize;
import lsfusion.gwt.client.classes.data.GDataType;
import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.form.filter.user.GCompare;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.async.GInputList;
import lsfusion.gwt.client.form.property.async.GInputListAction;
import lsfusion.gwt.client.form.property.cell.classes.controller.LinkCellEditor;
import lsfusion.gwt.client.form.property.cell.classes.controller.RequestValueCellEditor;
import lsfusion.gwt.client.form.property.cell.classes.view.LinkCellRenderer;
import lsfusion.gwt.client.form.property.cell.controller.EditContext;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.view.CellRenderer;

import java.text.ParseException;

import static lsfusion.gwt.client.form.filter.user.GCompare.EQUALS;

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
        return new GCompare[] {EQUALS};
    }

    @Override
    public PValue parseString(String s, String pattern) throws ParseException {
        return PValue.getPValue(s);
    }

    @Override
    public RequestValueCellEditor createCellEditor(EditManager editManager, GPropertyDraw editProperty, GInputList inputList, GInputListAction[] inputListActions, EditContext editContext) {
        return new LinkCellEditor(editManager, editProperty);
    }

    @Override
    public CellRenderer createCellRenderer(GPropertyDraw property) {
        return new LinkCellRenderer(property);
    }

    @Override
    public GSize getDefaultWidth(GFont font, GPropertyDraw propertyDraw, boolean needNotNull, boolean globalCaptionIsDrawn) {
        return GSize.CONST(50);
    }
}