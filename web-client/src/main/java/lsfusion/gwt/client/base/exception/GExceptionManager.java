package lsfusion.gwt.client.base.exception;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptException;
import com.google.gwt.core.shared.SerializableThrowable;
import com.google.gwt.logging.impl.StackTracePrintStream;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.result.VoidResult;
import lsfusion.gwt.client.base.view.PopupOwner;
import lsfusion.gwt.client.controller.remote.action.PriorityErrorHandlingCallback;
import lsfusion.gwt.client.controller.remote.action.navigator.LogClientExceptionAction;
import lsfusion.gwt.client.navigator.controller.dispatch.NavigatorDispatchAsync;
import lsfusion.gwt.client.view.MainFrame;

import java.io.PrintStream;
import java.util.*;

public class GExceptionManager {
    private final static Set<Throwable> unreportedThrowables = new HashSet<>();

    public static void logClientError(final Throwable throwable, PopupOwner popupOwner) {
        LogClientExceptionAction action = new LogClientExceptionAction(throwable);
        GWT.log("", throwable);
        Log.error("", throwable);

        try {
            NavigatorDispatchAsync dispatcher = MainFrame.navigatorDispatchAsync;
            if(dispatcher != null) { // dispatcher may be not initialized yet (at first look up logics call)
                dispatcher.executePriority(action, new PriorityErrorHandlingCallback<VoidResult>(popupOwner) {
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

    public static void flushUnreportedThrowables(PopupOwner popupOwner) {
        final List<Throwable> stillUnreported;
        synchronized (unreportedThrowables) {
            stillUnreported = new ArrayList<>(unreportedThrowables);
        }
        for (final Throwable t : stillUnreported) {
            try {
                NavigatorDispatchAsync dispatcher = MainFrame.navigatorDispatchAsync;
                if(dispatcher != null) { // dispatcher may be not initialized yet (at first look up logics call)
                    dispatcher.executePriority(new LogClientExceptionAction(t), new PriorityErrorHandlingCallback<VoidResult>(popupOwner) {
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
        try {
            StringBuilder sb = new StringBuilder();
            t.printStackTrace(new PrintStream(new StackTracePrintStream(sb)));
            return sb.toString();
        } catch (JavaScriptException caught) {
            Log.error("Error logging stackTrace", caught);
            return null;
        }
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
        StackTraceElement[] fromStackTrace = from.getStackTrace();

        if (from instanceof JavaScriptException)
            fromStackTrace = GwtClientUtils.add(parseJSExceptionStack(from).toArray(new StackTraceElement[0]), fromStackTrace, StackTraceElement[]::new);

        to.setStackTrace(fromStackTrace);
    }

    public static List<StackTraceElement> parseJSExceptionStack(Throwable exception) {
        List<StackTraceElement> stack = new ArrayList<>();
        for (String s : getJsExceptionStack(exception).split("\n")) {
            s = s.trim();
            if (s.contains("[")) //Parsing to the first line with square brackets because after will be trash from GWT
                break;

            int openBracketIndex = s.lastIndexOf("("); //in brackets name:lineNumber:characterNumber
            int lastColonIndex = s.lastIndexOf(":");

            if (openBracketIndex != -1 && lastColonIndex != -1 && openBracketIndex < lastColonIndex) {
                String fileNameWithLineNumber = s.substring(openBracketIndex + 1, lastColonIndex); //file name with line number
                int colonIndex = fileNameWithLineNumber.lastIndexOf(":");

                String filename = fileNameWithLineNumber.substring(0, colonIndex); // file name
                String lineNumber = fileNameWithLineNumber.substring(colonIndex + 1); // line number

                // string stack element example "at functionName (fileName:line:character)"
                stack.add(new StackTraceElement("Unknown", s.substring(s.indexOf("at ") + 3, s.indexOf(" (")), filename, Integer.parseInt(lineNumber)));
            }
        }
        return stack;
    }

    private static native String getJsExceptionStack(Throwable exception)/*-{
        return exception.@Throwable::backingJsObject.stack;
    }-*/;
}
