package lsfusion.gwt.client.base.view;

import com.google.gwt.user.client.ui.CheckBox;

public class FormCheckBox extends CheckBox {

    public FormCheckBox(String label) {
        super(label);

        setStyleName("form-check");
        getElement().getElementsByTagName("input").getItem(0).addClassName("form-check-input");
        getElement().getElementsByTagName("label").getItem(0).addClassName("form-check-label");
    }
}
