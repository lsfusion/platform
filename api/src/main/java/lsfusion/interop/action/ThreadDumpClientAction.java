package lsfusion.interop.action;

import java.io.IOException;

public class ThreadDumpClientAction implements ClientAction {

    public ThreadDumpClientAction() {
    }

    @Override
    public Object dispatch(ClientActionDispatcher dispatcher) {
        return dispatcher.execute(this);
    }
}