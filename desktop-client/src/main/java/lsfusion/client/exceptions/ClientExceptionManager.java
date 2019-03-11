package lsfusion.client.exceptions;

import lsfusion.base.ExceptionUtils;
import lsfusion.client.Log;
import lsfusion.client.Main;
import lsfusion.client.SwingUtils;
import lsfusion.client.rmi.ConnectionLostManager;
import lsfusion.interop.exceptions.*;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.rmi.ConnectException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;

import static lsfusion.client.ClientResourceBundle.getString;

public class ClientExceptionManager {
    private final static Logger logger = Logger.getLogger(ClientExceptionManager.class);
    private final static List<Throwable> unreportedThrowables = new ArrayList<>();
    
    public static Throwable fromDesktopClientToAppServer(Throwable e) {
        if (e instanceof RemoteServerException || e instanceof RemoteClientException) {
            assert e.getCause() == null;
            return e;
        }

        // in theory, here should be only swing exceptions, so server has all this exceptions and will be able do deserialize them
        // but we want to get rid of chained exceptions
        String message = ExceptionUtils.copyMessage(e);
        Throwable throwable;
        if(e instanceof RemoteException) // we want to keep remoteException class to show it as unhandled remote exception in server log
            throwable = new RemoteException(message);
        else
            throwable = new Throwable(getString("errors.internal.client.error")
                    + ": " + message);
        ExceptionUtils.copyStackTraces(e, throwable);
        return throwable;  
    }

    public static Throwable getRemoteExceptionCause(Throwable e) {
        for (Throwable ex = e; ex != null && ex != ex.getCause(); ex = ex.getCause()) {
            if (ex instanceof RemoteException || ex instanceof RemoteServerException || ex instanceof RemoteClientException || ex instanceof RemoteAbandonedException) {
                assert !(ex instanceof NonFatalRemoteClientException);
                return ex;
            }
        }
        return null;
    }

    public static void handle(final Throwable e) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                logger.error("Client error: ", e);

                Throwable exception = getRemoteExceptionCause(e);
                if(exception instanceof RemoteAbandonedException)
                    return;
                if(exception == null)
                    exception = e;

                reportThrowable(exception);
            }
        });
    }

    public static void reportThrowable(Throwable exception) {
        SwingUtils.assertDispatchThread();
        exception = fromDesktopClientToAppServer(exception);

        assert exception.getCause() == null;

        if(exception instanceof RemoteException) 
            ConnectionLostManager.connectionBroke();
        else if(!(exception instanceof RemoteClientException)) // we don't want to show remote handled exceptions
            Log.error(exception);

        logger.error("Reporting throwable : " + exception, exception);

        if (!(exception instanceof ConcurrentModificationException && ExceptionUtils.toString(exception).contains("bibliothek.gui.dock.themes.basic.action.buttons.ButtonPanel.setForeground"))) {
            synchronized (unreportedThrowables) {
                boolean reported = false;
                try {
                    reported = Main.clientExceptionLog("Client error", exception);
                } catch (ConnectException ex) {
                    logger.error("Error reporting client connect exception: " + exception, ex);
                } catch (Throwable ex) {
                    logger.error("Error reporting client exception: " + exception, ex);
                }
                if(!reported)
                    unreportedThrowables.add(exception);
            }
        }
    }

    public static void flushUnreportedThrowables() {
        synchronized (unreportedThrowables) {
            for (Iterator<Throwable> iterator = unreportedThrowables.iterator(); iterator.hasNext(); ) {
                Throwable t = iterator.next();
                boolean reported = false;
                try {
                    reported = Main.clientExceptionLog("Unreported client error", t);
                } catch (Throwable e) {
                    logger.error("Error reporting unreported client exception: " + t, e);
                }
                if(reported)
                    iterator.remove();
            }
        }
    }
}
