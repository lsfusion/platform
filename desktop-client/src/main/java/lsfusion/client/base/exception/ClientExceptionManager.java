package lsfusion.client.base.exception;

import lsfusion.base.ExceptionUtils;
import lsfusion.client.base.SwingUtils;
import lsfusion.client.base.log.Log;
import lsfusion.client.controller.remote.ConnectionLostManager;
import lsfusion.client.view.MainFrame;
import lsfusion.interop.base.exception.RemoteAbandonedException;
import lsfusion.interop.base.exception.RemoteClientException;
import lsfusion.interop.base.exception.RemoteHandledException;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.rmi.ConnectException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CancellationException;

import static lsfusion.client.ClientResourceBundle.getString;

public class ClientExceptionManager {
    private final static Logger logger = Logger.getLogger(ClientExceptionManager.class);
    private final static List<Throwable> unreportedThrowables = new ArrayList<>();
    
    public static Throwable fromDesktopClientToAppServer(Throwable e) {
        if (e instanceof RemoteHandledException) {
            assert e.getCause() == null;
            return e;
        }

        // in theory, here should be only swing exceptions, so server has all this exceptions and will be able do deserialize them
        // but we want to get rid of chained exceptions
        String message = ExceptionUtils.copyMessage(e);
        Throwable throwable;
        // we want to keep remoteException class to show it as unhandled remote exception in server log
        // however all remote calls are usually wrapped in runRetryableRequest, where RemoteException is handled and wrapped into RemoteClientException
        if(ExceptionUtils.getRootCause(e) instanceof RemoteException)
            throwable = new RemoteException(message);
        else
            throwable = new Throwable(getString("errors.internal.client.error")
                    + ": " + message);
        ExceptionUtils.copyStackTraces(e, throwable);
        return throwable;  
    }

    public static void handle(final Throwable e) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                logger.error("Client error: ", e);

                if(e instanceof RemoteAbandonedException || e instanceof CancellationException) // we don't need to log that exception
                    return;

                reportThrowable(e);
            }
        });
    }

    // some java and docking frames not critical bugs
    private static boolean ignoreException(Throwable exception) {
        if(exception instanceof ConcurrentModificationException && ExceptionUtils.toString(exception).contains("bibliothek.gui.dock.themes.basic.action.buttons.ButtonPanel.setForeground("))
            return true;
        if(exception instanceof IllegalArgumentException) {
            String exceptionString = ExceptionUtils.toString(exception);
            if(exceptionString.contains("Comparison method violates its general contract!") && exceptionString.contains("sun.awt.datatransfer.DataTransferer.setToSortedDataFlavorArray("))
                return true;
        }
        if(exception instanceof IllegalComponentStateException && ExceptionUtils.toString(exception).contains("component must be showing on the screen to determine its location"))
            return true;
        return false;
    }

    public static void reportThrowable(Throwable exception) {
        SwingUtils.assertDispatchThread();

        if(ignoreException(exception)) {
            logger.error("Ignoring throwable : " + exception, exception);
            return;
        }

        exception = fromDesktopClientToAppServer(exception);

        assert exception.getCause() == null;

        if(exception instanceof RemoteException) 
            ConnectionLostManager.connectionBroke();
        else if(!(exception instanceof RemoteClientException)) // we don't want to show remote handled exceptions
            Log.error(exception);

        logger.error("Reporting throwable : " + exception, exception);

        synchronized (unreportedThrowables) {
            boolean reported = false;
            try {
                reported = clientExceptionLog(exception);
            } catch (ConnectException ex) {
                logger.error("Error reporting client connect exception: " + exception, ex);
            } catch (Throwable ex) {
                logger.error("Error reporting client exception: " + exception, ex);
            }
            if(!reported)
                unreportedThrowables.add(exception);
        }
    }

    public static boolean clientExceptionLog(Throwable exception) throws RemoteException {
        if(MainFrame.instance != null)
            return MainFrame.instance.clientExceptionLog(exception);
        return false;        
    }

    public static void flushUnreportedThrowables() {
        synchronized (unreportedThrowables) {
            for (Iterator<Throwable> iterator = unreportedThrowables.iterator(); iterator.hasNext(); ) {
                Throwable t = iterator.next();
                boolean reported = false;
                try {
                    reported = clientExceptionLog(t);
                } catch (Throwable e) {
                    logger.error("Error reporting unreported client exception: " + t, e);
                }
                if(reported)
                    iterator.remove();
            }
        }
    }
}
