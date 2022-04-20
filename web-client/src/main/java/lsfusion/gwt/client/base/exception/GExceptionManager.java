package lsfusion.gwt.client.base.exception;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.shared.SerializableThrowable;
import com.google.gwt.logging.impl.StackTracePrintStream;
import lsfusion.gwt.client.base.GwtSharedUtils;
import lsfusion.gwt.client.base.result.VoidResult;
import lsfusion.gwt.client.controller.remote.action.PriorityErrorHandlingCallback;
import lsfusion.gwt.client.controller.remote.action.form.FormRequestAction;
import lsfusion.gwt.client.controller.remote.action.navigator.LogClientExceptionAction;
import lsfusion.gwt.client.controller.remote.action.navigator.NavigatorRequestAction;
import lsfusion.gwt.client.navigator.controller.dispatch.NavigatorDispatchAsync;
import lsfusion.gwt.client.view.MainFrame;
import net.customware.gwt.dispatch.shared.Action;

import java.io.PrintStream;
import java.util.*;

public class GExceptionManager {
    private final static Set<Throwable> unreportedThrowables = new HashSet<>();

    public static void logClientError(Throwable throwable) {
        logClientError(new LogClientExceptionAction(throwable), throwable);
    }
    
    public static void logClientError(NonFatalHandledException ex) {
        logClientError(new LogClientExceptionAction(ex), ex);
    }

    public static void logClientError(LogClientExceptionAction action, final Throwable throwable) {
        GWT.log("", throwable);
        Log.error("", throwable);

        try {
            NavigatorDispatchAsync dispatcher = MainFrame.navigatorDispatchAsync;
            if(dispatcher != null) { // dispatcher may be not initialized yet (at first look up logics call)
                dispatcher.executePriority(action, new PriorityErrorHandlingCallback<VoidResult>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        loggingFailed(caught, throwable);
                        //commented because we don't want to repeat logClientError exceptions
                        //super.onFailure(caught);
                    }
                });
            }
        } catch (Throwable caught) {
            loggingFailed(caught, throwable);
        }
    }

    private static void loggingFailed(Throwable caught, Throwable throwable) {
        Log.error("Error logging client exception", caught);

        synchronized (unreportedThrowables) {
            unreportedThrowables.add(throwable);
        }
    }

    public static void flushUnreportedThrowables() {
        final List<Throwable> stillUnreported;
        synchronized (unreportedThrowables) {
            stillUnreported = new ArrayList<>(unreportedThrowables);
        }
        for (final Throwable t : stillUnreported) {
            try {
                NavigatorDispatchAsync dispatcher = MainFrame.navigatorDispatchAsync;
                if(dispatcher != null) { // dispatcher may be not initialized yet (at first look up logics call)
                    dispatcher.executePriority(new LogClientExceptionAction(t), new PriorityErrorHandlingCallback<VoidResult>() {
                        @Override
                        public void onFailure(Throwable caught) {
                            Log.error("Error logging unreported client exception", caught);
                            //commented because we don't want to repeat logClientError exceptions
                            //super.onFailure(caught);
                        }

                        @Override
                        public void onSuccess(VoidResult result) {
                            synchronized (unreportedThrowables) {
                                unreportedThrowables.remove(t);
                            }
                        }
                    });
                }
            } catch (Throwable caught) {
                Log.error("Error logging unreported client exception", caught);
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
        if (action instanceof FormRequestAction) {
            reqId = ((FormRequestAction) action).requestIndex;
        } else if(action instanceof NavigatorRequestAction) {
            reqId = ((NavigatorRequestAction) action).requestIndex;
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

        SerializableThrowable thisStack = new SerializableThrowable("", "");
        NonFatalHandledException e = new NonFatalHandledException(copyMessage(t), thisStack, reqId);
        GExceptionManager.copyStackTraces(t, thisStack); // it seems that it is useless because only SerializableThrowable stacks are copied (see StackException)
        exceptions.add(e);
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
                            return Collections.singletonMap(key.getMessage() + getStackTrace(key), key.reqId);
                        }
                    }, flushExceptions);

                    for (Map.Entry<Map, Collection<NonFatalHandledException>> entry : group.entrySet()) {
                        Collection<NonFatalHandledException> all = entry.getValue();
                        NonFatalHandledException nonFatal = all.iterator().next();
                        nonFatal.count = all.size();
                        logClientError(nonFatal);
                    }
                }
            });
        }
    }

    // for debug purposes
    private static boolean rounded;
    private static int current;
    private static SerializableThrowable[] stacks = new SerializableThrowable[10];

    public static void addStackTrace(String message) {
        if(!MainFrame.devMode)
            return;

        SerializableThrowable throwable = new SerializableThrowable("", System.currentTimeMillis() + " " + message);
        throwable.setStackTrace(new Exception().getStackTrace());
        if(current >= stacks.length) {
            current = 0;
            rounded = true;
        }
        stacks[current++] = throwable;
    }

    public static RuntimeException propagate(Throwable t) {
        if(t instanceof Error)
            throw (Error)t;
        if(t instanceof RuntimeException)
            throw (RuntimeException)t;
        throw new RuntimeException(t);
    }

    public static void throwStackedException(String message) {
        StackedException exception = getStackedException(message);
        if(exception != null)
            throw exception;
    }

    public static StackedException getStackedException(String message) {
        if(!MainFrame.devMode)
            return null;
        
        SerializableThrowable[] result = new SerializableThrowable[rounded ? stacks.length : current];
        int f = rounded ? current + 1 : 0;
        for(int i=0;i<result.length;i++) {
            if(f >= stacks.length)
                f = 0;
            result[i] = stacks[f++];
        }
        SerializableThrowable thisStack = new SerializableThrowable("", "");
        StackedException exception = new StackedException(message, thisStack, result);
        copyStackTraces(exception, thisStack); // we need this because serializable throwable by default has no stack
        return exception;
    }

    public static String getStackTrace(Throwable t) {
        StringBuilder sb = new StringBuilder();
        t.printStackTrace(new PrintStream(new StackTracePrintStream(sb)));
        return sb.toString();
    }

    // the same as in ExceptionUtils
    public static Throwable getRootCause(Throwable throwable) {
        Throwable result = throwable;
        while (result != null && result.getCause() != null) {
            result = result.getCause();
        }

        return result;
    }

    // the same as in ExceptionUtils
    // when class of throwable changes
    public static String copyMessage(Throwable throwable) {
        throwable = getRootCause(throwable); // also it may make sense to show also messages of chained exceptions, but for now will show only root
        return throwable.getClass().getName() + " " + throwable.getMessage();
    }

    // the same as in ExceptionUtils
    // assuming that there should be primitive copy (Strings and other very primitive Java classes)  
    public static void copyStackTraces(Throwable from, Throwable to) {
        from = getRootCause(from); // chained exception stacks are pretty useless (they are always the same as root + line in catch, which is usually pretty evident)
        to.setStackTrace(from.getStackTrace());
    }
}
