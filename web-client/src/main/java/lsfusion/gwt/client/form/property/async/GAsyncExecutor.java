package lsfusion.gwt.client.form.property.async;

import lsfusion.gwt.client.controller.dispatch.GwtActionDispatcher;
import lsfusion.gwt.client.navigator.controller.GAsyncFormController;

import java.util.function.Function;

public class GAsyncExecutor {

    public GwtActionDispatcher dispatcher;
    public Function<GPushAsyncResult, Long> asyncExec;
    public final boolean sync;

    public GAsyncExecutor(GwtActionDispatcher dispatcher, Function<GPushAsyncResult, Long> asyncExec) {
        this(dispatcher, asyncExec, false);
    }

    public GAsyncExecutor(GwtActionDispatcher dispatcher, Function<GPushAsyncResult, Long> asyncExec, boolean sync) {
        this.dispatcher = dispatcher;
        this.asyncExec = asyncExec;
        this.sync = sync;
    }

    public GAsyncFormController execute() {
        return execute(null);
    }

    public GAsyncFormController execute(GPushAsyncResult pushAsyncResult) {
        return dispatcher.getAsyncFormController(asyncExec.apply(pushAsyncResult));
    }
}
