package platform.interop.action;

import java.util.List;
import java.util.ArrayList;
import java.io.IOException;

public class ListClientAction extends ClientAction {

    private List<ClientAction> actions;

    public ListClientAction(List<ClientAction> actions) {
        this.actions = actions;
    }

    public Object dispatch(ClientActionDispatcher dispatcher) throws IOException {
        List<Object> result = new ArrayList<Object>();
        for(ClientAction action : actions)
            result.add(action.dispatch(dispatcher));
        return result;
    }
}
