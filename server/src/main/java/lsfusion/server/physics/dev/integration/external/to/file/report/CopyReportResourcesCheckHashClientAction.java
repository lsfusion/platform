package lsfusion.server.physics.dev.integration.external.to.file.report;

import lsfusion.interop.action.ClientAction;
import lsfusion.interop.action.ClientActionDispatcher;

import java.io.File;
import java.io.IOException;

public class CopyReportResourcesCheckHashClientAction implements ClientAction {
    public final String md5;

    public CopyReportResourcesCheckHashClientAction(String md5) {
        this.md5 = md5;
    }

    @Override
    public Object dispatch(ClientActionDispatcher dispatcher) throws IOException {
        return new File(new File(System.getProperty("java.io.tmpdir"), "jasper-fonts"), md5).exists();
    }
}