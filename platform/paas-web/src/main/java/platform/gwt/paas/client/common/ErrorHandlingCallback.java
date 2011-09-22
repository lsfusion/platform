package platform.gwt.paas.client.common;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.util.SC;

public abstract class ErrorHandlingCallback<T> implements AsyncCallback<T> {
    @Override
    public void onFailure(Throwable caught) {
        SC.warn("Failure, while performing an action: " + caught.getMessage());
        Log.debug("Failure, while performing an action. ");
    }
}
