package lsfusion.gwt.base.client;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.RequestTimeoutException;
import com.google.gwt.user.client.rpc.StatusCodeException;
import lsfusion.gwt.base.client.ui.DialogBoxHelper;
import lsfusion.gwt.base.shared.InvalidateException;
import lsfusion.gwt.base.shared.MessageException;
import lsfusion.gwt.base.shared.actions.NavigatorAction;

import static lsfusion.gwt.base.client.GwtClientUtils.baseMessages;

public class ErrorHandlingCallback<T> extends AsyncCallbackEx<T> {
    private static final String TIMEOUT_MESSAGE = "SESSION_TIMED_OUT";

    @Override
    public void failure(Throwable caught) {
        GwtClientUtils.removeLoaderFromHostedPage();

        showErrorMessage(caught);
    }

    protected void showErrorMessage(final Throwable caught) {
        if (Log.isDebugEnabled()) {
            Log.debug("Failure, while performing an action. ", caught);
        } else {
            GWT.log("Failure, while performing an action. ", caught);
        }

        String message = getServerMessage(caught);
        if (message != null) {
            DialogBoxHelper.showMessageBox(true, "Error: ", message, null);
            return;
        } else if (caught instanceof RequestTimeoutException) {
            DialogBoxHelper.showMessageBox(true, "Error: ", baseMessages.actionTimeoutErrorMessage(), false, null);
            return;
        } else if (caught instanceof InvalidateException) {
            DialogBoxHelper.CloseCallback closeCallback = new DialogBoxHelper.CloseCallback() {
                @Override
                public void closed(DialogBoxHelper.OptionType chosenOption) {
                    switch (chosenOption) {
                        case LOGOUT: GwtClientUtils.logout();
                    }
                }
            };
            if (((InvalidateException) caught).action instanceof NavigatorAction) {
                DialogBoxHelper.showLogoutMessageBox("Error: ", caught.getMessage(), closeCallback);
            } else {
                DialogBoxHelper.showMessageBox(true, "Error: ",  caught.getMessage(), closeCallback);
            }
            return;
        } else if (caught instanceof StatusCodeException) {
            StatusCodeException statusEx = (StatusCodeException) caught;
            if (statusEx.getStatusCode() == 500 && statusEx.getEncodedResponse().contains(TIMEOUT_MESSAGE)) {
                DialogBoxHelper.showMessageBox(true, "Error: ", baseMessages.sessionTimeoutErrorMessage(), false, new DialogBoxHelper.CloseCallback() {
                    @Override
                    public void closed(DialogBoxHelper.OptionType chosenOption) {
                        relogin();
                    }
                });
                return;
            }
        }
        DialogBoxHelper.showMessageBox(true, "Error: ", baseMessages.internalServerErrorMessage(), false, null);
    }

    protected String getServerMessage(Throwable caught) {
        if (caught instanceof MessageException) {
            return caught.getMessage();
        }
        return null;
    }

    protected void relogin() {
        GwtClientUtils.relogin();
    }
}
