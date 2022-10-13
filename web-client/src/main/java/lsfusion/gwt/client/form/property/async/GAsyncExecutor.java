package lsfusion.gwt.client.form.property.async;

import lsfusion.gwt.client.controller.dispatch.GwtActionDispatcher;
import lsfusion.gwt.client.navigator.controller.GAsyncFormController;

import java.util.function.Function;

public class GAsyncExecutor {

    public GwtActionDispatcher dispatcher;
    public Function<GPushAsyncResult, Long> asyncExec;

    public GAsyncExecutor(GwtActionDispatcher dispatcher, Function<GPushAsyncResult, Long> asyncExec) {
        this.dispatcher = dispatcher;
        this.asyncExec = asyncExec;
    }

    public GAsyncFormController execute() {
        return execute(null);
    }

    public GAsyncFormController execute(GPushAsyncResult pushAsyncResult) {
        return dispatcher.getAsyncFormController(asyncExec.apply(pushAsyncResult));
    }
}