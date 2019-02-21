package lsfusion.gwt.shared.actions.logics;

import lsfusion.gwt.shared.actions.RequestAction;
import net.customware.gwt.dispatch.shared.Result;

public class LogicsAction<R extends Result> extends RequestAction<R> {

    public String host;
    public Integer port;
    public String exportName;
}
