package lsfusion.gwt.client.classes.data;

import com.google.gwt.i18n.client.DateTimeFormat;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.GwtSharedUtils;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.async.GInputList;
import lsfusion.gwt.client.form.property.cell.GEditBindingMap;
import lsfusion.gwt.client.form.property.cell.classes.GTimeDTO;
import lsfusion.gwt.client.form.property.cell.classes.controller.TimeCellEditor;
import lsfusion.gwt.client.form.property.cell.classes.view.DateCellRenderer;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.controller.CellEditor;
import lsfusion.gwt.client.form.property.cell.view.CellRenderer;

import java.sql.Time;
import java.text.ParseException;
import java.util.Date;

import static lsfusion.gwt.client.base.GwtSharedUtils.getDefaultTimeFormat;
import static lsfusion.gwt.client.base.GwtSharedUtils.getDefaultTimeShortFormat;
import static lsfusion.gwt.client.classes.data.GDateType.parseDate;

public class GTimeType extends GADateType {
    public static GTimeType instance = new GTimeType();

    @Override
    protected DateTimeFormat getFormat(String pattern) {
        return GwtSharedUtils.getTimeFormat(pattern);
    }

    @Override
    public CellEditor createGridCellEditor(EditManager editManager, GPropertyDraw editProperty, GInputList inputList) {
        return new TimeCellEditor(this, editManager, editProperty);
    }

    @Override
    protected DateTimeFormat[] getFormats(String pattern) {
        return new DateTimeFormat[] {GwtSharedUtils.getTimeFormat(pattern), getDefaultTimeShortFormat()};
    }

    @Override
    public Object fromDate(Date date) {
        return GTimeDTO.fromDate(date);
    }

    @Override
    public Time toDate(Object value) {
        return ((GTimeDTO) value).toTime();
    }

    @Override
    public String toString() {
        return ClientMessages.Instance.get().typeTimeCaption();
    }
}
