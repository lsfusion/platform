package platform.gwt.login.shared.actions;

import net.customware.gwt.dispatch.shared.Action;
import platform.gwt.base.shared.actions.VoidResult;

public class RemindPassword implements Action<VoidResult> {
    public String email;

    public RemindPassword() {
    }

    public RemindPassword(String email) {
        this.email = email;
    }

}
