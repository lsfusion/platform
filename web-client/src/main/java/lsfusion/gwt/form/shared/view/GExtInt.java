package lsfusion.gwt.form.shared.view;

import java.io.Serializable;

public class GExtInt implements Serializable {

    public int value;  // -1, бесконечность

    public GExtInt() {
    }

    public GExtInt(int value) {
        this.value = value;
    }

    public boolean isUnlimited() {
        return value == -1;
    }

    public int getValue() {
        assert !isUnlimited();
        return value;
    }
}
