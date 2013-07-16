package lsfusion.server.context;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.interop.action.ClientAction;
import lsfusion.server.classes.CustomClass;
import lsfusion.server.classes.DataClass;
import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.form.entity.ObjectEntity;
import lsfusion.server.form.entity.filter.FilterEntity;
import lsfusion.server.form.instance.DialogInstance;
import lsfusion.server.form.instance.FormInstance;
import lsfusion.server.form.instance.FormSessionScope;
import lsfusion.server.logics.LogicsInstance;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.PullChangeProperty;
import lsfusion.server.remote.RemoteDialog;
import lsfusion.server.remote.RemoteForm;
import lsfusion.server.session.DataSession;

import java.sql.SQLException;
import java.util.List;

public interface Context {

    LogicsInstance getLogicsInstance();

    FormInstance getFormInstance();

    FormInstance createFormInstance(FormEntity formEntity, ImMap<ObjectEntity, ? extends ObjectValue> mapObjects, DataSession session, boolean isModal, FormSessionScope sessionScope, boolean checkOnOk, boolean showDrop, boolean interactive, ImSet<FilterEntity> contextFilters, ImSet<PullChangeProperty> pullProps)  throws SQLException;
    RemoteForm createRemoteForm(FormInstance formInstance);
    RemoteDialog createRemoteDialog(DialogInstance dialogInstance);

    ObjectValue requestUserObject(ExecutionContext.RequestDialog requestDialog) throws SQLException;
    ObjectValue requestUserData(DataClass dataClass, Object oldValue);
    ObjectValue requestUserClass(CustomClass baseClass, CustomClass defaultValue, boolean concrete);

    String getLogMessage();
    void delayUserInteraction(ClientAction action);
    Object requestUserInteraction(ClientAction action);
    Object[] requestUserInteraction(ClientAction... actions);

    void setActionMessage(String message);
    String getActionMessage();
    void pushActionMessage(String segment);
    String popActionMessage();
}
