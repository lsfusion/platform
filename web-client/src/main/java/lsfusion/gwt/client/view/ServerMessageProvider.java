package lsfusion.gwt.client.view;

import lsfusion.gwt.client.base.exception.ErrorHandlingCallback;
import lsfusion.gwt.client.base.result.ListResult;
import net.customware.gwt.dispatch.shared.general.StringResult;

public interface ServerMessageProvider {
    void getServerActionMessage(ErrorHandlingCallback<StringResult> callback);
    void getServerActionMessageList(ErrorHandlingCallback<ListResult> callback);
    void interrupt(boolean cancelable);
}
