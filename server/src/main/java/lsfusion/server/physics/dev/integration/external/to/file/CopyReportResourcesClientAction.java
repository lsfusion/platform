package lsfusion.server.physics.dev.integration.external.to.file;

import com.google.common.base.Throwables;
import lsfusion.base.file.RawFileData;
import lsfusion.interop.action.ClientActionDispatcher;
import lsfusion.interop.action.ExecuteClientAction;

import java.io.File;
import java.util.Map;

public class CopyReportResourcesClientAction extends ExecuteClientAction {
    public final Map<String, RawFileData> files;

    public CopyReportResourcesClientAction(Map<String, RawFileData> files) {
        this.files = files;
    }

    @Override
    public void execute(ClientActionDispatcher dispatcher) {
        try {
            for (Map.Entry<String, RawFileData> file : files.entrySet()) {
                file.getValue().write(new File(file.getKey()));
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }
}