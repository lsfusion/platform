package platform.gwt.paas.client.common;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.http.client.RequestTimeoutException;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.util.SC;
import platform.gwt.paas.shared.exceptions.MessageException;

public abstract class ErrorHandlingCallback<T> implements AsyncCallback<T> {
    @Override
    public final void onSuccess(T result) {
        preProcess();
        success(result);
        postProcess();
    }

    @Override
    public final void onFailure(Throwable caught) {
        preProcess();
        failure(caught);
        postProcess();
    }

    public void preProcess() {

    }

    public void success(T result) {
    }

    public void failure(Throwable caught) {
        Log.debug("Failure, while performing an action. ", caught);

        if (caught instanceof RequestTimeoutException) {
            SC.warn("The action timed out.");
        }
        if (caught instanceof MessageException) {
            SC.warn(caught.getMessage());
        } else {
            SC.warn("Failure, while performing an action: " + caught.getMessage());
        }
    }

    public void postProcess() {
    }
}
