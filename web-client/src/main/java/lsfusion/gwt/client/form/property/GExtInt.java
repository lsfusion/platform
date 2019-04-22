package lsfusion.gwt.client.form.property;

import java.io.Serializable;

public class GExtInt implements Serializable {

    public int value;  // -1, бесконечность

    public static final GExtInt UNLIMITED = new GExtInt(-1);

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

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
