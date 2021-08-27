package lsfusion.gwt.client.controller.remote.action;

import com.google.gwt.http.client.RequestTimeoutException;
import com.google.gwt.user.client.rpc.IncompatibleRemoteServiceException;
import com.google.gwt.user.client.rpc.StatusCodeException;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.exception.*;
import lsfusion.gwt.client.base.view.DialogBoxHelper;

public class PriorityErrorHandlingCallback<T> implements PriorityAsyncCallback<T> {

    public PriorityErrorHandlingCallback() {
    }

    private static final String TIMEOUT_MESSAGE = "SESSION_TIMED_OUT";
    private static final Integer MAX_REQUEST_TRIES = 30;
    private static final ClientMessages messages = ClientMessages.Instance.get();

    public static void showErrorMessage(final Throwable caught) {
        GwtClientUtils.removeLoaderFromHostedPage();

        GExceptionManager.logClientError(caught);

        if(getMaxTries(caught) > -1) // if there is a trouble in connection, then we just setting connectionLost in DispatchAsyncWrapper, and showing no message (because there will be connection lost dialog anyway)
            return;

        if (caught instanceof RequestTimeoutException) {
            DialogBoxHelper.showMessageBox(true, messages.error(), messages.actionTimeoutError(), false, null);
            return;
        } else if (caught instanceof StatusCodeException) {
            StatusCodeException statusEx = (StatusCodeException) caught;
            if (statusEx.getStatusCode() == 500 && statusEx.getEncodedResponse().contains(TIMEOUT_MESSAGE)) {
                DialogBoxHelper.showMessageBox(true, messages.error(), messages.sessionTimeoutError(), false, new DialogBoxHelper.CloseCallback() {
                    @Override
                    public void closed(DialogBoxHelper.OptionType chosenOption) {
                        GwtClientUtils.logout();
                    }
                });
                return;
            }
        } else if (caught instanceof RemoteMessageDispatchException) {
            DialogBoxHelper.showMessageBox(true, messages.error(), caught.getMessage(), false, null);
            return;
        }
        // messages.internalServerError();
        String[] actualStacks = RemoteInternalDispatchException.toString(caught);
        ErrorDialog.show(messages.error(), actualStacks[0], actualStacks[1], actualStacks[2]);
    }

    public static boolean isAuthException(Throwable caught) {
        return caught instanceof StatusCodeException && ((StatusCodeException) caught).getStatusCode() == 401;
    }

    // client - web-server and web-server - app-server connection problems
    public static int getMaxTries(Throwable caught) {
        if(isAuthException(caught))
            return 0;
        if (caught instanceof StatusCodeException) // client - web-server
            return MAX_REQUEST_TRIES;
        else if (caught instanceof RemoteRetryException) // web-server - app-server
            return ((RemoteRetryException) caught).maxTries;
        if (caught instanceof IncompatibleRemoteServiceException) // client - web-server
            return 2;
        return -1; // not connection problem
    }

    @Override
    public void onSuccess(T result) {
    }

    @Override
    public void onFailure(Throwable caught) {
        showErrorMessage(caught);
    }
}
