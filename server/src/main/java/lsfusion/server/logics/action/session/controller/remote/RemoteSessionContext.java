package lsfusion.server.logics.action.session.controller.remote;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.interop.action.ClientAction;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.action.controller.stack.ExecutionStack;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.form.interactive.action.FormOptions;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ConnectionContext;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.interactive.listener.CustomClassListener;
import lsfusion.server.logics.form.interactive.listener.FocusListener;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.physics.admin.authentication.controller.remote.RemoteConnection;
import lsfusion.server.physics.admin.authentication.controller.remote.RemoteConnectionContext;

import java.sql.SQLException;

import static lsfusion.server.physics.admin.log.ServerLoggers.systemLogger;

public class RemoteSessionContext extends RemoteConnectionContext {

    private final RemoteSession session;

    private final ConnectionContext remoteContext;

    @Override
    protected RemoteConnection getConnectionObject() {
        return session;
    }

    @Override
    public ConnectionContext getConnectionContext() {
        return remoteContext;
    }

    public RemoteSessionContext(RemoteSession session) {
        this.session = session;

        remoteContext = new ConnectionContext(true, false, false, false);
    }

    @Override
    public FocusListener getFocusListener() {
        return null;
    }

    @Override
    public CustomClassListener getClassListener() {
        return null;
    }

    // an action called through the program interface has no client to show a form to, so opening one fails (see
    // AbstractContext) - unless openFormsWithoutClient is set, as the platform's tests do: then SHOW ... NOWAIT creates the
    // form, which runs its ON INIT, and requestFormUserInteraction closes it right away, the way a form whose client has
    // gone is closed, with no ON CLOSE
    @Override
    public FormInstance createFormInstance(FormEntity formEntity, ImMap<ObjectEntity, ? extends ObjectValue> mapObjects, DataSession session, ExecutionStack stack, boolean interactive, FormOptions options) throws SQLException, SQLHandledException {
        if(!Settings.get().isOpenFormsWithoutClient())
            return super.createFormInstance(formEntity, mapObjects, session, stack, interactive, options);
        if(options.syncType) // checked before the form is created, so that its ON INIT does not run for an open that fails
            throw new UnsupportedOperationException("A form opened with no client can only be shown NOWAIT: nobody would close a form the action waits for");

        return new FormInstance(formEntity, getLogicsInstance(), session, this.session.securityPolicy, getFocusListener(), getClassListener(),
                mapObjects, stack, interactive, false, getLocale(), options);
    }

    @Override
    public void requestFormUserInteraction(FormInstance formInstance, FormOptions formOptions, ExecutionStack stack) throws SQLException, SQLHandledException {
        if(!Settings.get().isOpenFormsWithoutClient())
            super.requestFormUserInteraction(formInstance, formOptions, stack);
        else
            formInstance.close();
    }
}
