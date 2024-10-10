package lsfusion.server.base.controller.context;

import lsfusion.interop.action.ClientAction;
import lsfusion.interop.connection.LocalePreferences;
import lsfusion.server.logics.LogicsInstance;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ConnectionContext;
import lsfusion.server.logics.form.interactive.listener.CustomClassListener;
import lsfusion.server.logics.form.interactive.listener.FocusListener;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.physics.admin.log.LogInfo;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.util.Locale;

// new thread async context (everything should be synchronized / immutable)
public class AsyncContext extends AbstractContext {

    private final Context upContext;

    public AsyncContext(Context upContext) {
        this.upContext = upContext;
    }

    @Override
    public LogicsInstance getLogicsInstance() {
        return upContext.getLogicsInstance();
    }

    @Override
    public FormEntity getCurrentForm() {
        return upContext.getCurrentForm();
    }

//    @Override
//    public FormInstance createFormInstance(FormEntity formEntity, ImSet<ObjectEntity> inputObjects, ImMap<ObjectEntity, ? extends ObjectValue> mapObjects, DataSession session, boolean isModal, Boolean noCancel, ManageSessionType manageSession, ExecutionStack stack, boolean checkOnOk, boolean showDrop, boolean interactive, WindowFormType type, ImSet<ContextFilterInstance> contextFilters, boolean readonly) throws SQLException, SQLHandledException {
//        return upContext.createFormInstance(formEntity, inputObjects, mapObjects, session, isModal, noCancel, manageSession, stack, checkOnOk, showDrop, interactive, type, contextFilters, readonly);
//    }

//    @Override
//    public RemoteForm createRemoteForm(FormInstance formInstance, ExecutionStack stack) {
//        return upContext.createRemoteForm(formInstance, stack);
//    }

//    @Override
//    public void requestFormUserInteraction(FormInstance formInstance, ShowFormType showFormType, boolean forbidDuplicate, String formId, ExecutionStack stack) throws SQLException, SQLHandledException {
//        upContext.requestFormUserInteraction(formInstance, showFormType, forbidDuplicate, formId, stack);
//    }

//    @Override
//    public InputContext lockInputContext() {
//        return upContext.lockInputContext();
//    }

//    @Override
//    public void unlockInputContext() {
//        upContext.unlockInputContext();
//    }

//    @Override
//    public InputResult inputUserData(ActionOrProperty securityProperty, DataClass dataClass, Object oldValue, boolean hasOldValue, InputContext inputContext, String customChangeFunction, InputList inputList, InputListAction[] actions) {
//        return upContext.inputUserData(securityProperty, dataClass, oldValue, hasOldValue, inputContext, customChangeFunction, inputList, actions);
//    }

//    @Override
//    public void pushLogMessage() {
//        upContext.pushLogMessage();
//    }

//    @Override
//    public ImList<AbstractContext.LogMessage> popLogMessage() {
//        return upContext.popLogMessage();
//    }

//    @Override
//    public AbstractContext.MessageLogger getLogMessage() {
//        return upContext.getLogMessage();
//    }

    @Override
    public void updateUserLastActivity() {
        upContext.updateUserLastActivity();
    }

    @Override
    public long getUserLastActivity() {
        return upContext.getUserLastActivity();
    }

    @Override
    public LogInfo getLogInfo() {
        return upContext.getLogInfo();
    }

//    @Override
//    public void delayUserInteraction(ClientAction action) {
//        upContext.delayUserInteraction(action);
//    }

//    @Override
//    public Object requestUserInteraction(ClientAction action) {
//        return upContext.requestUserInteraction(action);
//    }

//    @Override
//    public boolean userInteractionCanBeProcessedInTransaction() {
//        return true;
//    }

//    @Override
//    public Object[] requestUserInteraction(ClientAction... actions) {
//        return upContext.requestUserInteraction(actions);
//    }

    @Override
    public FocusListener getFocusListener() {
        return upContext.getFocusListener();
    }

    @Override
    public CustomClassListener getClassListener() {
        return upContext.getClassListener();
    }

    @Override
    public Long getCurrentComputer() {
        return upContext.getCurrentComputer();
    }

    @Override
    public Long getCurrentConnection() {
        return upContext.getCurrentConnection();
    }

    @Override
    public Long getCurrentUser() {
        return upContext.getCurrentUser();
    }

    @Override
    public Long getCurrentUserRole() {
        return upContext.getCurrentUserRole();
    }

//    @Override
//    public String localize(LocalizedString s) {
//        return upContext.localize(s);
//    }

//    @Override
//    public String localize(LocalizedString s, Locale locale) {
//        return upContext.localize(s, locale);
//    }

    @Override
    public Locale getLocale() {
        return upContext.getLocale();
    }

    @Override
    public LocalePreferences getLocalePreferences() {
        return upContext.getLocalePreferences();
    }

    @Override
    public ConnectionContext getConnectionContext() {
        return upContext.getConnectionContext();
    }
}
