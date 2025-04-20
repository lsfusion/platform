package lsfusion.interop.action;

import com.google.common.base.Throwables;
import lsfusion.base.SystemUtils;

import java.io.IOException;

public class RunCommandClientAction implements ClientAction {
    public final String command;
    public final String directory;
    public final boolean wait;

    public RunCommandClientAction(String command, String directory, boolean wait) {
        this.command = command;
        this.directory = directory;
        this.wait = wait;
    }

    public Object dispatch(ClientActionDispatcher dispatcher) throws IOException {
        try {
            return SystemUtils.runCmd(command, directory, wait);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }
}