package lsfusion.gwt.server.form;

import lsfusion.client.controller.remote.proxy.RemoteObjectProxy;
import lsfusion.gwt.client.action.GAction;
import lsfusion.gwt.client.action.GThrowExceptionAction;
import lsfusion.gwt.client.controller.remote.action.form.FormAction;
import lsfusion.gwt.client.controller.remote.action.form.ServerResponseResult;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.convert.ClientActionToGwtConverter;
import lsfusion.http.provider.form.FormSessionObject;
import lsfusion.interop.action.ServerResponse;
import lsfusion.interop.base.remote.PendingRemoteInterface;
import lsfusion.interop.form.remote.RemoteFormInterface;

import java.rmi.RemoteException;

public abstract class FormServerResponseActionHandler<A extends FormAction<ServerResponseResult>> extends FormActionHandler<A, ServerResponseResult> {
    private static ClientActionToGwtConverter clientActionConverter = ClientActionToGwtConverter.getInstance();

    protected FormServerResponseActionHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    protected interface RemoteCall {
        ServerResponse call(RemoteFormInterface remoteForm) throws RemoteException;
    }
    protected ServerResponseResult getServerResponseResult(FormAction action, RemoteCall call) throws RemoteException {
        String formID = action.formSessionID;
        FormSessionObject form = getFormSessionObject(formID);
        return getServerResponseResult(formID, form.remoteForm, form, call.call(form.remoteForm), servlet);
    }

    public static ServerResponseResult getServerResponseResult(String formID, PendingRemoteInterface remoteInterface, FormSessionObject form, ServerResponse serverResponse, MainDispatchServlet servlet) {
        String realHostName = ((RemoteObjectProxy)remoteInterface).realHostName;
        GAction[] resultActions = new GAction[serverResponse.actions.length];
        for (int i = 0; i < serverResponse.actions.length; i++) {
            try {
                resultActions[i] = clientActionConverter.convertAction(serverResponse.actions[i], form, realHostName, formID, servlet);
            } catch (Exception e) {
                resultActions[i] = new GThrowExceptionAction(MainDispatchServlet.fromWebServerToWebClient(e));
            }
        }

        return new ServerResponseResult(resultActions, serverResponse.requestIndex, serverResponse.resumeInvocation);
    }

}
