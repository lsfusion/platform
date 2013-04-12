package platform.gwt.paas.shared.actions;

import net.customware.gwt.dispatch.shared.Action;
import platform.gwt.base.shared.actions.VoidResult;

public class RemindPasswordAction implements Action<VoidResult> {
    public String email;
    
    public RemindPasswordAction() {}
    
    public RemindPasswordAction(String email) {
        this.email = email;
    }
}
