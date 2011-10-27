package platform.gwt.paas.client.common;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.http.client.RequestTimeoutException;
import com.smartgwt.client.util.SC;
import platform.gwt.base.client.AsyncCallbackEx;
import platform.gwt.paas.shared.exceptions.MessageException;
import platform.gwt.utils.GwtUtils;

public abstract class ErrorHandlingCallback<T> extends AsyncCallbackEx<T> {
    public void failure(Throwable caught) {
        Log.debug("Failure, while performing an action. ", caught);

        if (caught instanceof RequestTimeoutException) {
            SC.warn("The action timed out.");
        }
        String message = GwtUtils.toHtml(caught.getMessage());
        if (caught instanceof MessageException) {
            SC.warn(message);
        } else {
            SC.warn("Failure, while performing an action: " + message);
        }
    }
}
