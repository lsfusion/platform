package lsfusion.interop.action;

import java.io.IOException;

public class LoadLinkClientAction implements ClientAction {

    public LoadLinkClientAction() {
    }

    @Override
    public Object dispatch(ClientActionDispatcher dispatcher) {
        return dispatcher.execute(this);
    }
}