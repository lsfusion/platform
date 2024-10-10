package lsfusion.server.base.controller.context;

import lsfusion.base.col.ListFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.interop.action.ClientAction;
import lsfusion.interop.action.MessageClientAction;
import lsfusion.interop.action.MessageClientType;
import lsfusion.interop.action.ResetServerSettingsCacheClientAction;
import lsfusion.interop.form.ShowFormType;
import lsfusion.interop.form.WindowFormType;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.LogicsInstance;
import lsfusion.server.logics.action.controller.stack.ExecutionStack;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.form.interactive.ManageSessionType;
import lsfusion.server.logics.form.interactive.action.async.InputList;
import lsfusion.server.logics.form.interactive.action.async.InputListAction;
import lsfusion.server.logics.form.interactive.action.input.InputContext;
import lsfusion.server.logics.form.interactive.action.input.InputResult;
import lsfusion.server.logics.form.interactive.controller.remote.RemoteForm;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ConnectionContext;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.interactive.listener.CustomClassListener;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.filter.ContextFilterInstance;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.property.oraction.ActionOrProperty;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Stack;

import static lsfusion.base.BaseUtils.padLeft;
import static lsfusion.base.BaseUtils.replicate;
import static lsfusion.server.physics.admin.log.ServerLoggers.systemLogger;

public abstract class AbstractContext implements Context {

    public abstract LogicsInstance getLogicsInstance();

    @Override
    public FormEntity getCurrentForm() {
        return null;
    }

    public static class LogMessage {
        public final long time;
        public final String message;
        public final boolean failed;
        public final String lsfStackTrace;

        public LogMessage(String message, boolean failed) {
            this(message, failed, null);
        }

        public LogMessage(String message, boolean failed, String lsfStackTrace) {
            this.message = message;
            this.failed = failed;
            this.lsfStackTrace = lsfStackTrace;
            this.time = System.currentTimeMillis();
        }
    }

    public static class MessageLogger {
        private final List<LogMessage> messages = new ArrayList<>();
        private final Stack<Integer> startIndexes = new Stack<>();
        
        public void add(String message, boolean failed) {
            messages.add(new LogMessage(message, failed));
        }
        
        public void addAll(ImList<LogMessage> addMessages) {
            ListFact.addJavaAll(addMessages, messages);
        }

        public void push() {
            startIndexes.push(messages.size());
        }

        public ImList<LogMessage> pop() {
            int index = startIndexes.pop();
            return ListFact.fromJavaList(messages.subList(index, messages.size()));
        }
        
        public boolean isEmpty() {
            return startIndexes.isEmpty();
        }
    }

    private ThreadLocal<MessageLogger> logMessage = new ThreadLocal<>();

    @Override
    public void pushLogMessage() {
        MessageLogger logMessages = logMessage.get();
        if(logMessages == null) {
            logMessages = new MessageLogger();
            logMessage.set(logMessages);
        }
        logMessages.push();
    }

    public MessageLogger getLogMessage() {
        return logMessage.get();
    }

    @Override
    public ImList<LogMessage> popLogMessage() {
        MessageLogger logMessages = logMessage.get();
        ImList<LogMessage> result = logMessages.pop();
        if(logMessages.isEmpty())
            logMessage.remove();
        return result;
    }

    public static String getMessage(ClientAction action) {
        if (action instanceof MessageClientAction) {
            MessageClientAction msgAction = (MessageClientAction) action;
            String exInfo = errorDataToTextTable(msgAction.titles, msgAction.data);
            return msgAction.textMessage + (exInfo.isEmpty() ? "" : "\n" + exInfo); //message can be null
        }
//        else if (action instanceof ConfirmClientAction) {
//            ConfirmClientAction confirmAction = (ConfirmClientAction) action;
//            return confirmAction.message;
//        }
        return null;
    }

    public static String errorDataToTextTable(List<String> titles, List<List<String>> data) {
        if (titles.size() == 0) {
            return "";
        }

        int rCount = data.size() + 1;
        int cCount = titles.size();

        ArrayList<List<String>> all = new ArrayList<>();
        all.add(titles);
        all.addAll(data);

        int columnWidths[] = new int[cCount];
        for (int i = 0; i < rCount; ++i) {
            List<String> rowData = all.get(i);
            for (int j = 0; j < cCount; ++j) {
                String cellText = rowData.get(j);
                columnWidths[j] = Math.max(columnWidths[j], cellText == null ? 0 : cellText.trim().length());
            }
        }

        int tableWidth = cCount + 1; //рамки
        for (int j = 0; j < cCount; ++j) {
            tableWidth += columnWidths[j];
        }

        String br = replicate('-', tableWidth) + "\n";

        StringBuilder result = new StringBuilder(br);
        for (int i = 0; i < rCount; ++i) {
            List<String> rowData = all.get(i);
            result.append("|");
            for (int j = 0; j < cCount; ++j) {
                String cellText = rowData.get(j);
                result.append(padLeft(cellText == null ? "" : cellText, columnWidths[j])).append("|");
            }
            result.append("\n");
            if (i == 0) {
                result.append(br);
            }
        }
        result.append(br);

        return result.toString();
    }

    private String processClientAction(ClientAction action) {
        String message = getMessage(action);
        if(message != null) {
            MessageLogger messageLogger = logMessage.get();
            if(messageLogger != null)
                messageLogger.add(message, action instanceof MessageClientAction && ((MessageClientAction) action).type == MessageClientType.ERROR);
            return message;
        }
        return null;
    }

    private String[] processClientActions(ClientAction[] actions) {
        String[] messages = new String[actions.length];
        for (int i = 0; i < actions.length; i++)
            messages[i] = processClientAction(actions[i]);
        return messages;
    }

    @Override
    public void delayUserInteraction(ClientAction action) {
        aspectDelayUserInteraction(action, processClientAction(action));
    }

    @Override
    public Object requestUserInteraction(ClientAction action) {
        return requestUserInteraction(new ClientAction[]{action})[0];
    }

    @Override
    public Object[] requestUserInteraction(ClientAction... actions) {
        // the problem is that we shouldn't pauseDispatching when it's delay call (not request), and vice a versa
        // usually in server we manage it manually (for now), but for backward compatibility, adding this check
        String[] messages = new String[actions.length];
        for (int i = 0; i < actions.length; i++) {
            ClientAction action = actions[i];
            if(action instanceof MessageClientAction)
                ((MessageClientAction) action).syncType = true;
        }
        return aspectRequestUserInteraction(actions, processClientActions(actions));
    }

    public void aspectDelayUserInteraction(ClientAction action, String message) {
        if(message != null)
            systemLogger.info("Server message: " + message);
        else if (!(action instanceof ResetServerSettingsCacheClientAction)) //todo. temporary fix. problem on empty DB is that ResetServerSettingsCacheClientAction is called when there is no client context yet.
            throw new UnsupportedOperationException("delayUserInteraction is not supported in server context, action : " + action.getClass());
    }

    public Object[] aspectRequestUserInteraction(ClientAction[] actions, String[] messages) {
        for (int i = 0; i < messages.length; i++) {
            String message = messages[i];
            if (message == null)
                throw new UnsupportedOperationException("requestUserInteraction is not supported in server context, action : " + actions[i].getClass());
        }
        return new Object[actions.length];
    }

    @Override
    public boolean userInteractionCanBeProcessedInTransaction() {
        return true;
    }

    public abstract CustomClassListener getClassListener();

    // used in global context (when there is no sql / data session), otherwise use SQLSession.contextProvider
    public abstract Long getCurrentComputer();
    public abstract Long getCurrentUser();
    public abstract Long getCurrentConnection();

    // needed for settings
    public abstract Long getCurrentUserRole();

    @Override
    public String localize(LocalizedString s) {
        return localize(s, getLocale());
    }

    public String localize(LocalizedString s, Locale locale) {
        return s.getString(locale, getLogicsInstance().getBusinessLogics().getLocalizer());
    }

    // UI interfaces, careful with that because RemoteNavigatorContext has multiple inheritance, so every that interfaces should be "proxied" there
    public void requestFormUserInteraction(FormInstance formInstance, ShowFormType showFormType, boolean forbidDuplicate, String formId, ExecutionStack stack) throws SQLException, SQLHandledException {
        throw new UnsupportedOperationException("requestFormUserInteraction is not supported");
    }

    public InputContext lockInputContext() {
        throw new UnsupportedOperationException("inputContext is not supported");
    }
    public void unlockInputContext() {
        throw new UnsupportedOperationException("inputContext is not supported");
    }
    public InputResult inputUserData(ActionOrProperty securityProperty, DataClass dataClass, Object oldValue, boolean hasOldValue, InputContext inputContext, String customChangeFunction, InputList inputList, InputListAction[] actions) {
        throw new UnsupportedOperationException("requestUserData is not supported");
    }

    private long userLastActivity;
    public void updateUserLastActivity() {
        userLastActivity = System.currentTimeMillis();
    }

    public long getUserLastActivity() {
        return userLastActivity;
    }

    public FormInstance createFormInstance(FormEntity formEntity, ImSet<ObjectEntity> inputObjects, ImMap<ObjectEntity, ? extends ObjectValue> mapObjects, DataSession session, boolean isModal, Boolean noCancel, ManageSessionType manageSession, ExecutionStack stack, boolean checkOnOk, boolean showDrop, boolean interactive, WindowFormType type, ImSet<ContextFilterInstance> contextFilters, boolean readonly) throws SQLException, SQLHandledException {
        throw new UnsupportedOperationException("createFormInstance is not supported");
    }

    public RemoteForm createRemoteForm(FormInstance formInstance, ExecutionStack stack) {
        throw new UnsupportedOperationException("createRemoteForm is not supported");
    }
}
