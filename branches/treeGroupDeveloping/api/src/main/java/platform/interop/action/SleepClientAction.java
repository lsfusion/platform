package platform.interop.action;

import java.io.IOException;

public class SleepClientAction extends AbstractClientAction {

    public long millis;

    public SleepClientAction(long millis) {
        this.millis = millis;
    }

    @Override
    public void dispatch(ClientActionDispatcher dispatcher) throws IOException {
        dispatcher.execute(this);
    }
}
