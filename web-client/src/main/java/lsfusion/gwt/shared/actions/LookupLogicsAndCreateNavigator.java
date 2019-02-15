package lsfusion.gwt.shared.actions;

import net.customware.gwt.dispatch.shared.general.StringResult;

public class LookupLogicsAndCreateNavigator extends RequestAction<StringResult> {

    public String host;
    public Integer port;
    public String exportName;
    
    public String logicsName = "default";

    public LookupLogicsAndCreateNavigator() {
    }

    public LookupLogicsAndCreateNavigator(String host, Integer port, String exportName) {
        this.host = host;
        this.port = port;
        this.exportName = exportName;
    }
}
