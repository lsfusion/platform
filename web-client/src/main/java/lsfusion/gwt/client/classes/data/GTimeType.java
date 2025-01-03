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
import lsfusion.gwt.client.form.property.cell.classes.GTimeDTO;
import lsfusion.gwt.client.form.property.cell.classes.controller.RequestValueCellEditor;
import lsfusion.gwt.client.form.property.cell.classes.controller.TimeCellEditor;
import lsfusion.gwt.client.form.property.cell.controller.EditContext;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;

import static lsfusion.gwt.client.base.GwtSharedUtils.getDefaultTimeFormat;
import static lsfusion.gwt.client.base.GwtSharedUtils.getDefaultTimeShortFormat;

public class GTimeType extends GADateType {
    public static GTimeType instance = new GTimeType();

    @Override
    public DateTimeFormat getFormat(String pattern) {
        return GwtSharedUtils.getTimeFormat(pattern);
    }

    @Override
    public RequestValueCellEditor createCellEditor(EditManager editManager, GPropertyDraw editProperty, GInputList inputList, GInputListAction[] inputListActions, EditContext editContext) {
        return new TimeCellEditor(this, editManager, editProperty);
    }

    @Override
    protected DateTimeFormat[] getFormats(String pattern) {
        return GwtClientUtils.add(super.getFormats(pattern), new DateTimeFormat[] { getDefaultTimeShortFormat(), getDefaultTimeFormat() }, DateTimeFormat[]::new);
    }

    @Override
    public DateTimeFormat getISOFormat() {
        return DateTimeFormat.getFormat("HH:mm");
    }

    @Override
    public JsDate toJsDate(PValue value) {
        return PValue.getTimeValue(value).toJsDate();
    }

    @Override
    public PValue fromJsDate(JsDate date) {
        return PValue.getPValue(GTimeDTO.fromJsDate(date));
    }

    @Override
    public String toString() {
        return ClientMessages.Instance.get().typeTimeCaption();
    }
}
