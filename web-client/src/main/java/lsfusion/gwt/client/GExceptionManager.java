package lsfusion.gwt.client;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.logging.impl.StackTracePrintStream;
import lsfusion.gwt.shared.GwtSharedUtils;
import lsfusion.gwt.shared.exceptions.NonFatalHandledException;
import lsfusion.gwt.shared.result.VoidResult;
import lsfusion.gwt.client.dispatch.NavigatorDispatchAsync;
import lsfusion.gwt.shared.actions.form.FormRequestIndexCountingAction;
import lsfusion.gwt.shared.actions.navigator.LogClientExceptionAction;
import net.customware.gwt.dispatch.shared.Action;

import java.io.PrintStream;
import java.util.*;

public class GExceptionManager {
    private final static List<Throwable> unreportedThrowables = new ArrayList<>();
    private final static Map<Throwable, Integer> unreportedThrowablesTryCount = new HashMap<>();
    
    public static void logClientError(String message, Throwable throwable) {
        logClientError(new LogClientExceptionAction(message, throwable), message, throwable);
    }
    
    public static void logClientError(NonFatalHandledException ex, String message) {
        logClientError(new LogClientExceptionAction(message, ex, ex.count, ex.reqId), message, ex);
    }

    public static void logClientError(LogClientExceptionAction action, String message, final Throwable throwable) {
        GWT.log(message, throwable);
        Log.error(message, throwable);

        try {
            NavigatorDispatchAsync dispatcher = MainFrame.navigatorDispatchAsync;
            if(dispatcher != null) { // dispatcher may be not initialized yet (at first look up logics call)
                dispatcher.execute(action, new ErrorHandlingCallback<VoidResult>() {
                    @Override
                    public void failure(Throwable caught) {
                        loggingFailed(caught, throwable);
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
        synchronized (unreportedThrowables) {
            final List<Throwable> stillUnreported = new ArrayList<>(unreportedThrowables);
            for (final Throwable t : unreportedThrowables) {
                Integer tryCount = unreportedThrowablesTryCount.get(t);
                try {
                    NavigatorDispatchAsync dispatcher = MainFrame.navigatorDispatchAsync;
                    if(dispatcher != null) { // dispatcher may be not initialized yet (at first look up logics call)
                        dispatcher.execute(new LogClientExceptionAction("Unreported client error, try count : " + (tryCount == null ? 0 : tryCount), t), new ErrorHandlingCallback<VoidResult>() {
                            @Override
                            public void failure(Throwable caught) {
                                Log.error("Error logging unreported client exception", caught);
                            }

                            @Override
                            public void success(VoidResult result) {
                                stillUnreported.remove(t);
                                unreportedThrowablesTryCount.remove(t);
                            }
                        });
                    }
                } catch (Throwable caught) {
                    Log.error("Error logging unreported client exception", caught);
                }
            }
            unreportedThrowables.clear();
            for(Throwable throwable : stillUnreported) {
                unreportedThrowables.add(throwable);

                Integer prevCount = unreportedThrowablesTryCount.get(throwable);
                unreportedThrowablesTryCount.put(throwable, prevCount == null ? 1 : prevCount + 1);
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
                            return Collections.singletonMap(key.getMessage() + getStackTrace(key), key.reqId);
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

    public static String getStackTrace(Throwable t) {
        StringBuilder sb = new StringBuilder();
        t.printStackTrace(new PrintStream(new StackTracePrintStream(sb)));
        return sb.toString();
    }
}
