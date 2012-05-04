package platform.interop.action;

import java.util.ArrayList;
import java.util.List;
import java.io.IOException;

public class ListClientAction implements ClientAction {

    private final List<ClientAction> actions;

    public ListClientAction(List<ClientAction> actions) {
        this.actions = actions;
    }

    @Override
    public Object dispatch(ClientActionDispatcher dispatcher) throws IOException {
        List<Object> result = new ArrayList<Object>();
        for (ClientAction action : actions) {
            result.add(action.dispatch(dispatcher));
        }
        return result;
    }
}
