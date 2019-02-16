package lsfusion.gwt.shared.actions;

import lsfusion.gwt.shared.actions.logics.LogicsAction;
import net.customware.gwt.dispatch.shared.general.StringResult;

public class CreateNavigatorAction extends LogicsAction<StringResult> {

    public String host;
    public Integer port;
    public String exportName;
    
    public CreateNavigatorAction() {
    }

    public CreateNavigatorAction(String host, Integer port, String exportName) {
        this.host = host;
        this.port = port;
        this.exportName = exportName;
    }
}
