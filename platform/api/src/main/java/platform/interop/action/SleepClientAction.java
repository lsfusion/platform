package platform.interop.action;

import java.io.IOException;

public class SleepClientAction extends ClientAction {

    public long millis;

    public SleepClientAction(long millis) {
        this.millis = millis;
    }

    public ClientActionResult dispatch(ClientActionDispatcher dispatcher) throws IOException {
        return dispatcher.execute(this);
    }
}
