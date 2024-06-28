package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.view.EventHandler;
import lsfusion.gwt.client.classes.data.GIntegerType;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;

import static lsfusion.gwt.client.base.GwtClientUtils.nvl;

public class YearCellEditor extends IntegralCellEditor {

    public YearCellEditor(EditManager editManager, GPropertyDraw property) {
        super(GIntegerType.instance, editManager, property);
    }

    @Override
    public void start(EventHandler handler, Element parent, RenderContext renderContext, boolean notFocusable, PValue oldValue) {
        super.start(handler, parent, renderContext, notFocusable, oldValue);

        if (started) {
            openYearPicker(inputElement, parent, nvl(PValue.getIntegerValue(oldValue), 0));
            GwtClientUtils.addDropDownPartner(parent, getYearPickerContainer(parent));
        }
    }

    @Override
    public void stop(Element parent, boolean cancel, boolean blurred) {
        if (started)
            destroyYearPicker(parent);
        super.stop(parent, cancel, blurred);
    }

    protected void pickerApply(Element parent) {
        commit(parent);
    }

    protected native void openYearPicker(Element inputElement, Element parent, int initYear)/*-{
        var thisObj = this;
        parent.picker = new $wnd.AirDatepicker(inputElement, {
            view: 'years', // displaying the years of one decade
            minView: 'years', // The minimum possible representation of the calendar. It is used, for example, when you need to provide only a choice of the year.
            visible: true, // Shows the calendar immediately after initialization.
            startDate: initYear !== 0 ? new Date().setFullYear(initYear) : new Date(),
            dateFormat: function (date) {
                return date.getFullYear(); // to return a number, not a Date object
            },
            onHide: function(isFinished) {
                if (isFinished) // isFinished — animation completion indicator
                    thisObj.@YearCellEditor::pickerApply(*)(parent);
            }
        });
    }-*/;

    protected native void destroyYearPicker(Element parent)/*-{
        parent.picker.destroy();
    }-*/;

    protected native Element getYearPickerContainer(Element parent)/*-{
        return parent.picker.$datepicker;
    }-*/;
}
