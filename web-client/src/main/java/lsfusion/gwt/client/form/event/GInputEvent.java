package lsfusion.gwt.client.form.event;

import java.io.Serializable;
import java.util.Map;

public class GInputEvent implements Serializable {
    public Map<String, GBindingMode> bindingModes;

    public GInputEvent() {
    }

    public GInputEvent(Map<String, GBindingMode> bindingModes) {
        this.bindingModes = bindingModes;
    }
}
