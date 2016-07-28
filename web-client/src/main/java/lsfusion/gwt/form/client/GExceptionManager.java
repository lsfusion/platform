package lsfusion.gwt.form.client;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.shared.SerializableThrowable;
import com.google.gwt.event.shared.UmbrellaException;
import lsfusion.gwt.base.shared.MessageException;
import lsfusion.gwt.base.shared.actions.VoidResult;
import lsfusion.gwt.form.client.dispatch.NavigatorDispatchAsync;
import lsfusion.gwt.form.shared.actions.navigator.LogClientExceptionAction;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GExceptionManager {
    private final static List<Throwable> unreportedThrowables = new ArrayList<>();
    
    public static void logClientError(String message, final Throwable throwable) {
        GWT.log(message, throwable);
        Log.error(message, throwable);
        
        NavigatorDispatchAsync.Instance.get().execute(new LogClientExceptionAction(message, toSerializable(throwable)), new ErrorHandlingCallback<VoidResult>() {
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
}
