package lsfusion.gwt.client.classes.data;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsDate;
import com.google.gwt.i18n.client.DateTimeFormat;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.async.GInputList;
import lsfusion.gwt.client.form.property.async.GInputListAction;
import lsfusion.gwt.client.form.property.cell.classes.GDateTimeDTO;
import lsfusion.gwt.client.form.property.cell.classes.controller.DateTimeCellEditor;
import lsfusion.gwt.client.form.property.cell.classes.controller.RequestValueCellEditor;
import lsfusion.gwt.client.form.property.cell.controller.EditContext;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;

import java.util.Date;

import static lsfusion.gwt.client.base.GwtSharedUtils.*;

public class GDateTimeType extends GADateType {
    public static GDateTimeType instance = new GDateTimeType();

    @Override
    public com.google.gwt.i18n.client.DateTimeFormat getFormat(String pattern) {
        return getDateTimeFormat(pattern);
    }

    @Override
    public RequestValueCellEditor createCellEditor(EditManager editManager, GPropertyDraw editProperty, GInputList inputList, GInputListAction[] inputListActions, EditContext editContext) {
        return new DateTimeCellEditor(this, editManager, editProperty);
    }

    @Override
    protected DateTimeFormat[] getFormats(String pattern) {
        return GwtClientUtils.add(super.getFormats(pattern), new DateTimeFormat[] { getDefaultDateTimeShortFormat(), getDefaultDateTimeFormat() }, DateTimeFormat[]::new);
    }

    @Override
    public DateTimeFormat getISOFormat() {
        return DateTimeFormat.getFormat("yyyy-MM-dd'T'HH:mm");
    }

    @Override
    public PValue fromDate(Date date) {
        return PValue.getPValue(GDateTimeDTO.fromDate(date));
    }

    @Override
    public Date toDate(PValue value) {
        return PValue.getDateTimeValue(value).toDateTime();
    }

    @Override
    public JsDate toJsDate(PValue value) {
        return GwtClientUtils.toJsDate(PValue.getDateTimeValue(value).toDateTime());
    }

    @Override
    public PValue fromJsDate(JavaScriptObject date) {
        return PValue.getPValue(GDateTimeDTO.fromDate(GwtClientUtils.fromJsDate((JsDate)date)));
    }

    @Override
    public String toString() {
        return ClientMessages.Instance.get().typeDateTimeCaption();
    }
}
