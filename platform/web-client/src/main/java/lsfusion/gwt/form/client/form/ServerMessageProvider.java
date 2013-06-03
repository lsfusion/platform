package lsfusion.gwt.form.client.form;

import net.customware.gwt.dispatch.shared.general.StringResult;
import lsfusion.gwt.base.client.ErrorHandlingCallback;

public interface ServerMessageProvider {
    void getServerActionMessage(ErrorHandlingCallback<StringResult> callback);
}
