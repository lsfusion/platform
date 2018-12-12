package lsfusion.gwt.client;

import com.google.gwt.http.client.RequestTimeoutException;
import com.google.gwt.user.client.rpc.IncompatibleRemoteServiceException;
import com.google.gwt.user.client.rpc.StatusCodeException;
import lsfusion.gwt.client.base.AsyncCallbackEx;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.ui.DialogBoxHelper;
import lsfusion.gwt.client.base.ui.DialogBoxHelper.OptionType;
import lsfusion.gwt.shared.exceptions.AppServerNotAvailableException;
import lsfusion.gwt.shared.exceptions.MessageException;
import lsfusion.gwt.shared.exceptions.RemoteRetryException;

import static lsfusion.gwt.client.base.GwtClientUtils.baseMessages;

public class ErrorHandlingCallback<T> extends AsyncCallbackEx<T> {
    private static final String TIMEOUT_MESSAGE = "SESSION_TIMED_OUT";
    private static final Integer MAX_REQUEST_TRIES = 30;

    @Override
    public void failure(Throwable caught) {
        GwtClientUtils.removeLoaderFromHostedPage();

        showErrorMessage(caught);
    }

    protected void showErrorMessage(final Throwable caught) {
        GExceptionManager.logClientError("Failure, while performing an action. ", caught);

        if(getMaxTries(caught) > -1) // if there is a trouble in connection, then we just setting connectionLost in DispatchAsyncWrapper, and showing no message (because there will be connection lost dialog anyway)
            return;

        String message = getServerMessage(caught);
        if (message != null) {
            ErrorDialog.show(baseMessages.error(), message, getJavaStackTrace(caught), getLSFStackTrace(caught));
            return;
        } else if (caught instanceof RequestTimeoutException) {
            DialogBoxHelper.showMessageBox(true, baseMessages.error(), baseMessages.actionTimeoutErrorMessage(), false, null);
            return;
        } else if (caught instanceof StatusCodeException) {
            StatusCodeException statusEx = (StatusCodeException) caught;
            if (statusEx.getStatusCode() == 500 && statusEx.getEncodedResponse().contains(TIMEOUT_MESSAGE)) {
                DialogBoxHelper.showMessageBox(true, baseMessages.error(), baseMessages.sessionTimeoutErrorMessage(), false, new DialogBoxHelper.CloseCallback() {
                    @Override
                    public void closed(OptionType chosenOption) {
                        relogin();
                    }
                });
                return;
            }
        }
        ErrorDialog.show(baseMessages.error(), baseMessages.internalServerErrorMessage(), getJavaStackTrace(caught));
    }

    public static int getMaxTries(Throwable caught) {
        if (caught instanceof StatusCodeException)
            return MAX_REQUEST_TRIES;
        else if (caught instanceof RemoteRetryException)
            return ((RemoteRetryException) caught).maxTries;
        if (caught instanceof IncompatibleRemoteServiceException)
            return 2;
        else return -1;
    }

    protected String getServerMessage(Throwable caught) {
        if (caught instanceof MessageException || caught instanceof AppServerNotAvailableException) {
            return caught.getMessage();
        }
        return null;
    }

    private String getJavaStackTrace(Throwable caught) {
        StringBuilder result = new StringBuilder(caught.getMessage());
        for (StackTraceElement element : getStackTrace(caught)) {
            result.append("\n\tat ").append(element);
        }
        return result.toString();
    }
    
    private StackTraceElement[] getStackTrace(Throwable caught) {
        return caught instanceof MessageException ? ((MessageException) caught).myTrace : caught.getStackTrace();
    }

    private String getLSFStackTrace(Throwable caught) {
        if (caught instanceof MessageException) {
            return ((MessageException) caught).lsfStack;
        }
        return null;
    }

    protected void relogin() {
        GwtClientUtils.relogin();
    }
}
