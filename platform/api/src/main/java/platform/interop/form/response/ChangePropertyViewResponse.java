package platform.interop.form.response;

import platform.interop.action.ClientAction;

import java.io.Serializable;
import java.util.List;

public class ChangePropertyViewResponse implements Serializable {

    public List<ClientAction> actions;
    public byte[] formChanges;

    public ChangePropertyViewResponse(List<ClientAction> actions, byte[] formChanges) {
        this.actions = actions;
        this.formChanges = formChanges;
    }
}
