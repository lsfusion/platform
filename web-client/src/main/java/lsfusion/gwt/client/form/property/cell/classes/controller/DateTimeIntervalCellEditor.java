package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.form.property.cell.classes.view.DateTimeIntervalCellRenderer;
import lsfusion.gwt.client.form.property.cell.controller.CellEditor;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;

import java.math.BigDecimal;
import java.util.Date;


public class DateTimeIntervalCellEditor implements CellEditor {

    private final EditManager editManager;
    private JavaScriptObject dateRangePicker;

    public DateTimeIntervalCellEditor(EditManager editManager) {
        this.editManager = editManager;
    }

    public void validateAndCommit(Long dateFrom, Long dateTo) {
        if (dateFrom != null && dateTo != null)
            editManager.commitEditing(new BigDecimal(dateFrom + "." + dateTo));
        else
            editManager.cancelEditing();

        destroyDateRangePicker(dateRangePicker);
    }

    @Override
    public void startEditing(Event event, Element parent, Object oldValue) {
        String object = String.valueOf(oldValue);
        int indexOfDecimal = object.indexOf(".");
        dateRangePicker = createPicker(parent,
                DateTimeIntervalCellRenderer.getTimestamp(object.substring(0, indexOfDecimal)),
                DateTimeIntervalCellRenderer.getTimestamp(object.substring(indexOfDecimal + 1)));
    }

    protected native void destroyDateRangePicker(JavaScriptObject picker)/*-{
        picker.destroy();
    }-*/;

    protected native JavaScriptObject createPicker(Element parent, Date startDate, Date endDate)/*-{
        var thisObj = this;
        var dateFrom, dateTo;

        var picker = new $wnd.Litepicker({
            element: parent,
            startDate: startDate,
            endDate: endDate,
            singleMode: false,
            selectForward: true
        });

        picker.on('hide', function () {
            thisObj.@DateTimeIntervalCellEditor::validateAndCommit(*)(dateFrom, dateTo);
        });

        picker.on('preselect', function (date1, date2){
            dateFrom = date1 != null ? date1.getTime() / 1000 : null;
            dateTo = date2 != null ? date2.getTime() / 1000 : null;
        });

        return picker;
    }-*/;
}
