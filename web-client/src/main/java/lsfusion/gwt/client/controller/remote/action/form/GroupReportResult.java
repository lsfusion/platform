package lsfusion.gwt.client.controller.remote.action.form;

import net.customware.gwt.dispatch.shared.Result;

public class GroupReportResult implements Result {
    
    public String filename;

    public GroupReportResult() {
    }

    public GroupReportResult(String filename) {
        this.filename = filename;
    }
}
