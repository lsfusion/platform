package lsfusion.interop.form.event;

import java.io.Serializable;
import java.util.Map;

public class InputEvent implements Serializable {
    public Map<String, BindingMode> bindingModes;

    public InputEvent() {
    }

    public InputEvent(Map<String, BindingMode> bindingModes) {
        this.bindingModes = bindingModes;
    }
}
