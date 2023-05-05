package lsfusion.gwt.client.classes.data;

import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.async.GInputList;
import lsfusion.gwt.client.form.property.cell.classes.GZDateTimeDTO;
import lsfusion.gwt.client.form.property.cell.classes.controller.RequestValueCellEditor;
import lsfusion.gwt.client.form.property.cell.classes.controller.ZDateTimeCellEditor;
import lsfusion.gwt.client.form.property.cell.controller.EditContext;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;

import java.util.Date;

public class GZDateTimeType extends GDateTimeType {
    public static GZDateTimeType instance = new GZDateTimeType();

    @Override
    public RequestValueCellEditor createCellEditor(EditManager editManager, GPropertyDraw editProperty, GInputList inputList, EditContext editContext) {
        return new ZDateTimeCellEditor(this, editManager, editProperty);
    }

    // we want to have string width independent of the timezone
    @Override
    public String getDefaultWidthString(GPropertyDraw propertyDraw) {
        return GDateTimeType.instance.getDefaultWidthString(propertyDraw);
    }

    @Override
    public PValue fromDate(Date date) {
        return GZDateTimeDTO.fromDate(date);
    }

    @Override
    public Date toDate(PValue value) {
        return PValue.getZDateTimeValue(value).toDateTime();
    }

    @Override
    public String toString() {
        return ClientMessages.Instance.get().typeZDateTimeCaption();
    }

}
