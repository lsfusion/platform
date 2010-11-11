package platform.interop.action;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ListClientResultAction extends AbstractClientAction {

    private List<ClientResultAction> actions;

    public ListClientResultAction(List<ClientResultAction> actions) {
        this.actions = actions;
    }

    @Override
    public Object dispatchResult(ClientActionDispatcher dispatcher) throws IOException {
        List<Object> result = new ArrayList<Object>();
        for(ClientResultAction action : actions)
            result.add(action.dispatchResult(dispatcher));
        return result;
    }
}
