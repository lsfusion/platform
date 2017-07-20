package lsfusion.gwt.login.shared.actions;

import net.customware.gwt.dispatch.shared.Action;
import lsfusion.gwt.base.shared.actions.VoidResult;

public class RemindPassword implements Action<VoidResult> {
    public String email;

    public RemindPassword() {
    }

    public RemindPassword(String email) {
        this.email = email;
    }

}
