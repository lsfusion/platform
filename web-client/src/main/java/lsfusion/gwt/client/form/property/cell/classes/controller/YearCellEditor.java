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
            openYearPicker(inputElement, nvl(PValue.getIntegerValue(oldValue), 0));
            GwtClientUtils.addDropDownPartner(parent, getYearPickerContainer(parent));
        }
    }

    @Override
    public void stop(Element parent, boolean cancel, boolean blurred) {
        if (started) {
            getYearPickerContainer(parent).removeFromParent();
            destroyYearPicker(inputElement);
        }
        super.stop(parent, cancel, blurred);
    }

    protected void pickerApply(Element parent) {
        commit(parent);
    }

    protected native void openYearPicker(Element inputElement, int initYear)/*-{
        window.$ = $wnd.jQuery;
        var thisObj = this;

        var yearpicker = $(inputElement).data("yearpicker");
        if(!yearpicker) {
            $(inputElement).yearpicker({
                onHide: function (value) {
                    if (value != null && value.toString() !== this.year) {
                        thisObj.@YearCellEditor::pickerApply(*)(parent);
                    }
                }, year: initYear
            });

            yearpicker = $(inputElement).data("yearpicker");
        }
        yearpicker.showView();
    }-*/;

    protected native void destroyYearPicker(Element inputElement)/*-{
        window.$ = $wnd.jQuery;
        $(inputElement).removeData("yearpicker");
    }-*/;

    protected native Element getYearPickerContainer(Element parent)/*-{
        return parent.getElementsByClassName("yearpicker-container")[0];
    }-*/;
}
