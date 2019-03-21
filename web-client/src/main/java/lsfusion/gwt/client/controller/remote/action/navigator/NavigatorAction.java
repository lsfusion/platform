package lsfusion.gwt.client.controller.remote.action.navigator;

import lsfusion.gwt.client.controller.remote.action.RequestAction;
import net.customware.gwt.dispatch.shared.Result;

public class NavigatorAction<R extends Result> extends RequestAction<R> {
    public String sessionID;
}
