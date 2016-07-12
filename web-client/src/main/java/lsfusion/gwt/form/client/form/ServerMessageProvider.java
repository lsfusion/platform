package lsfusion.gwt.form.client.form;

import lsfusion.gwt.base.shared.actions.ListResult;
import net.customware.gwt.dispatch.shared.general.StringResult;
import lsfusion.gwt.base.client.ErrorHandlingCallback;

public interface ServerMessageProvider {
    void getServerActionMessage(ErrorHandlingCallback<StringResult> callback);
    void getServerActionMessageList(ErrorHandlingCallback<ListResult> callback);
    void interrupt(boolean cancelable);
}
