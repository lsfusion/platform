package lsfusion.gwt.client.classes.data;

import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.async.GInputList;
import lsfusion.gwt.client.form.property.cell.classes.GZDateTimeDTO;
import lsfusion.gwt.client.form.property.cell.classes.controller.ZDateTimeCellEditor;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.controller.CellEditor;

import java.util.Date;

public class GZDateTimeType extends GDateTimeType {
    public static GZDateTimeType instance = new GZDateTimeType();

    @Override
    public CellEditor createGridCellEditor(EditManager editManager, GPropertyDraw editProperty, GInputList inputList) {
        return new ZDateTimeCellEditor(this, editManager, editProperty);
    }

    // we want to have string width independent of the timezone
    @Override
    public String getDefaultWidthString(GPropertyDraw propertyDraw) {
        return GDateTimeType.instance.getDefaultWidthString(propertyDraw);
    }

    @Override
    public Object fromDate(Date date) {
        return GZDateTimeDTO.fromDate(date);
    }

    @Override
    public Date toDate(Object value) {
        return ((GZDateTimeDTO) value).toDateTime();
    }

    @Override
    public String toString() {
        return ClientMessages.Instance.get().typeZDateTimeCaption();
    }

}
