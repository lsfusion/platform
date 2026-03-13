package lsfusion.gwt.client.classes.data;

import lsfusion.gwt.client.base.size.GSize;
import lsfusion.gwt.client.classes.GInputType;
import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.async.GInputList;
import lsfusion.gwt.client.form.property.async.GInputListAction;
import lsfusion.gwt.client.form.property.cell.classes.controller.RequestValueCellEditor;
import lsfusion.gwt.client.form.property.cell.classes.controller.TextCellEditor;
import lsfusion.gwt.client.form.property.cell.controller.EditContext;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;

import java.text.ParseException;

public abstract class GStringFileBasedType extends GFileBasedType {

    @Override
    public RequestValueCellEditor createCellEditor(EditManager editManager, GPropertyDraw editProperty, GInputList inputList, GInputListAction[] inputListActions, EditContext editContext) {
        return new TextCellEditor(editManager, editProperty, inputList, editContext);
    }

    @Override
    public GSize getDefaultWidth(GFont font, GPropertyDraw propertyDraw, boolean globalCaptionIsDrawn) {
        return GSize.CONST(150);
    }

    @Override
    public PValue parseString(String s, String pattern) throws ParseException {
        return PValue.getPValue(s);
    }

    @Override
    public String formatString(PValue value, String pattern) {
        return PValue.getStringValue(value);
    }

    @Override
    public int getDefaultCharHeight() {
        return 4;
    }

    private static final GInputType inputType = new GInputType("textarea");

    @Override
    public GInputType getValueInputType() {
        return inputType;
    }
}
