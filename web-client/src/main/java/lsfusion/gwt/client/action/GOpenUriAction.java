package lsfusion.gwt.client.action;

import java.io.Serializable;

public class GOpenUriAction extends GExecuteAction {
    public Serializable uri;
    public boolean noEncode;

    //needed for it to be gwt-serializable
    @SuppressWarnings("UnusedDeclaration")
    public GOpenUriAction() {}

    public GOpenUriAction(Serializable uri, boolean noEncode) {
        this.uri = uri;
        this.noEncode = noEncode;
    }

    @Override
    public void execute(GActionDispatcher dispatcher) throws Throwable {
        dispatcher.execute(this);
    }
}
