package lsfusion.interop.action;

import lsfusion.base.file.FileData;

import java.io.IOException;
import java.util.Map;

public class ChangeSizeClientAction extends ExecuteClientAction {
    public Map<String, FileData> resources;

    public ChangeSizeClientAction(Map<String, FileData> resources) {
        this.resources = resources;
    }

    @Override
    public void execute(ClientActionDispatcher dispatcher) throws IOException {
        //do nothing in desktop
    }
}