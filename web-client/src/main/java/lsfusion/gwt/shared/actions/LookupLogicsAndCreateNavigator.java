package lsfusion.gwt.shared.actions;

import lsfusion.gwt.shared.result.RequestAction;
import net.customware.gwt.dispatch.shared.general.StringResult;

public class LookupLogicsAndCreateNavigator extends RequestAction<StringResult> {

    public String host;
    public Integer port;
    public String exportName;

    public LookupLogicsAndCreateNavigator() {
    }

    public LookupLogicsAndCreateNavigator(String host, Integer port, String exportName) {
        this.host = host;
        this.port = port;
        this.exportName = exportName;
    }
}
