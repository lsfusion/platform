package lsfusion.server.physics.admin.interpreter.action;

import com.google.common.base.Throwables;
import lsfusion.interop.action.ClientAction;
import lsfusion.interop.action.ClientActionDispatcher;
import lsfusion.server.physics.dev.integration.external.to.file.FileUtils;

import java.io.IOException;

public class RunCommandClientAction implements ClientAction {
    private final String command;
    private final String directory;
    private final boolean wait;

    public RunCommandClientAction(String command, String directory, boolean wait) {
        this.command = command;
        this.directory = directory;
        this.wait = wait;
    }

    public Object dispatch(ClientActionDispatcher dispatcher) throws IOException {
        try {
            return FileUtils.runCmd(command, directory, wait);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }
}