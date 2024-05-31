package lsfusion.interop.action;

import lsfusion.base.file.FileData;

import java.io.IOException;

public class LoadResourceClientAction extends ExecuteClientAction {
    public String resourceName;
    public FileData resourceFile;

    public LoadResourceClientAction(String resourceName, FileData resourceFile) {
        this.resourceName = resourceName;
        this.resourceFile = resourceFile;
    }

    @Override
    public void execute(ClientActionDispatcher dispatcher) throws IOException {
        dispatcher.execute(this);
    }
}