package lsfusion.gwt.client.form.property.cell.classes.view;

import com.google.gwt.core.client.JsDate;
import com.google.gwt.dom.client.InputElement;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.classes.data.GADateType;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.cell.view.RendererType;

public class DateCellRenderer extends FormatCellRenderer<Object> {
    private final GADateType type;

    public DateCellRenderer(GPropertyDraw property, GADateType type) {
        super(property);

        this.type = type;
    }

    @Override
    public String format(PValue value, RendererType rendererType, String pattern) {
        if(rendererType == RendererType.CELL && isTagInput() && getInputType(rendererType).inputType.hasNativePopup()) {
            return type.formatISOString(value);
        }
        return super.format(value, rendererType, pattern);
    }

    // valueAsDate doesn't work for datetime + hours
//    public native static JsDate setDateInputValue(InputElement element, JsDate date)/*-{
//        element.valueAsDate = date;
//    }-*/;
//
//    @Override
//    protected void updateInputContent(InputElement inputElement, String innerText, PValue value, RendererType rendererType) {
//        if(isTagInput() && getInputType(rendererType).hasNativePopup()) {
//            setDateInputValue(inputElement, GwtClientUtils.toJsDate(value != null ? type.toDate(value) : null));
//            return;
//        }
//
//        super.updateInputContent(inputElement, innerText, value, rendererType);
//    }
}
