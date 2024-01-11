package lsfusion.gwt.client.form.property.cell.classes.view;

import com.google.gwt.dom.client.*;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.view.SizedFlexPanel;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.cell.view.RendererType;
import lsfusion.gwt.client.form.property.cell.view.UpdateContext;

public class LogicalCellRenderer extends InputBasedCellRenderer {

    private boolean threeState;

    public LogicalCellRenderer(GPropertyDraw property, boolean threeState) {
        super(property);
        this.threeState = threeState;
        assert isTagInput();
    }

    @Override
    public void renderPanelLabel(Widget label) {
//        label.addStyleName("form-check-label");
    }

    @Override
    public void renderPanelContainer(SizedFlexPanel panel) {
        // we're not setting form-check since it's mostly used only for layouting, which we do ourselves
//        panel.addStyleName("form-check");
    }

    @Override
    public boolean updateContent(Element element, PValue value, Object extraValue, UpdateContext updateContext) {
        InputElement input = getInputElement(element);

        boolean newValue;
        if(threeState) {
            Boolean value3s = get3sBooleanValue(value);
            newValue = value3s != null && value3s;
            setIndeterminate(input, value3s == null);
        } else
            newValue = getBooleanValue(value);

        input.setChecked(newValue);
        input.setDefaultChecked(newValue);

        return false;
    }

    private native void setIndeterminate(InputElement element, boolean indeterminate) /*-{
        element.indeterminate = indeterminate;
    }-*/;

    public static void cancelChecked(InputElement input) {
        input.setChecked(input.isDefaultChecked());
    }

    //    private String getCBImagePath(Object value) {
//        boolean checked = value != null && (Boolean) value;
//        return GwtClientUtils.getModuleImagePath("checkbox_" + (checked ? "checked" : "unchecked") + ".png");
//    }

    @Override
    public String format(PValue value, RendererType rendererType, String pattern) {
        if(threeState) {
            Boolean value3s = get3sBooleanValue(value);
            return value3s != null ? (value3s ? "TRUE" : "FALSE") : "NULL";
        } else
            return getBooleanValue(value) ? "TRUE" : "FALSE";
    }

    private boolean getBooleanValue(PValue value) {
        return PValue.getBooleanValue(value);
    }

    private Boolean get3sBooleanValue(PValue value) {
        return PValue.get3SBooleanValue(value);
    }
}
