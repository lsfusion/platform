package lsfusion.interop.action;

import lsfusion.base.file.RawFileData;

import java.io.IOException;

public class BeepClientAction extends ExecuteClientAction {

    public RawFileData file;
    public boolean async;

    public BeepClientAction(RawFileData file, boolean async) {
        this.file = file;
        this.async = async;
    }

    public void execute(ClientActionDispatcher dispatcher) {
        dispatcher.execute(this);
    }
}