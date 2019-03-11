package lsfusion.server.context;

import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.interop.ModalityType;
import lsfusion.interop.action.ClientAction;
import lsfusion.server.auth.SecurityPolicy;
import lsfusion.server.classes.CustomClass;
import lsfusion.server.classes.DataClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.form.entity.ManageSessionType;
import lsfusion.server.form.entity.ObjectEntity;
import lsfusion.server.form.entity.filter.ContextFilter;
import lsfusion.server.form.instance.FormInstance;
import lsfusion.server.form.instance.listener.CustomClassListener;
import lsfusion.server.form.instance.listener.FocusListener;
import lsfusion.server.form.navigator.LogInfo;
import lsfusion.server.logics.LogicsInstance;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.property.DialogRequest;
import lsfusion.server.logics.property.PullChangeProperty;
import lsfusion.server.remote.RemoteForm;
import lsfusion.server.session.DataSession;

import java.sql.SQLException;
import java.util.Locale;

public interface Context {

    LogicsInstance getLogicsInstance();

    FormInstance getFormInstance();

    FormInstance createFormInstance(FormEntity formEntity, ImMap<ObjectEntity, ? extends ObjectValue> mapObjects, DataSession session, boolean isModal, Boolean noCancel, ManageSessionType manageSession, ExecutionStack stack, boolean checkOnOk, boolean showDrop, boolean interactive, ImSet<ContextFilter> contextFilters, ImSet<PullChangeProperty> pullProps, boolean readonly) throws SQLException, SQLHandledException;
    RemoteForm createRemoteForm(FormInstance formInstance, ExecutionStack stack);

    void requestFormUserInteraction(FormInstance formInstance, ModalityType modalityType, boolean forbidDuplicate, ExecutionStack stack) throws SQLException, SQLHandledException;
    
    ObjectValue requestUserObject(DialogRequest dialogRequest, ExecutionStack stack) throws SQLException, SQLHandledException;
    ObjectValue requestUserData(DataClass dataClass, Object oldValue);
    ObjectValue requestUserClass(CustomClass baseClass, CustomClass defaultValue, boolean concrete);

    void pushLogMessage();
    ImList<AbstractContext.LogMessage> popLogMessage();
    LogInfo getLogInfo();
    void delayUserInteraction(ClientAction action);
    Object requestUserInteraction(ClientAction action);
    boolean canBeProcessed();
    Object[] requestUserInteraction(ClientAction... actions);

    // для создания форм
    FocusListener getFocusListener();
    CustomClassListener getClassListener();
    Long getCurrentComputer();
    Long getCurrentUser();
    Long getCurrentUserRole();

    String localize(LocalizedString s);
    String localize(LocalizedString s, Locale locale);
    Locale getLocale();
}
