package platform.gwt.base.client;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.core.client.GWT;
import platform.gwt.base.client.ui.ErrorFrameWidget;

public class ErrorAsyncCallback<T> extends AsyncCallbackEx<T> {
    @Override
    public void failure(Throwable caught) {
        if (Log.isDebugEnabled()) {
            Log.debug("Failure, while performing an action. ", caught);
        } else {
            GWT.log("Failure, while performing an action. ", caught);
        }
        showErrorPage(caught);
    }

    private static void showErrorPage(Throwable caught) {
        GwtClientUtils.setAsRootPane(new ErrorFrameWidget(caught));
    }
}
