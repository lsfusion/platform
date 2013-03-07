package platform.server.context;

import platform.base.col.interfaces.immutable.ImMap;
import platform.interop.action.ClientAction;
import platform.server.classes.CustomClass;
import platform.server.classes.DataClass;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.instance.DialogInstance;
import platform.server.form.instance.FormInstance;
import platform.server.form.instance.FormSessionScope;
import platform.server.remote.RemoteDialog;
import platform.server.remote.RemoteForm;
import platform.server.logics.*;
import platform.server.logics.property.ExecutionContext;
import platform.server.session.DataSession;

import java.sql.SQLException;

public interface Context {

    LogicsInstance getLogicsInstance();

    FormInstance getFormInstance();

    FormInstance createFormInstance(FormEntity formEntity, ImMap<ObjectEntity, DataObject> mapObjects, DataSession session, boolean isModal, FormSessionScope sessionScope, boolean checkOnOk, boolean showDrop, boolean interactive)  throws SQLException;
    RemoteForm createRemoteForm(FormInstance formInstance);
    RemoteDialog createRemoteDialog(DialogInstance dialogInstance);

    ObjectValue requestUserObject(ExecutionContext.RequestDialog requestDialog) throws SQLException;
    ObjectValue requestUserData(DataClass dataClass, Object oldValue);
    ObjectValue requestUserClass(CustomClass baseClass, CustomClass defaultValue, boolean concrete);

    String getLogMessage();
    void delayRemoteChanges();
    void delayUserInteraction(ClientAction action);
    Object requestUserInteraction(ClientAction action);
    Object[] requestUserInteraction(ClientAction... actions);

    void setActionMessage(String message);
    String getActionMessage();
    void pushActionMessage(String segment);
    String popActionMessage();
}
