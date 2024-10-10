package lsfusion.gwt.client.base.view;

import com.google.gwt.user.client.ui.RadioButton;
import lsfusion.gwt.client.base.GwtClientUtils;

public class FormRadioButton extends RadioButton {
    public FormRadioButton(String name, String label) {
        super(name, label);
        
        GwtClientUtils.addClassName(this, "form-check");
        GwtClientUtils.addClassName(getElement().getElementsByTagName("input").getItem(0), "form-check-input");
        GwtClientUtils.addClassName(getElement().getElementsByTagName("label").getItem(0), "form-check-label");
    }
}
