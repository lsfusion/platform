package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.view.EventHandler;
import lsfusion.gwt.client.classes.data.GIntegerType;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;

import java.text.ParseException;

public class YearCellEditor extends IntegralCellEditor {

    public YearCellEditor(EditManager editManager, GPropertyDraw property) {
        super(GIntegerType.instance, editManager, property);
    }

    @Override
    public void start(EventHandler handler, Element parent, RenderContext renderContext, boolean notFocusable, PValue oldValue) {
        super.start(handler, parent, renderContext, notFocusable, oldValue);

        if (started) {
            openYearPicker(inputElement, PValue.getStringValue(oldValue));

            GwtClientUtils.addDropDownPartner(parent, getYearPickerContainer(parent));
        }
    }

    protected void pickerApply(Element parent) {
        commit(parent);
    }

    protected PValue tryParseInputText(String inputText, boolean onCommit) throws ParseException {
        //to be able to enter the year from keyboard
        if (!onCommit)
            return PValue.getPValue(inputText);

        return super.tryParseInputText(inputText, onCommit);
    }

    protected native void openYearPicker(Element inputElement, String value)/*-{
        window.$ = $wnd.jQuery;
        var thisObj = this;

        var yearpicker = $(inputElement).data("yearpicker");
        yearpicker.year = value;
        yearpicker.options.onHide = function (value) {
            if (value != null && value.toString() !== this.year) {
                thisObj.@YearCellEditor::pickerApply(*)(parent);
            }
        }
        yearpicker.renderYear();
    }-*/;

    protected native Element getYearPickerContainer(Element parent)/*-{
        return parent.getElementsByClassName("yearpicker-container")[0];
    }-*/;
}
