package lsfusion.gwt.client.controller.remote.action.form;

import net.customware.gwt.dispatch.shared.Result;

public class FormRequestAction<R extends Result> extends FormAction<R> {
    
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
