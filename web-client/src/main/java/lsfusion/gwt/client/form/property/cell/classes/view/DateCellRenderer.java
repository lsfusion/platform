package lsfusion.gwt.client.form.property.cell.classes.view;

import lsfusion.gwt.client.classes.GFullInputType;
import lsfusion.gwt.client.form.property.GPropertyDraw;

public class DateCellRenderer extends FormatCellRenderer<Object> {

    public DateCellRenderer(GPropertyDraw property) {
        super(property);
    }

    @Override
    protected boolean isNative(GFullInputType fullInputType) {
        return fullInputType.inputType.hasNativePopup();
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
