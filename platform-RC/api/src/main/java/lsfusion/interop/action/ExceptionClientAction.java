package lsfusion.interop.action;

import java.io.IOException;

public class ExceptionClientAction extends ExecuteClientAction {
    public Exception e;

    public ExceptionClientAction(Exception e) {
        this.e = e;
    }

    @Override
    public void execute(ClientActionDispatcher dispatcher) throws IOException {
        dispatcher.execute(this);
    }
}