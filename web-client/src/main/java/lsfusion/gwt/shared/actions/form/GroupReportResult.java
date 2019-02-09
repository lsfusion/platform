package lsfusion.gwt.shared.actions.form;

import net.customware.gwt.dispatch.shared.Result;

public class GroupReportResult implements Result {
    
    public String filename;
    public String extension;

    public GroupReportResult() {
    }

    public GroupReportResult(String filename, String extension) {
        this.filename = filename;
        this.extension = extension;
    }
}
