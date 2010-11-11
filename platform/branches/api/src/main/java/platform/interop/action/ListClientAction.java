package platform.interop.action;

import java.util.List;
import java.io.IOException;

public class ListClientAction extends AbstractClientAction {

    List<ClientAction> actions;
    public ListClientAction(List<ClientAction> actions) {
        this.actions = actions;
    }

    public void dispatch(ClientActionDispatcher dispatcher) throws IOException {
        for(ClientAction action : actions)
            action.dispatch(dispatcher);
    }
}
