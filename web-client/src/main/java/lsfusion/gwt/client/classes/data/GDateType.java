package lsfusion.gwt.client.classes.data;

import com.google.gwt.core.client.JsDate;
import com.google.gwt.i18n.client.DateTimeFormat;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.GwtSharedUtils;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.async.GInputList;
import lsfusion.gwt.client.form.property.async.GInputListAction;
import lsfusion.gwt.client.form.property.cell.classes.GDateDTO;
import lsfusion.gwt.client.form.property.cell.classes.controller.DateCellEditor;
import lsfusion.gwt.client.form.property.cell.classes.controller.RequestValueCellEditor;
import lsfusion.gwt.client.form.property.cell.controller.EditContext;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;

import java.text.ParseException;
import java.util.Date;

public class GDateType extends GADateType {

    public static GDateType instance = new GDateType();

    @Override
    public DateTimeFormat getFormat(String pattern) {
        return GwtSharedUtils.getDateFormat(pattern);
    }

    @Override
    public RequestValueCellEditor createCellEditor(EditManager editManager, GPropertyDraw editProperty, GInputList inputList, GInputListAction[] inputListActions, EditContext editContext) {
        return new DateCellEditor(this, editManager, editProperty);
    }

    @Override
    protected DateTimeFormat[] getFormats(String pattern) {
        return GwtClientUtils.add(super.getFormats(pattern), new DateTimeFormat[] { GwtSharedUtils.getDefaultDateFormat() }, DateTimeFormat[]::new);
    }

    @Override
    public DateTimeFormat getISOFormat() {
        return DateTimeFormat.getFormat("yyyy-MM-dd");
    }

    @Override
    public JsDate toJsDate(PValue value) {
        return PValue.getDateValue(value).toJsDate();
    }

    @Override
    public PValue fromJsDate(JsDate date) {
        return PValue.getPValue(GDateDTO.fromJsDate(date));
    }

    @Override
    public String toString() {
        return ClientMessages.Instance.get().typeDateCaption();
    }

    public static Date parseDate(String value, DateTimeFormat... formats) throws ParseException {
        for (DateTimeFormat format : formats) {
            try {
                return format.parseStrict(value);
            } catch (IllegalArgumentException ignore) {
            }
        }
        throw new ParseException("string " + value + "can not be converted to date", 0);
    }

}
