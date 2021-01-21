package lsfusion.gwt.client.form.property;

import java.io.Serializable;

public class GOpenForm implements Serializable {
    public String caption;
    public boolean modal;

    @SuppressWarnings("UnusedDeclaration")
    public GOpenForm() {
    }

    public GOpenForm(String caption, boolean modal) {
        this.caption = caption;
        this.modal = modal;
    }
}