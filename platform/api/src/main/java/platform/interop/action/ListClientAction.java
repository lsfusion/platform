package platform.interop.action;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
