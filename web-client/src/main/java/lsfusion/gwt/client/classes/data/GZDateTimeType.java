package lsfusion.gwt.client.classes.data;

import com.google.gwt.core.client.JsDate;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.async.GInputList;
import lsfusion.gwt.client.form.property.async.GInputListAction;
import lsfusion.gwt.client.form.property.cell.classes.GZDateTimeDTO;
import lsfusion.gwt.client.form.property.cell.classes.controller.RequestValueCellEditor;
import lsfusion.gwt.client.form.property.cell.classes.controller.ZDateTimeCellEditor;
import lsfusion.gwt.client.form.property.cell.controller.EditContext;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;

public class GZDateTimeType extends GDateTimeType {
    public static GZDateTimeType instance = new GZDateTimeType();

    @Override
    public RequestValueCellEditor createCellEditor(EditManager editManager, GPropertyDraw editProperty, GInputList inputList, GInputListAction[] inputListActions, EditContext editContext) {
        return new ZDateTimeCellEditor(this, editManager, editProperty);
    }

    // we want to have string width independent of the timezone
    @Override
    public String getDefaultWidthString(GPropertyDraw propertyDraw) {
        return GDateTimeType.instance.getDefaultWidthString(propertyDraw);
    }

    @Override
    public PValue fromJsDate(JsDate date) {
        return GZDateTimeDTO.fromJsDate(date);
    }

    @Override
    public JsDate toJsDate(PValue value) {
        return PValue.getZDateTimeValue(value).toJsDate();
    }

    @Override
    public String toString() {
        return ClientMessages.Instance.get().typeZDateTimeCaption();
    }

}
