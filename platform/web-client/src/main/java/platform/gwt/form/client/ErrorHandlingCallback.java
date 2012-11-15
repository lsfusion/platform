package platform.gwt.form.client;

import platform.gwt.base.client.ErrorAsyncCallback;
import platform.gwt.base.shared.MessageException;
import platform.gwt.form.client.form.ui.dialog.DialogBoxHelper;

public class ErrorHandlingCallback<T> extends ErrorAsyncCallback<T> {
    @Override
    public void failure(Throwable caught) {
        if (caught instanceof MessageException) {
            DialogBoxHelper.showMessageBox(true, "Error: ", caught.getMessage(), null);
        } else {
            super.failure(caught);
        }
    }
}
