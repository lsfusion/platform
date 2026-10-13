package lsfusion.gwt.client.controller.remote.action.form;

import java.util.ArrayList;

// several of the form's requests sent as one: the server makes them in their order, in one request, with one
// answer - a request it makes in a batch is one whose web handler is a BatchRequestHandler, any other failing the batch
public class ExecuteBatch extends FormRequestCountingAction<ServerResponseResult> {
    public ArrayList<FormRequestCountingAction<?>> requests;

    public ExecuteBatch() {
    }

    public ExecuteBatch(ArrayList<FormRequestCountingAction<?>> requests) {
        this.requests = requests;
    }
}
