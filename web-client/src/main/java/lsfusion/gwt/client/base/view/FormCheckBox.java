package lsfusion.gwt.client.base.view;

import com.google.gwt.user.client.ui.CheckBox;
import lsfusion.gwt.client.base.GwtClientUtils;

public class FormCheckBox extends CheckBox {

    public FormCheckBox(String label) {
        super(label);

        GwtClientUtils.addClassName(this, "form-check");
        GwtClientUtils.addClassName(getElement().getElementsByTagName("input").getItem(0), "form-check-input");
        GwtClientUtils.addClassName(getElement().getElementsByTagName("input").getItem(0), "panel-renderer-value", "panelRendererValue");
        GwtClientUtils.addClassName(getElement().getElementsByTagName("label").getItem(0), "form-check-label");
    }
}
