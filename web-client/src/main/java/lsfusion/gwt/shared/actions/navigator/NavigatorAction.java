package lsfusion.gwt.shared.actions.navigator;

import lsfusion.gwt.shared.actions.RequestAction;
import net.customware.gwt.dispatch.shared.Result;

public class NavigatorAction<R extends Result> extends RequestAction<R> {
    public String sessionID;
}
