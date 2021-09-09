package lsfusion.gwt.client.view;

import lsfusion.gwt.client.base.result.ListResult;
import lsfusion.gwt.client.controller.remote.action.PriorityErrorHandlingCallback;
import net.customware.gwt.dispatch.shared.general.StringResult;

public interface ServerMessageProvider {
    void getServerActionMessage(PriorityErrorHandlingCallback<StringResult> callback);
    void getServerActionMessageList(PriorityErrorHandlingCallback<ListResult> callback);
    void interrupt(boolean cancelable);
}
