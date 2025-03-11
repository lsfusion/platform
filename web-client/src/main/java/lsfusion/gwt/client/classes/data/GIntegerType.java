package lsfusion.gwt.client.classes.data;

import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.async.GInputList;
import lsfusion.gwt.client.form.property.async.GInputListAction;
import lsfusion.gwt.client.form.property.cell.classes.controller.IntegerCellEditor;
import lsfusion.gwt.client.form.property.cell.classes.controller.RequestValueCellEditor;
import lsfusion.gwt.client.form.property.cell.classes.controller.YearCellEditor;
import lsfusion.gwt.client.form.property.cell.controller.EditContext;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;

public class GIntegerType extends GIntegralType {
    public static GIntegerType instance = new GIntegerType();

    @Override
    public RequestValueCellEditor createCellEditor(EditManager editManager, GPropertyDraw editProperty, GInputList inputList, GInputListAction[] inputListActions, EditContext editContext) {
        return editProperty.inputType.isYear() ? new YearCellEditor(editManager, editProperty) : new IntegerCellEditor(editManager, editProperty);
    }

    @Override
    public PValue convertDouble(Double doubleValue) {
        return PValue.getPValue(doubleValue.intValue());
    }

    @Override
    protected int getPrecision() {
        return 8;
    }

    @Override
    public String toString() {
        return ClientMessages.Instance.get().typeIntegerCaption();
    }

    @Override
    public boolean isId() {
        return true;
    }
}
