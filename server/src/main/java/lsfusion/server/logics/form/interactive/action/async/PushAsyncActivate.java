package lsfusion.server.logics.form.interactive.action.async;

import lsfusion.interop.form.FormActivateType;

import static lsfusion.base.BaseUtils.nullEquals;

public class PushAsyncActivate extends PushAsyncResult {
    private final String canonicalName;
    private final String formId;
    private final String window;
    private final FormActivateType activateType;

    public PushAsyncActivate(String canonicalName, String formId, String window, FormActivateType activateType) {
        this.canonicalName = canonicalName;
        this.formId = formId;
        this.window = window;
        this.activateType = activateType;
    }

    public boolean matches(String canonicalName, String formId, String window, FormActivateType activateType) {
        return canonicalName != null && activateType != null
                && canonicalName.equals(this.canonicalName) && nullEquals(formId, this.formId)
                && window.equals(this.window) && activateType == this.activateType;
    }
}
