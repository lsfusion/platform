package lsfusion.interop.action;

import java.io.IOException;

public class LoadLinkClientAction implements ClientAction {

    public LoadLinkClientAction() {
    }

    @Override
    public Object dispatch(ClientActionDispatcher dispatcher) throws IOException {
        return dispatcher.execute(this);
    }
}