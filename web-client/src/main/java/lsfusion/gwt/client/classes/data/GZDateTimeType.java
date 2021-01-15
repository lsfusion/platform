package lsfusion.gwt.client.classes.data;

import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.classes.GZDateTimeDTO;
import lsfusion.gwt.client.form.property.cell.classes.controller.ZDateTimeCellEditor;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.controller.CellEditor;

import java.text.ParseException;

import static lsfusion.gwt.client.base.GwtSharedUtils.*;

public class GZDateTimeType extends GDateTimeType {
    public static GZDateTimeType instance = new GZDateTimeType();

    @Override
    public CellEditor createGridCellEditor(EditManager editManager, GPropertyDraw editProperty) {
        return new ZDateTimeCellEditor(editManager, editProperty);
    }

    @Override
    public Object parseString(String value, String pattern) throws ParseException {
        return value.isEmpty() ? null : GZDateTimeDTO.fromDate(GDateType.parseDate(value, getDateTimeFormat(pattern, true), getDefaultDateTimeShortFormat(), getDefaultDateFormat(true)));
    }

    @Override
    public String toString() {
        return ClientMessages.Instance.get().typeZDateTimeCaption();
    }

}
