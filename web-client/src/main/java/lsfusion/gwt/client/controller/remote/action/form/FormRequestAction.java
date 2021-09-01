package lsfusion.gwt.client.controller.remote.action.form;

import lsfusion.gwt.client.controller.remote.action.RequestAction;
import net.customware.gwt.dispatch.shared.Result;

public class FormRequestAction<R extends Result> extends FormAction<R> implements RequestAction<R> {
    
    public long requestIndex;
    public long lastReceivedRequestIndex;

    public FormRequestAction() {
    }

    public FormRequestAction(long requestIndex) {
        this.requestIndex = requestIndex;
    }

    @Override
    public String toString() {
        return super.toString() + " [request#: " + requestIndex + "]";
    }

}
