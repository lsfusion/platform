package lsfusion.gwt.client.controller.remote.action.logics;

import lsfusion.gwt.client.controller.remote.action.BaseAction;
import net.customware.gwt.dispatch.shared.Result;

public class LogicsAction<R extends Result> extends BaseAction<R> {

    public String host;
    public Integer port;
    public String exportName;
}
