package lsfusion.interop.form.property;

import java.io.Serializable;

public class OpenForm implements Serializable {
    public String caption;
    public boolean modal;

    public OpenForm(String caption, boolean modal) {
        this.caption = caption;
        this.modal = modal;
    }
}