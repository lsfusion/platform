package lsfusion.server.context;

import com.google.common.base.Throwables;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.interop.ModalityType;
import lsfusion.interop.action.*;
import lsfusion.interop.form.UserInputResult;
import lsfusion.server.Settings;
import lsfusion.server.auth.SecurityPolicy;
import lsfusion.server.classes.CustomClass;
import lsfusion.server.classes.DataClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.form.entity.ManageSessionType;
import lsfusion.server.form.entity.ObjectEntity;
import lsfusion.server.form.entity.filter.ContextFilter;
import lsfusion.server.form.instance.FormCloseType;
import lsfusion.server.form.instance.FormInstance;
import lsfusion.server.form.instance.PropertyObjectInterfaceInstance;
import lsfusion.server.form.instance.listener.CustomClassListener;
import lsfusion.server.form.instance.listener.FocusListener;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.LogicsInstance;
import lsfusion.server.logics.NullValue;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.property.DialogRequest;
import lsfusion.server.logics.property.PullChangeProperty;
import lsfusion.server.remote.RemoteForm;
import lsfusion.server.session.DataSession;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

import static lsfusion.base.BaseUtils.padLeft;
import static lsfusion.base.BaseUtils.replicate;
import static lsfusion.base.BaseUtils.serializeObject;
import static lsfusion.server.data.type.TypeSerializer.serializeType;

public abstract class AbstractContext implements Context {

    public abstract LogicsInstance getLogicsInstance();

    @Override
    public FormInstance getFormInstance() {
        return null;
    }

    @Override
    public void requestFormUserInteraction(FormInstance formInstance, ModalityType modalityType, boolean forbidDuplicate, ExecutionStack stack) throws SQLException, SQLHandledException {
        FormEntity formEntity = formInstance.entity;
        RemoteForm remoteForm = createRemoteForm(formInstance, stack);
        FormClientAction action = new FormClientAction(formEntity.getCanonicalName(), formEntity.getSID(), forbidDuplicate, remoteForm, remoteForm.getImmutableMethods(), Settings.get().isDisableFirstChangesOptimization() ? null : remoteForm.getFormChangesByteArray(stack), modalityType);
        if(modalityType.isModal()) {
            requestUserInteraction(action);
            formInstance.syncLikelyOnClose(true, stack);
        } else
            delayUserInteraction(action);
    }

    public ObjectValue requestUserObject(DialogRequest dialog, ExecutionStack stack) throws SQLException, SQLHandledException { // null если canceled
        FormInstance dialogInstance = dialog.createDialog();
        if (dialogInstance == null) {
            return null;
        }

        requestFormUserInteraction(dialogInstance, ModalityType.DIALOG_MODAL, false, stack);

        if (dialogInstance.getFormResult() == FormCloseType.CLOSE) {
            return null;
        }
        return dialogInstance.getFormResult() == FormCloseType.DROP ? NullValue.instance : dialog.getValue();
    }

    public ObjectValue requestUserData(DataClass dataClass, Object oldValue) {
        try {
            UserInputResult result = (UserInputResult) requestUserInteraction(new RequestUserInputClientAction(serializeType(dataClass), serializeObject(oldValue)));
            if (result.isCanceled()) {
                return null;
            }
            return result.getValue() == null ? NullValue.instance : new DataObject(result.getValue(), dataClass);
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    public ObjectValue requestUserClass(CustomClass baseClass, CustomClass defaultValue, boolean concrete) {
        try {
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            DataOutputStream dataStream = new DataOutputStream(outStream);
            baseClass.serialize(dataStream);
            defaultValue.serialize(dataStream);
            Long result = (Long) requestUserInteraction(new ChooseClassClientAction(outStream.toByteArray(), concrete));
            if (result == null) {
                return null;
            }
            return new DataObject(result, baseClass.getBaseClass().objectClass);
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
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

    private static class MessageLogger {
        private final List<LogMessage> messages = new ArrayList<>();
        private final Stack<Integer> startIndexes = new Stack<>();
        
        public void add(String message, boolean failed) {
            messages.add(new LogMessage(message, failed));
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

    @Override
    public ImList<LogMessage> popLogMessage() {
        MessageLogger logMessages = logMessage.get();
        ImList<LogMessage> result = logMessages.pop();
        if(logMessages.isEmpty())
            logMessage.remove();
        return result;
    }

    public static String getMessage(ClientAction action) {
        if (action instanceof LogMessageClientAction) {
            LogMessageClientAction logAction = (LogMessageClientAction) action;
            return logAction.message + "\n" + errorDataToTextTable(logAction.titles, logAction.data);
        } else if (action instanceof MessageClientAction) {
            MessageClientAction msgAction = (MessageClientAction) action;
            return msgAction.message;
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
                result.append(padLeft(cellText, columnWidths[j])).append("|");
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
                messageLogger.add(message, action instanceof LogMessageClientAction ? ((LogMessageClientAction) action).failed : false);
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
        return aspectRequestUserInteraction(actions, processClientActions(actions));
    }

    protected abstract void aspectDelayUserInteraction(ClientAction action, String message);

    protected abstract Object[] aspectRequestUserInteraction(ClientAction[] actions, String[] messages);

    @Override
    public boolean canBeProcessed() {
        return false;
    }

    public abstract SecurityPolicy getSecurityPolicy();

    public abstract FocusListener getFocusListener();

    public abstract CustomClassListener getClassListener();

    public abstract PropertyObjectInterfaceInstance getComputer(ExecutionStack stack);

    public abstract Long getCurrentUser();

    public abstract Long getCurrentUserRole();

    public abstract DataObject getConnection();

    @Override
    public String localize(LocalizedString s) {
        return localize(s, getLocale());
    }

    public String localize(LocalizedString s, Locale locale) {
        return s.getString(locale, getLogicsInstance().getBusinessLogics().getLocalizer());
    }

    public FormInstance createFormInstance(FormEntity formEntity, ImMap<ObjectEntity, ? extends ObjectValue> mapObjects, DataSession session, boolean isModal, Boolean noCancel, ManageSessionType manageSession, ExecutionStack stack, boolean checkOnOk, boolean showDrop, boolean interactive, ImSet<ContextFilter> contextFilters, ImSet<PullChangeProperty> pullProps, boolean readonly) throws SQLException, SQLHandledException {
        return new FormInstance(formEntity, getLogicsInstance(),
                session,
                getSecurityPolicy(), getFocusListener(), getClassListener(),
                getComputer(stack), getConnection(), mapObjects, stack, isModal,
                noCancel, manageSession,
                checkOnOk, showDrop, interactive, contextFilters, pullProps, readonly, getLocale());
    }

    public RemoteForm createRemoteForm(FormInstance formInstance, ExecutionStack stack) {
        throw new UnsupportedOperationException("createRemoteForm is not supported");
    }
}
