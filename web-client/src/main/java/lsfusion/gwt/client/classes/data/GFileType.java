package lsfusion.gwt.client.classes.data;

import lsfusion.gwt.client.base.size.GSize;
import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.form.filter.user.GCompare;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.async.GInputList;
import lsfusion.gwt.client.form.property.async.GInputListAction;
import lsfusion.gwt.client.form.property.cell.classes.controller.FileCellEditor;
import lsfusion.gwt.client.form.property.cell.classes.controller.RequestValueCellEditor;
import lsfusion.gwt.client.form.property.cell.classes.view.FileCellRenderer;
import lsfusion.gwt.client.form.property.cell.controller.EditContext;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.view.CellRenderer;

import java.text.ParseException;
import java.util.List;

import static lsfusion.gwt.client.form.filter.user.GCompare.EQUALS;

public abstract class GFileType extends GDataType {
    public boolean multiple;
    public boolean storeName;
    public String description;
    public List<String> validExtensions;
    public boolean named;

    public GFileType() {
    }

    public GFileType(boolean multiple, boolean storeName, boolean named) {
        this.multiple = multiple;
        this.storeName = storeName;
        this.named = named;
    }

    @Override
    public GCompare[] getFilterCompares() {
        return new GCompare[] {EQUALS};
    }

    @Override
    public PValue parseString(String s, String pattern) throws ParseException {
        throw new ParseException("File class doesn't support conversion from string", 0);
    }

    @Override
    public RequestValueCellEditor createCellEditor(EditManager editManager, GPropertyDraw editProperty, GInputList inputList, GInputListAction[] inputListActions, EditContext editContext) {
        return new FileCellEditor(editManager, storeName, validExtensions, named);
    }

    @Override
    public CellRenderer createCellRenderer(GPropertyDraw property) {
        return new FileCellRenderer(property);
    }

    @Override
    public GSize getDefaultWidth(GFont font, GPropertyDraw propertyDraw, boolean globalCaptionIsDrawn) {
        return GSize.CONST(18);
    }
}
