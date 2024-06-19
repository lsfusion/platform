package lsfusion.gwt.client.form.property.cell.classes.view;

import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.KeyCodes;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.cell.view.RendererType;
import lsfusion.gwt.client.form.property.cell.view.UpdateContext;

public class YearCellRenderer extends IntegralCellRenderer {
    public YearCellRenderer(GPropertyDraw property) {
        super(property);
    }

    @Override
    public boolean updateContent(Element element, PValue value, Object extraValue, UpdateContext updateContext) {
        Element inputElement = getInputElement(element);
        if (inputElement != null) {
            inputElement.setAttribute("type", "number");
            inputElement.addClassName("yearpicker");
            inputElement.getStyle().setProperty("textAlign", "right");

            initYearPicker(inputElement, PValue.getStringValue(value));
        }

        return super.updateContent(element, value, extraValue, updateContext);
    }

    protected native void initYearPicker(Element inputElement, String value)/*-{
        inputElement.addEventListener("keydown", function (ev) {
            console.log(ev.key);
            if(ev.key === 'Enter') {
                var yearpicker = $wnd.$(inputElement).data("yearpicker");
                yearpicker.year = $wnd.$(inputElement).val();
                yearpicker.hideView();
                ev.stopPropagation();
            }
        });
        $wnd.$(inputElement).yearpicker();

    }-*/;

    @Override
    public String format(PValue value, RendererType rendererType, String pattern) {
        return PValue.getStringValue(value);
    }
}
