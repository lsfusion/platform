package lsfusion.server.context;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.interop.action.ClientAction;
import lsfusion.server.auth.SecurityPolicy;
import lsfusion.server.classes.CustomClass;
import lsfusion.server.classes.DataClass;
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
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.LogicsInstance;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.property.DialogRequest;
import lsfusion.server.logics.property.PullChangeProperty;
import lsfusion.server.remote.RemoteForm;
import lsfusion.server.session.DataSession;
import lsfusion.server.stack.ExecutionStackItem;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;

public interface Context {

    LogicsInstance getLogicsInstance();

    FormInstance getFormInstance();

    FormInstance createFormInstance(FormEntity formEntity, ImMap<ObjectEntity, ? extends ObjectValue> mapObjects, DataSession session, boolean isModal, boolean isAdd, FormSessionScope sessionScope, ExecutionStack stack, boolean checkOnOk, boolean showDrop, boolean interactive, ImSet<FilterEntity> contextFilters, PropertyDrawEntity initFilterProperty, ImSet<PullChangeProperty> pullProps, boolean readonly) throws SQLException, SQLHandledException;
    RemoteForm createRemoteForm(FormInstance formInstance, ExecutionStack stack);

    ObjectValue requestUserObject(DialogRequest dialogRequest, ExecutionStack stack) throws SQLException, SQLHandledException;
    ObjectValue requestUserData(DataClass dataClass, Object oldValue);
    ObjectValue requestUserClass(CustomClass baseClass, CustomClass defaultValue, boolean concrete);

    String getLogMessage();
    LogInfo getLogInfo();
    void delayUserInteraction(ClientAction action);
    Object requestUserInteraction(ClientAction action);
    boolean canBeProcessed();
    Object[] requestUserInteraction(ClientAction... actions);

    String getActionMessage();
    List<Object> getActionMessageList();
    Thread getLastThread();
    void pushActionMessage(ExecutionStackItem stackItem);
    void popActionMessage(ExecutionStackItem stackItem);

    // для создания форм
    SecurityPolicy getSecurityPolicy();
    FocusListener getFocusListener();
    CustomClassListener getClassListener();
    PropertyObjectInterfaceInstance getComputer(ExecutionStack stack);
    Integer getCurrentUser();
    DataObject getConnection();
    
    String localize(LocalizedString s);
    String localize(LocalizedString s, Locale locale);
    Locale getLocale();
}
