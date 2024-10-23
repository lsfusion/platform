package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.view.EventHandler;
import lsfusion.gwt.client.classes.data.GIntegerType;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;

public class YearCellEditor extends IntegralCellEditor {

    public YearCellEditor(EditManager editManager, GPropertyDraw property) {
        super(GIntegerType.instance, editManager, property);
    }

    @Override
    public void start(EventHandler handler, Element parent, RenderContext renderContext, boolean notFocusable, PValue oldValue) {
        super.start(handler, parent, renderContext, notFocusable, oldValue);

        if (started) {
            openYearPicker(inputElement, parent, PValue.getIntegerValue(oldValue));

            if(oldValue == null) // if value is null - current date will be set, so we need to select the value, since we want to rewrite data on key input
                inputElement.select();

            GwtClientUtils.addDropDownPartner(parent, getYearPickerContainer(parent));
        }
    }

    @Override
    public void stop(Element parent, boolean cancel, boolean blurred) {
        if (started)
            hideYearPicker(parent);
        super.stop(parent, cancel, blurred);
    }

    protected void pickerApply(Element parent) {
        commit(parent);
    }

    protected native void openYearPicker(Element inputElement, Element parent, Integer initYear)/*-{
        var thisObj = this;

        var startDate;
        if(initYear != null)
            startDate = new Date().setFullYear(initYear);
        else { // the air datepicker differs from the "regular" datepicker - it doesn't set the value of start date to the input element, so we have to do it manually
            startDate = new Date();
            inputElement.value = startDate.getFullYear();
        }

        inputElement.addEventListener('keydown', function (e) {
            // stop Escape propagation because there is some magic inside the AirDatepicker when it is pressed, which prevents our logic from working properly (as if esc triggers 2 times).
            // stop Enter propagation because when you press it, the wrong date is written to the inputElement because ActionOrPropertyValue.setValue is called before the library writes the new date to inputElement.
            if (parent.picker.visible && ('Escape' === e.key || 'Enter' === e.key))
                e.stopPropagation();
        });

        parent.picker = new $wnd.AirDatepicker(inputElement, {
            view: 'years', // displaying the years of one decade
            minView: 'years', // The minimum possible representation of the calendar. It is used, for example, when you need to provide only a choice of the year.
            visible: true, // Shows the calendar immediately after initialization.
            startDate: startDate,
            dateFormat: function (date) {
                return date.getFullYear(); // to return a number, not a Date object
            },
            onSelect: function() {
                thisObj.@YearCellEditor::pickerApply(*)(parent);
                parent.picker.hide(); // to hide popup on Enter pressed
            },
            onHide: function (isFinished) {
                if (isFinished)
                    parent.picker.destroy();
            }
        });
    }-*/;

    protected native void hideYearPicker(Element parent)/*-{
        //If call destroy() here, we get an error, because destroy() can be called after the picker is completely closed. destroy() is called inside onHide
        parent.picker.hide();
    }-*/;

    protected native Element getYearPickerContainer(Element parent)/*-{
        return parent.picker.$datepicker;
    }-*/;
}
