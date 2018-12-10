package lsfusion.gwt.shared.form.actions;

import lsfusion.gwt.shared.base.actions.RequestAction;
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
