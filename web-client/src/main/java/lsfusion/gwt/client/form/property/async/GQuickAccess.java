package lsfusion.gwt.client.form.property.async;

import java.io.Serializable;

public class GQuickAccess implements Serializable {
    public GQuickAccessMode mode;
    public Boolean hover;

    @SuppressWarnings("unused")
    public GQuickAccess() {
    }

    public GQuickAccess(GQuickAccessMode mode, Boolean hover) {
        this.mode = mode;
        this.hover = hover;
    }
}