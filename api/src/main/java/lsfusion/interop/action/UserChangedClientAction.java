package lsfusion.interop.action;

import java.io.IOException;

public class UserChangedClientAction extends ExecuteClientAction {

    @Override
    public void execute(ClientActionDispatcher dispatcher) {
        dispatcher.execute(this);
    }
}
