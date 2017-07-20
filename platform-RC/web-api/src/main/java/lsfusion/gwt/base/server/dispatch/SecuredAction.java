package lsfusion.gwt.base.server.dispatch;

import net.customware.gwt.dispatch.shared.Action;
import net.customware.gwt.dispatch.shared.Result;

public interface SecuredAction<R extends Result> extends Action<R> {
    boolean isAllowed();
}
