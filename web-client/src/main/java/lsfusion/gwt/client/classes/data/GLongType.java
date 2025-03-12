package lsfusion.gwt.client.classes.data;

import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.async.GInputList;
import lsfusion.gwt.client.form.property.async.GInputListAction;
import lsfusion.gwt.client.form.property.cell.classes.controller.LongCellEditor;
import lsfusion.gwt.client.form.property.cell.classes.controller.RequestValueCellEditor;
import lsfusion.gwt.client.form.property.cell.controller.EditContext;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;

public class GLongType extends GIntegralType {
    public static GLongType instance = new GLongType();

    @Override
    public RequestValueCellEditor createCellEditor(EditManager editManager, GPropertyDraw editProperty, GInputList inputList, GInputListAction[] inputListActions, EditContext editContext) {
        return new LongCellEditor(editManager, editProperty);
    }

    @Override
    public PValue convertDouble(Double doubleValue) {
        return PValue.getPValue(doubleValue.longValue());
    }

    @Override
    protected int getPrecision() {
        return 10;
    }

    @Override
    public String toString() {
        return ClientMessages.Instance.get().typeLongCaption();
    }

    @Override
    public boolean isId() {
        return true;
    }
}
