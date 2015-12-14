package lsfusion.server.context;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.ConcurrentWeakHashMap;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.interop.ModalityType;
import lsfusion.interop.action.ChooseClassClientAction;
import lsfusion.interop.action.ClientAction;
import lsfusion.interop.action.FormClientAction;
import lsfusion.interop.action.RequestUserInputClientAction;
import lsfusion.interop.form.UserInputResult;
import lsfusion.server.Settings;
import lsfusion.server.auth.SecurityPolicy;
import lsfusion.server.classes.CustomClass;
import lsfusion.server.classes.DataClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.form.entity.ObjectEntity;
import lsfusion.server.form.entity.PropertyDrawEntity;
import lsfusion.server.form.entity.filter.FilterEntity;
import lsfusion.server.form.instance.FormCloseType;
import lsfusion.server.form.instance.FormInstance;
import lsfusion.server.form.instance.FormSessionScope;
import lsfusion.server.form.instance.PropertyObjectInterfaceInstance;
import lsfusion.server.form.instance.listener.CustomClassListener;
import lsfusion.server.form.instance.listener.FocusListener;
import lsfusion.server.logics.*;
import lsfusion.server.logics.property.DialogRequest;
import lsfusion.server.logics.property.PullChangeProperty;
import lsfusion.server.remote.RemoteForm;
import lsfusion.server.session.DataSession;
import lsfusion.server.session.UpdateCurrentClasses;
import lsfusion.base.ProgressBar;
import lsfusion.server.stack.ExecutionStackItem;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static lsfusion.base.BaseUtils.serializeObject;
import static lsfusion.server.data.type.TypeSerializer.serializeType;

public abstract class AbstractContext implements Context {
    public final ConcurrentWeakHashMap<Thread, TimedMessageStack> actionMessageStackMap = new ConcurrentWeakHashMap<>();

    public abstract LogicsInstance getLogicsInstance();

    @Override
    public FormInstance getFormInstance() {
        return null;
    }

    public ObjectValue requestUserObject(DialogRequest dialog) throws SQLException, SQLHandledException { // null если canceled
        FormInstance dialogInstance = dialog.createDialog();
        if (dialogInstance == null) {
            return null;
        }

        RemoteForm remoteForm = createRemoteForm(dialogInstance);
        requestUserInteraction(
                new FormClientAction(
                        dialogInstance.entity.getCanonicalName(),
                        dialogInstance.entity.getSID(),
                        remoteForm,
                        remoteForm.getImmutableMethods(),
                        Settings.get().isDisableFirstChangesOptimization() ? null : remoteForm.getFormChangesByteArray(),
                        ModalityType.DIALOG_MODAL));

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
            Integer result = (Integer) requestUserInteraction(new ChooseClassClientAction(outStream.toByteArray(), concrete));
            if (result == null) {
                return null;
            }
            return new DataObject(result, baseClass.getBaseClass().objectClass);
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public String getLogMessage() {
        throw new UnsupportedOperationException("getLogMessage is not supported");
    }

    @Override
    public void delayUserInteraction(ClientAction action) {
        throw new UnsupportedOperationException("delayUserInteraction is not supported");
    }

    @Override
    public Object requestUserInteraction(ClientAction action) {
        return requestUserInteraction(new ClientAction[]{action})[0];
    }

    @Override
    public boolean canBeProcessed() {
        return false;
    }

    @Override
    public Object[] requestUserInteraction(ClientAction... actions) {
        throw new UnsupportedOperationException("requestUserInteraction is not supported");
    }

    public abstract SecurityPolicy getSecurityPolicy();

    public abstract FocusListener getFocusListener();

    public abstract CustomClassListener getClassListener();

    public abstract PropertyObjectInterfaceInstance getComputer();

    public abstract Integer getCurrentUser();

    public abstract DataObject getConnection();

    public UpdateCurrentClasses getUpdateCurrentClasses(UpdateCurrentClasses outerUpdateCurrentClasses) {
        return outerUpdateCurrentClasses;
    }

    public FormInstance createFormInstance(FormEntity formEntity, ImMap<ObjectEntity, ? extends ObjectValue> mapObjects, DataSession session, boolean isModal, boolean isAdd, FormSessionScope sessionScope, UpdateCurrentClasses outerUpdateCurrentClasses, boolean checkOnOk, boolean showDrop, boolean interactive, ImSet<FilterEntity> contextFilters, PropertyDrawEntity initFilterProperty, ImSet<PullChangeProperty> pullProps) throws SQLException, SQLHandledException {
        return new FormInstance(formEntity, getLogicsInstance(),
                sessionScope.createSession(session),
                getSecurityPolicy(), getFocusListener(), getClassListener(),
                getComputer(), getConnection(), mapObjects, getUpdateCurrentClasses(outerUpdateCurrentClasses), isModal,
                isAdd, sessionScope,
                checkOnOk, showDrop, interactive, contextFilters, initFilterProperty, pullProps);
    }

    public RemoteForm createRemoteForm(FormInstance formInstance) {
        throw new UnsupportedOperationException("createRemoteForm is not supported");
    }

    public String getActionMessage() {
        String result = "";
        for(MessageStack messageStack : getMessageStackList()) {
            result += messageStack.getMessage();
        }
        return result;
    }

    public List<Object> getActionMessageList() {
        List<Object> result = new ArrayList<>();
        for(MessageStack messageStack : getMessageStackList()) {
            result.addAll(messageStack.getMessageList());
        }
        return result;
    }

    public Thread getLastThread() {
        List<Map.Entry<Thread, TimedMessageStack>> list = getSortedActionMessageStackMap();

        List<Thread> threadList = new ArrayList<>();
        for (Map.Entry<Thread, TimedMessageStack> entry : list)
            threadList.add(entry.getKey());
        //last one is interrupt thread
        return threadList.size() < 2 ? null : threadList.get(threadList.size() - 2);
    }

    private List<MessageStack> getMessageStackList() {
        List<Map.Entry<Thread, TimedMessageStack>> list = getSortedActionMessageStackMap();

        List<MessageStack> messageStackList = new ArrayList<>();
        for (Map.Entry<Thread, TimedMessageStack> entry : list)
            messageStackList.add(entry.getValue().messageStack);
        return messageStackList;
    }

    private List<Map.Entry<Thread, TimedMessageStack>> getSortedActionMessageStackMap() {
        // Convert Map to List
        List<Map.Entry<Thread, TimedMessageStack>> list = new ArrayList<>(actionMessageStackMap.entrySet());
        // Sort list with comparator
        Collections.sort(list, new Comparator<Map.Entry<Thread, TimedMessageStack>>() {
            public int compare(Map.Entry<Thread, TimedMessageStack> o1,
                               Map.Entry<Thread, TimedMessageStack> o2) {
                return (o1.getValue().time).compareTo(o2.getValue().time);
            }
        });
        return list;
    }

    // тут нужно быть аккуратно с утечками
    public void pushActionMessage(ExecutionStackItem stackItem) {
        Thread thread = Thread.currentThread();
        TimedMessageStack timedMessageStack = actionMessageStackMap.get(thread);
        if (timedMessageStack == null) {
            timedMessageStack = new TimedMessageStack(new MessageStack());
            actionMessageStackMap.put(thread, timedMessageStack);
        }
        timedMessageStack.time = System.currentTimeMillis();
        timedMessageStack.messageStack.push(stackItem);
    }

    public void popActionMessage(ExecutionStackItem stackItem) {
        Thread thread = Thread.currentThread();
        TimedMessageStack timedMessageStack = actionMessageStackMap.get(thread);
        timedMessageStack.messageStack.popOrEmpty();
        if (timedMessageStack.messageStack.isEmpty()) {
            actionMessageStackMap.remove(thread);
        }
    }

    private static class MessageStack extends Stack<ExecutionStackItem> {

        public synchronized String getMessage() {
            return BaseUtils.toString(this, "\n");
        }

        public synchronized Object popOrEmpty() {
            return isEmpty() ? "" : pop();
        }

        public synchronized List<Object> getMessageList() {
            List<Object> result = new ArrayList<>();
            for (Object entry : this) {
                if (entry instanceof ExecutionStackItem) {
                    ExecutionStackItem stackItem = (ExecutionStackItem) entry;

                    if (stackItem.isCancelable())
                        result.add(true);

                    ImList<ProgressBar> progress = stackItem.getProgress();
                    if (progress == null || progress.isEmpty())
                        result.add(String.valueOf(stackItem));
                    else
                        result.addAll(progress.toJavaList());
                } else
                    result.add(String.valueOf(entry));
            }
            return result;
        }
    }

    private class TimedMessageStack {
        private MessageStack messageStack;
        private Long time;

        public TimedMessageStack(MessageStack messageStack) {
            this.messageStack = messageStack;
        }
    }

    private ScheduledExecutorService executor;
    @Override
    public ScheduledExecutorService getExecutorService() {
        if(executor==null)
            synchronized (this) {
                if(executor==null)
                    executor = Executors.newScheduledThreadPool(50, new ContextAwareDaemonThreadFactory(this, "newthread-pool"));
            }
        return executor;
    }
}
