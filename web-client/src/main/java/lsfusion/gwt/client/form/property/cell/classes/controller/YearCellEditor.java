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
    public boolean startText(EventHandler handler, Element parent, RenderContext renderContext, PValue oldValue) {
        boolean explicitValue = super.startText(handler, parent, renderContext, oldValue);

        if (started) {
            openYearPicker(inputElement, parent, PValue.getIntegerValue(oldValue), !explicitValue && oldValue == null);

            GwtClientUtils.addDropDownPartner(parent, getYearPickerContainer(parent));
        }

        return explicitValue;
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

    protected native void openYearPicker(Element inputElement, Element parent, Integer initYear, boolean updateInput)/*-{
        var thisObj = this;

        var startDate;
        if(initYear != null)
            startDate = new Date().setFullYear(initYear);
        else if (updateInput) { // the air datepicker differs from the "regular" datepicker - it doesn't set the value of start date to the input element, so we have to do it manually
            startDate = new Date();
            inputElement.value = startDate.getFullYear();
        }

        parent.picker = new $wnd.AirDatepicker(inputElement, {
            view: 'years', // displaying the years of one decade
            minView: 'years', // The minimum possible representation of the calendar. It is used, for example, when you need to provide only a choice of the year.
            visible: true, // Shows the calendar immediately after initialization.
            startDate: startDate,
            keyboardNav: false,
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

        thisObj.@InputBasedCellEditor::selectInputElement(*)(updateInput);
    }-*/;

    protected native void hideYearPicker(Element parent)/*-{
        //If call destroy() here, we get an error, because destroy() can be called after the picker is completely closed. destroy() is called inside onHide
        parent.picker.hide();
    }-*/;

    protected native Element getYearPickerContainer(Element parent)/*-{
        return parent.picker.$datepicker;
    }-*/;
}
