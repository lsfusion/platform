package platform.gwt.sgwtbase.client;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.http.client.RequestTimeoutException;
import com.google.gwt.user.client.rpc.StatusCodeException;
import com.smartgwt.client.util.BooleanCallback;
import com.smartgwt.client.util.SC;
import platform.gwt.base.client.AsyncCallbackEx;
import platform.gwt.base.client.GwtClientUtils;
import platform.gwt.base.shared.MessageException;

import static platform.gwt.base.client.GwtClientUtils.baseMessages;

public class ErrorAsyncCallback<T> extends AsyncCallbackEx<T> {
    private static final String TIMEOUT_MESSAGE = "SESSION_TIMED_OUT";

    @Override
    public void failure(Throwable caught) {
        GwtClientUtils.removeLoaderFromHostedPage();

        Log.debug("Failure, while performing an action. ", caught);

        String message = getServerMessage(caught);
        if (message != null) {
            SC.warn(GwtClientUtils.toHtml(message));
            return;
        } else if (caught instanceof RequestTimeoutException) {
            SC.warn(baseMessages.actionTimeoutErrorMessage());
            return;
        } else if (caught instanceof StatusCodeException) {
            StatusCodeException statusEx = (StatusCodeException) caught;
            if (statusEx.getStatusCode() == 500 && statusEx.getEncodedResponse().contains(TIMEOUT_MESSAGE)) {
                SC.warn(baseMessages.sessionTimeoutErrorMessage(), new BooleanCallback() {
                    @Override
                    public void execute(Boolean value) {
                        relogin();
                    }
                });
                return;
            }
        }
        SC.warn(baseMessages.internalServerErrorMessage());
    }

    protected void relogin() {
        GwtClientUtils.relogin();
    }

    protected String getServerMessage(Throwable caught) {
        if (caught instanceof MessageException) {
            return caught.getMessage();
        }
        return null;
    }
}
