package lsfusion.gwt.form.shared.actions.navigator;

import net.customware.gwt.dispatch.shared.Action;

public class ClientResponse implements Action<ClientResponseResult> {
    public Integer clientResponse;
    public ClientResponse() {
    }
    public ClientResponse(Integer clientResponse) {
        this.clientResponse = clientResponse;
    }
}
