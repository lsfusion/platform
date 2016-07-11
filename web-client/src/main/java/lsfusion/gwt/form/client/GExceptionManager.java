package lsfusion.gwt.form.client;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.core.client.GWT;
import lsfusion.gwt.base.shared.MessageException;
import lsfusion.gwt.base.shared.actions.VoidResult;
import lsfusion.gwt.form.client.dispatch.NavigatorDispatchAsync;
import lsfusion.gwt.form.shared.actions.navigator.LogClientExceptionAction;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GExceptionManager {
    private final static List<Throwable> unreportedThrowables = new ArrayList<>();
    
    
    private static String getMessage(Throwable throwable) {
        String ret = "";
        while (throwable != null) {
            if (throwable instanceof com.google.gwt.event.shared.UmbrellaException || throwable instanceof com.google.web.bindery.event.shared.UmbrellaException) {
                for (Throwable thr2 : ((com.google.web.bindery.event.shared.UmbrellaException) throwable).getCauses()) {
                    if (!ret.isEmpty()) {
                        ret += "\nCaused by: ";
                    }
                    ret += thr2.toString();
                    ret += "\n\tat " + getMessage(thr2);
                }
            } else if (throwable instanceof MessageException) {
                if (!ret.isEmpty()) {
                    ret += "\nCaused by: ";
                }
                ret += throwable.toString();
                
                MessageException me = (MessageException) throwable;
                for (StackTraceElement sTE : (me.myTrace != null ? me.myTrace : me.getStackTrace())) {
                    ret += "\n\tat " + sTE;
                }
            } else {
                if (!ret.isEmpty()) {
                    ret += "\nCaused by: ";
                }
                ret += throwable.toString();
                for (StackTraceElement sTE : throwable.getStackTrace()) {
                    ret += "\n\tat " + sTE;
                }
            }
            throwable = throwable.getCause();
        }
        return ret;
    }
    
    public static void logClientError(String message, final Throwable throwable) {
        GWT.log(message, throwable);
        Log.error(message, throwable);
        
        NavigatorDispatchAsync.Instance.get().execute(new LogClientExceptionAction(message, getMessage(throwable)), new ErrorHandlingCallback<VoidResult>() {
            @Override
            public void failure(Throwable caught) {
                Log.error("Error logging client exception", caught);

                synchronized (unreportedThrowables) {
                    unreportedThrowables.add(throwable);
                }
            }
        });
    }

    public static void flushUnreportedThrowables() {
        synchronized (unreportedThrowables) {
            for (final Iterator<Throwable> iterator = unreportedThrowables.iterator(); iterator.hasNext(); ) {
                final Throwable t = iterator.next();

                NavigatorDispatchAsync.Instance.get().execute(new LogClientExceptionAction("Unreported client error", getMessage(t)), new ErrorHandlingCallback<VoidResult>() {
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
