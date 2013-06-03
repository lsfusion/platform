package lsfusion.interop.action;

import java.io.IOException;

public abstract class ExecuteClientAction implements ClientAction {

    public final Object dispatch(ClientActionDispatcher dispatcher) throws IOException {
        execute(dispatcher);
        return null;
    }

    public abstract void execute(ClientActionDispatcher dispatcher) throws IOException;
}
