package lsfusion.server.physics.admin.interpreter.action;

import lsfusion.interop.action.ClientAction;
import lsfusion.interop.action.ClientActionDispatcher;
import lsfusion.server.physics.dev.integration.external.to.file.FileUtils;

public class RunCommandClientAction implements ClientAction {
    private String command;
    private String directory;

    public RunCommandClientAction(String command, String directory) {
        this.command = command;
        this.directory = directory;
    }

    public Object dispatch(ClientActionDispatcher dispatcher) {
        try {
            return FileUtils.runCmd(command, directory);
        } catch (Exception e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }
}