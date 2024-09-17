package lsfusion.gwt.client.classes.data;

import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.size.GSize;
import lsfusion.gwt.client.classes.GInputType;
import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.async.GInputList;
import lsfusion.gwt.client.form.property.async.GInputListAction;
import lsfusion.gwt.client.form.property.cell.classes.ColorDTO;
import lsfusion.gwt.client.form.property.cell.classes.controller.ColorCellEditor;
import lsfusion.gwt.client.form.property.cell.classes.controller.RequestValueCellEditor;
import lsfusion.gwt.client.form.property.cell.classes.view.ColorCellRenderer;
import lsfusion.gwt.client.form.property.cell.controller.EditContext;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.view.CellRenderer;

import java.text.ParseException;

public class GColorType extends GDataType {
    public static GColorType instance = new GColorType();

    @Override
    public RequestValueCellEditor createCellEditor(EditManager editManager, GPropertyDraw editProperty, GInputList inputList, GInputListAction[] inputListActions, EditContext editContext) {
        return new ColorCellEditor(editManager, editProperty);
    }

    @Override
    public CellRenderer createCellRenderer(GPropertyDraw property) {
        return new ColorCellRenderer(property);
    }

    private final static GInputType inputType = new GInputType("color");
    @Override
    public GInputType getValueInputType() {
        return inputType;
    }

    @Override
    public GSize getDefaultWidth(GFont font, GPropertyDraw propertyDraw, boolean globalCaptionIsDrawn) {
        return GSize.CONST(40);
    }

    @Override
    public PValue parseString(String s, String pattern) throws ParseException {
        try {
            return PValue.getPValue(new ColorDTO(s.substring(s.length() - 6)));
        } catch (Exception e) {
            throw new ParseException("error parsing color", 0);
        }
    }

    @Override
    public String toString() {
        return ClientMessages.Instance.get().typeColorCaption();
    }
}
