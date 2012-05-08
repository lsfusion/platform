package platform.interop.action;

import java.io.IOException;
import java.util.List;

public class CheckFailed implements ClientAction {

    List<ClientAction> actions;
    public CheckFailed(List<ClientAction> actions) {
        this.actions = actions;
    }

    @Override
    public Object dispatch(ClientActionDispatcher dispatcher) throws IOException {
        for(ClientAction action : actions)
            action.dispatch(dispatcher);
        return null;
    }
}
