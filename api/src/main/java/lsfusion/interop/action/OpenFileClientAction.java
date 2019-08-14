package lsfusion.interop.action;

import lsfusion.base.file.RawFileData;

import java.io.IOException;

public class OpenFileClientAction extends ExecuteClientAction {

    public final RawFileData file;
    public final String name;
    public final String extension;

    public OpenFileClientAction(RawFileData file, String name, String extension) {
        this.file = file;
        this.name = name;
        this.extension = extension;
    }

    @Override
    public void execute(ClientActionDispatcher dispatcher) {
        dispatcher.execute(this);
    }
}
