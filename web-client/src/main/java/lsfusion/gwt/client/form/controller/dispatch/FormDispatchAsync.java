package lsfusion.gwt.client.form.controller.dispatch;

import lsfusion.gwt.client.GForm;
import lsfusion.gwt.client.RemoteDispatchAsync;
import lsfusion.gwt.client.base.result.ListResult;
import lsfusion.gwt.client.base.view.PopupOwner;
import lsfusion.gwt.client.controller.remote.action.BaseAction;
import lsfusion.gwt.client.controller.remote.action.PriorityErrorHandlingCallback;
import lsfusion.gwt.client.controller.remote.action.RequestAction;
import lsfusion.gwt.client.controller.remote.action.RequestAsyncCallback;
import lsfusion.gwt.client.controller.remote.action.RequestCountingAsyncCallback;
import lsfusion.gwt.client.controller.remote.action.form.*;
import lsfusion.gwt.client.form.controller.GFormController;
import net.customware.gwt.dispatch.shared.Result;
import net.customware.gwt.dispatch.shared.general.StringResult;

import java.util.ArrayList;

public class FormDispatchAsync extends RemoteDispatchAsync {
    private final GForm form;
    private final GFormController formController;

    // priority modifier to make inner window request more important
    public final int dispatchPriority;

    //отдельный флаг закрытой формы нужен, чтобы не посылать случайных запросов в закрытую форму (в частности changePageSize)
    private boolean formClosed = false;

    public FormDispatchAsync(GFormController formController, int dispatchPriority) {
        this.formController = formController;
        this.form = formController.getForm();
        this.dispatchPriority = dispatchPriority;
    }

    @Override
    protected <A extends BaseAction<R>, R extends Result> void fillAction(A action) {
        ((FormAction) action).formSessionID = form.sessionID;
    }

    @Override
    protected <A extends RequestAction<R>, R extends Result> long fillQueuedAction(A action) {
        FormRequestAction formRequestAction = (FormRequestAction) action;
        if (action instanceof FormRequestCountingAction)
            formRequestAction.requestIndex = nextRequestIndex++;
        formRequestAction.lastReceivedRequestIndex = lastReceivedRequestIndex;
        return formRequestAction.requestIndex;
    }

    // a view's batch while it runs (GFormController.batch), null when none does: the counted requests the form makes
    // meanwhile, each told the index the batch is sent with - every one of them comes here (executeQueue), so none is
    // sent apart and that index is still the next one when the batch ends
    private ArrayList<FormRequestCountingAction<?>> batch;

    public boolean isBatching() {
        return batch != null;
    }

    public void startBatch() {
        batch = new ArrayList<>();
    }

    // ... and when it ends, the requests it holds sent as one (ExecuteBatch), if any. fn calls members only, whose
    // requests a batch carries; another goes with them all the same, failing the batch on the server
    // (BatchRequest.Unsupported): sent apart, it would take the batch's index
    public void sendBatch(RequestCountingAsyncCallback<ServerResponseResult> callback) {
        ArrayList<FormRequestCountingAction<?>> requests = batch;
        batch = null;
        if (!requests.isEmpty())
            asyncExecute(new ExecuteBatch(requests), callback);
    }

    @Override
    public <A extends RequestAction<R>, R extends Result> long executeQueue(A action, RequestAsyncCallback<R> callback, boolean sync, boolean continueInvocation) {
        if (batch != null && action instanceof FormRequestCountingAction) { // a counted request goes in the batch
            batch.add((FormRequestCountingAction<?>) action);
            return nextRequestIndex;
        }
        return super.executeQueue(action, callback, sync, continueInvocation);
    }

    @Override
    protected void showAsync(boolean set) {
        formController.showAsync(set);
    }

    @Override
    protected boolean isEditing() {
        return formController.isEditing();
    }

    @Override
    protected long getEditingRequestIndex() {
        return formController.getEditingRequestIndex();
    }

    @Override
    protected boolean isClosed() {
        return formClosed;
    }
    public boolean isFormClosed() { // public accessor so the form controller can reject pending callbacks when closed
        return formClosed;
    }

    @Override
    protected int getDispatchPriority() {
        return dispatchPriority;
    }

    public void close() {
        formClosed = true;
    }

    @Override
    public PopupOwner getPopupOwner() {
        return formController.getPopupOwner();
    }

    @Override
    public void getServerActionMessage(PriorityErrorHandlingCallback<StringResult> callback) {
        executePriority(new GetRemoteActionMessage(), callback);
    }

    @Override
    public void getServerActionMessageList(PriorityErrorHandlingCallback<ListResult> callback) {
        executePriority(new GetRemoteActionMessageList(), callback);
    }

    @Override
    public void interrupt(boolean cancelable, PopupOwner popupOwner) {
        executePriority(new Interrupt(cancelable), new PriorityErrorHandlingCallback<>(popupOwner));
    }

}
