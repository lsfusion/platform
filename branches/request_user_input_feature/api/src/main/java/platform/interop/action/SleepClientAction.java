package platform.interop.action;

import java.io.IOException;

public class SleepClientAction extends ExecuteClientAction {

    public long millis;

    public SleepClientAction(long millis) {
        this.millis = millis;
    }

    @Override
    public void execute(ClientActionDispatcher dispatcher) throws IOException {
        dispatcher.execute(this);
    }
}
