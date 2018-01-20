package lsfusion.server.logics.property.actions;

import com.google.common.base.Throwables;
import lsfusion.interop.action.ClientAction;
import lsfusion.interop.action.ClientActionDispatcher;

import java.io.IOException;


public class PostReadClientAction implements ClientAction {

    String sourcePath;
    String type;
    String filePath;
    String movePath;
    boolean delete;

    public PostReadClientAction(String sourcePath, String type, String filePath, String movePath, boolean delete) {
        this.sourcePath = sourcePath;
        this.type = type;
        this.filePath = filePath;
        this.movePath = movePath;
        this.delete = delete;
    }

    public Object dispatch(ClientActionDispatcher dispatcher) throws IOException {
        try {
            ReadUtils.postProcessFile(sourcePath, type, filePath, movePath, delete);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
        return null;
    }
}