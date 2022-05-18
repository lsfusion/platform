package lsfusion.gwt.client.classes.data;

import com.google.gwt.i18n.client.DateTimeFormat;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.GwtSharedUtils;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.async.GInputList;
import lsfusion.gwt.client.form.property.cell.classes.GTimeDTO;
import lsfusion.gwt.client.form.property.cell.classes.controller.TimeCellEditor;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.controller.CellEditor;

import java.util.Date;

import static lsfusion.gwt.client.base.GwtSharedUtils.*;

public class GTimeType extends GADateType {
    public static GTimeType instance = new GTimeType();

    @Override
    public DateTimeFormat getFormat(String pattern, boolean edit) {
        return GwtSharedUtils.getTimeFormat(pattern);
    }

    @Override
    public CellEditor createGridCellEditor(EditManager editManager, GPropertyDraw editProperty, GInputList inputList) {
        return new TimeCellEditor(this, editManager, editProperty);
    }

    @Override
    protected DateTimeFormat[] getFormats(String pattern, boolean edit) {
        return GwtClientUtils.add(super.getFormats(pattern, edit), new DateTimeFormat[] { getDefaultTimeShortFormat(), getDefaultTimeFormat() }, DateTimeFormat[]::new);
    }

    @Override
    public Object fromDate(Date date) {
        return GTimeDTO.fromDate(date);
    }

    @Override
    public Date toDate(Object value) {
        return ((GTimeDTO) value).toTime();
    }

    @Override
    public String toString() {
        return ClientMessages.Instance.get().typeTimeCaption();
    }
}
