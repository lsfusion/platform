package lsfusion.gwt.form.client;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.shared.SerializableThrowable;
import com.google.gwt.event.shared.UmbrellaException;
import com.google.gwt.logging.impl.StackTracePrintStream;
import lsfusion.gwt.base.shared.GwtSharedUtils;
import lsfusion.gwt.base.shared.MessageException;
import lsfusion.gwt.base.shared.NonFatalHandledException;
import lsfusion.gwt.base.shared.actions.VoidResult;
import lsfusion.gwt.form.client.dispatch.NavigatorDispatchAsync;
import lsfusion.gwt.form.shared.actions.form.FormRequestIndexCountingAction;
import lsfusion.gwt.form.shared.actions.navigator.LogClientExceptionAction;
import net.customware.gwt.dispatch.shared.Action;

import java.io.PrintStream;
import java.util.*;

public class GExceptionManager {
    private final static List<Throwable> unreportedThrowables = new ArrayList<>();
    
    public static void logClientError(String message, Throwable throwable) {
        logClientError(new LogClientExceptionAction(message, toSerializable(throwable)), message, throwable);
    }
    
    public static void logClientError(NonFatalHandledException ex, String message) {
        logClientError(new LogClientExceptionAction(message, toSerializable(ex), ex.count, ex.reqId), message, ex);
    }

    public static void logClientError(LogClientExceptionAction action, String message, final Throwable throwable) {
        GWT.log(message, throwable);
        Log.error(message, throwable);

        NavigatorDispatchAsync.Instance.get().execute(action, new ErrorHandlingCallback<VoidResult>() {
            @Override
            public void failure(Throwable caught) {
                Log.error("Error logging client exception", caught);

                synchronized (unreportedThrowables) {
                    unreportedThrowables.add(throwable);
                }
            }
        });
    }
    
    private static SerializableThrowable toSerializable(Throwable t) {
        Throwable originalT = (t instanceof UmbrellaException || t instanceof com.google.web.bindery.event.shared.UmbrellaException) ? t.getCause() : t;
        SerializableThrowable st = new SerializableThrowable(originalT.getClass().getName(), originalT.getMessage());
        StackTraceElement[] stackTrace;
        if (t instanceof MessageException && ((MessageException) t).myTrace != null) {
            stackTrace = ((MessageException) t).myTrace;
        } else {
            stackTrace = originalT.getStackTrace();
        }
        st.setStackTrace(stackTrace);
        st.setDesignatedType(originalT.getClass().getName(), true);
        return st;
    }

    public static void flushUnreportedThrowables() {
        synchronized (unreportedThrowables) {
            for (final Iterator<Throwable> iterator = unreportedThrowables.iterator(); iterator.hasNext(); ) {
                final Throwable t = iterator.next();

                NavigatorDispatchAsync.Instance.get().execute(new LogClientExceptionAction("Unreported client error", toSerializable(t)), new ErrorHandlingCallback<VoidResult>() {
                    @Override
                    public void failure(Throwable caught) {
                        Log.error("Error logging unreported client exception", caught);
                    }

                    @Override
                    public void success(VoidResult result) {
                        iterator.remove();
                    }
                });
            }
        }
    }

    private static final HashMap<Action, List<NonFatalHandledException>> failedNotFatalHandledRequests = new LinkedHashMap<>();

    public static void addFailedRmiRequest(Throwable t, Action action) {
        List<NonFatalHandledException> exceptions = failedNotFatalHandledRequests.get(action);
        if(exceptions == null) {
            exceptions = new ArrayList<>();
            failedNotFatalHandledRequests.put(action, exceptions);
        }

        long reqId;
        if (action instanceof FormRequestIndexCountingAction) {
            reqId = ((FormRequestIndexCountingAction) action).requestIndex;
        } else {
            int ind = -1;
            for (Map.Entry<Action, List<NonFatalHandledException>> actionListEntry : failedNotFatalHandledRequests.entrySet()) {
                ind++;
                if (actionListEntry.getKey() == action) {
                    break;
                }
            }
            reqId = ind;
        }
        
        exceptions.add(new NonFatalHandledException(t, reqId));
    }

    public static void flushFailedNotFatalRequests(Action action) {
        final List<NonFatalHandledException> flushExceptions = failedNotFatalHandledRequests.remove(action);
        if(flushExceptions != null) {
            Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                @Override
                public void execute() {
                    Map<Map, Collection<NonFatalHandledException>> group;
                    group = GwtSharedUtils.group(new GwtSharedUtils.Group<Map, NonFatalHandledException>() {
                        public Map group(NonFatalHandledException key) {
                            return Collections.singletonMap(key.getMessage() + getStackTraceString(key), key.reqId);
                        }
                    }, flushExceptions);

                    for (Map.Entry<Map, Collection<NonFatalHandledException>> entry : group.entrySet()) {
                        Collection<NonFatalHandledException> all = entry.getValue();
                        NonFatalHandledException nonFatal = all.iterator().next();
                        nonFatal.count = all.size();
                        logClientError(nonFatal, "Connection error");
                    }
                }
            });
        }
    }

    public static String getStackTraceString(Throwable t) {
        StringBuilder sb = new StringBuilder();
        t.printStackTrace(new PrintStream(new StackTracePrintStream(sb)));
        return sb.toString();
    }
}
