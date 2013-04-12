package platform.gwt.paas.shared.actions;

import net.customware.gwt.dispatch.shared.Result;

public class AddUserResult implements Result {
    public String result;
    
    public AddUserResult() {}
    
    public AddUserResult(String result) {
        this.result = result;
    }
}
