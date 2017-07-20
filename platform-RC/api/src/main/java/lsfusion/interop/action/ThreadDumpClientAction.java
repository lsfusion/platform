package lsfusion.interop.action;

import java.io.IOException;

public class ThreadDumpClientAction implements ClientAction {

    public ThreadDumpClientAction() {
    }

    @Override
    public Object dispatch(ClientActionDispatcher dispatcher) throws IOException {
        return dispatcher.execute(this);
    }
}