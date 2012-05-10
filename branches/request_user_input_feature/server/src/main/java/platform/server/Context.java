package platform.server;

import platform.interop.action.ClientAction;
import platform.interop.form.UserInputResult;
import platform.server.data.type.Type;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.instance.DialogInstance;
import platform.server.form.instance.FormInstance;
import platform.server.form.instance.remote.RemoteDialog;
import platform.server.form.instance.remote.RemoteForm;
import platform.server.logics.BusinessLogics;
import platform.server.logics.DataObject;
import platform.server.session.DataSession;

import java.sql.SQLException;
import java.util.Map;

public interface Context {
    ThreadLocal<Context> context = new ThreadLocal<Context>();

    BusinessLogics getBL();

    void setActionMessage(String message);
    String getActionMessage();
    void pushActionMessage(String segment);
    String popActionMessage();

    FormInstance createFormInstance(FormEntity formEntity, Map<ObjectEntity, DataObject> mapObjects, DataSession session, boolean newSession, boolean interactive)  throws SQLException;
    RemoteForm createRemoteForm(FormInstance formInstance, boolean checkOnOk);
    RemoteDialog createRemoteDialog(DialogInstance dialogInstance);

    UserInputResult requestUserInput(Type type, Object oldValue);
    Object requestUserInteraction(ClientAction action);
    Object[] requestUserInteraction(ClientAction... actions);
}
