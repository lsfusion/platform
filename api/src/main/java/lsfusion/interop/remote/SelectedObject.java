package lsfusion.interop.remote;

import java.io.Serializable;

public class SelectedObject implements Serializable {
    public final Object value;
    public final Object displayValue;

    public SelectedObject(Object value, Object displayValue) {
        this.value = value;
        this.displayValue = displayValue;
    }
}
