package lsfusion.gwt.sgwtbase.client;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.RequestTimeoutException;
import com.google.gwt.user.client.rpc.StatusCodeException;
import com.smartgwt.client.util.BooleanCallback;
import com.smartgwt.client.util.SC;
import lsfusion.gwt.base.client.AsyncCallbackEx;
import lsfusion.gwt.base.client.EscapeUtils;
import lsfusion.gwt.base.client.GwtClientUtils;
import lsfusion.gwt.base.client.ui.DialogBoxHelper;
import lsfusion.gwt.base.shared.InvalidateException;
import lsfusion.gwt.base.shared.MessageException;
import lsfusion.gwt.base.shared.actions.NavigatorAction;

import static lsfusion.gwt.base.client.GwtClientUtils.TIMEOUT_MESSAGE;
import static lsfusion.gwt.base.client.GwtClientUtils.baseMessages;

public class SGWTErrorHandlingCallback<T> extends AsyncCallbackEx<T> {

    @Override
    public void failure(Throwable caught) {
        GwtClientUtils.removeLoaderFromHostedPage();

        Log.debug("Failure, while performing an action. ", caught);

        String message = getServerMessage(caught);
        if (message != null) {
            SC.warn(EscapeUtils.toHtml(message));
            return;
        } else if (caught instanceof RequestTimeoutException) {
            SC.warn(baseMessages.actionTimeoutErrorMessage());
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
                SC.warn(caught.getMessage());
            }
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
        logClientError("Internal Server Error. ", caught);
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

    public static void logClientError(String message, Throwable t) {
        GWT.log(message, t);
        Log.error(message, t);
    }
}
