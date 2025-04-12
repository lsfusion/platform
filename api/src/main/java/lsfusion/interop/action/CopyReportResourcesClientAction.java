package lsfusion.interop.action;

import lsfusion.base.file.FileData;

import java.io.IOException;

public class CopyReportResourcesClientAction implements ClientAction {
    public final String logicsName;
    public final FileData zipFile;
    public final String md5;

    public CopyReportResourcesClientAction(String logicsName, FileData zipFile, String md5) {
        this.logicsName = logicsName;
        this.zipFile = zipFile;
        this.md5 = md5;
    }

    @Override
    public Object dispatch(ClientActionDispatcher dispatcher) throws IOException {
        return dispatcher.execute(this);
    }
}