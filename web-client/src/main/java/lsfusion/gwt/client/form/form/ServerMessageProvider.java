package lsfusion.gwt.client.form.form;

import lsfusion.gwt.shared.base.actions.ListResult;
import lsfusion.gwt.client.form.ErrorHandlingCallback;
import net.customware.gwt.dispatch.shared.general.StringResult;

public interface ServerMessageProvider {
    void getServerActionMessage(ErrorHandlingCallback<StringResult> callback);
    void getServerActionMessageList(ErrorHandlingCallback<ListResult> callback);
    void interrupt(boolean cancelable);
}
