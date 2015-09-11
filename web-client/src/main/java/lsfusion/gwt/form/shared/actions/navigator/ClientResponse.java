package lsfusion.gwt.form.shared.actions.navigator;

import lsfusion.gwt.base.shared.actions.NavigatorAction;
import net.customware.gwt.dispatch.shared.Action;

public class ClientResponse implements Action<ClientResponseResult>, NavigatorAction {
    public Integer clientResponse;
    public ClientResponse() {
    }
    public ClientResponse(Integer clientResponse) {
        this.clientResponse = clientResponse;
    }
}
