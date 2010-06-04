package platform.interop.action;

import java.io.IOException;

public class RuntimeClientAction extends ClientAction<RuntimeClientActionResult> {

    public String command;
    public String[] environment;
    public String directory;

    public boolean waitFor = true;

    public byte[] input;

    public RuntimeClientAction(String command, String[] environment, String directory) {
        
        this.command = command;
        this.environment = environment;
        this.directory = directory;
    }

    public RuntimeClientActionResult dispatch(ClientActionDispatcher dispatcher) throws IOException {
        return dispatcher.executeRuntime(this);
    }
}
