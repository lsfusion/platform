package lsfusion.gwt.form.client.form;

import lsfusion.gwt.base.shared.actions.ListResult;
import lsfusion.gwt.form.client.ErrorHandlingCallback;
import net.customware.gwt.dispatch.shared.general.StringResult;

public interface ServerMessageProvider {
    void getServerActionMessage(ErrorHandlingCallback<StringResult> callback);
    void getServerActionMessageList(ErrorHandlingCallback<ListResult> callback);
    void interrupt(boolean cancelable);
}
