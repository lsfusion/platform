package lsfusion.interop.action;

import lsfusion.base.file.RawFileData;

import java.io.IOException;
import java.util.Map;

public class UserLogsClientAction implements ClientAction {

    public UserLogsClientAction() {
    }

    @Override
    public Map<String, RawFileData> dispatch(ClientActionDispatcher dispatcher) {
        return dispatcher.execute(this);
    }
}