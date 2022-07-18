package lsfusion.gwt.client.base.view;

import com.google.gwt.user.client.ui.RadioButton;

public class FormRadioButton extends RadioButton {
    public FormRadioButton(String name, String label) {
        super(name, label);
        
        setStyleName("form-check");
        getElement().getElementsByTagName("input").getItem(0).addClassName("form-check-input");
        getElement().getElementsByTagName("label").getItem(0).addClassName("form-check-label");
    }
}
