package lsfusion.interop.action;

import java.io.IOException;
import java.util.Map;

public class UserLogsClientAction implements ClientAction {

    public UserLogsClientAction() {
    }

    @Override
    public Map<String, byte[]> dispatch(ClientActionDispatcher dispatcher) throws IOException {
        return dispatcher.execute(this);
    }
}