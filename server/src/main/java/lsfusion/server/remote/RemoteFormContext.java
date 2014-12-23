package lsfusion.server.remote;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.interop.action.ClientAction;
import lsfusion.server.auth.SecurityPolicy;
import lsfusion.server.context.AbstractContext;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.form.entity.ObjectEntity;
import lsfusion.server.form.entity.PropertyDrawEntity;
import lsfusion.server.form.entity.filter.FilterEntity;
import lsfusion.server.form.instance.FormInstance;
import lsfusion.server.form.instance.FormSessionScope;
import lsfusion.server.form.instance.PropertyObjectInterfaceInstance;
import lsfusion.server.form.instance.listener.CustomClassListener;
import lsfusion.server.form.instance.listener.FocusListener;
import lsfusion.server.form.navigator.LogInfo;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.LogicsInstance;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.property.PullChangeProperty;
import lsfusion.server.session.CompoundUpdateCurrentClasses;
import lsfusion.server.session.DataSession;
import lsfusion.server.session.UpdateCurrentClasses;

import java.sql.SQLException;

public class RemoteFormContext<T extends BusinessLogics<T>, F extends FormInstance<T>> extends AbstractContext {
    private final RemoteForm<T, F> form;

    public RemoteFormContext(RemoteForm<T, F> form) {
        this.form = form;
    }

    @Override
    public LogicsInstance getLogicsInstance() {
        return form.form.logicsInstance;
    }

    public FormInstance getFormInstance() {
        return form.form;
    }

    public String getLogMessage() {
        return form.getLogMessage();
    }

    @Override
    public LogInfo getLogInfo() {
        return form.getLogInfo();
    }

    public void delayUserInteraction(ClientAction action) {
        form.delayUserInteraction(action);
    }

    public Object[] requestUserInteraction(ClientAction... actions) {
        return form.requestUserInteraction(actions);
    }

    @Override
    public RemoteForm createRemoteForm(FormInstance formInstance) {
        try {
            return new RemoteForm<T, FormInstance<T>>(formInstance, form.getExportPort(), form.getRemoteFormListener());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public SecurityPolicy getSecurityPolicy() {
        return form.form.securityPolicy;
    }

    public FocusListener getFocusListener() {
        return form.form.getFocusListener();
    }

    public CustomClassListener getClassListener() {
        return form.form.getClassListener();
    }

    public PropertyObjectInterfaceInstance getComputer() {
        return form.form.instanceFactory.computer;
    }

    public DataObject getConnection() {
        return form.form.instanceFactory.connection;
    }

    @Override
    public UpdateCurrentClasses getUpdateCurrentClasses(UpdateCurrentClasses outerUpdateCurrentClasses) {
        return CompoundUpdateCurrentClasses.merge(outerUpdateCurrentClasses, form.form.outerUpdateCurrentClasses);
    }
}
