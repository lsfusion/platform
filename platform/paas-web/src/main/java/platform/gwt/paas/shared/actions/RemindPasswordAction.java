package platform.gwt.paas.shared.actions;

import com.gwtplatform.dispatch.shared.UnsecuredActionImpl;

public class RemindPasswordAction extends UnsecuredActionImpl<VoidResult> {
    public String email;
    
    public RemindPasswordAction() {}
    
    public RemindPasswordAction(String email) {
        this.email = email;
    }
}
