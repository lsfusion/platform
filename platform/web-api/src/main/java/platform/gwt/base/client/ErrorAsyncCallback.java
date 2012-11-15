package platform.gwt.base.client;

import com.allen_sauer.gwt.log.client.Log;
import platform.gwt.base.client.ui.ErrorFrameWidget;

public class ErrorAsyncCallback<T> extends AsyncCallbackEx<T> {
    @Override
    public void failure(Throwable caught) {
        Log.debug("Failure, while performing an action. ", caught);
        showErrorPage(caught);
    }

    private static void showErrorPage(Throwable caught) {
        GwtClientUtils.setAsRootPane(new ErrorFrameWidget(caught));
    }
}
