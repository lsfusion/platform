package lsfusion.gwt.client.controller.remote;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.shared.SerializableThrowable;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.AtomicBoolean;
import lsfusion.gwt.client.base.AtomicLong;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.GwtSharedUtils;
import lsfusion.gwt.client.base.exception.GExceptionManager;
import lsfusion.gwt.client.base.exception.NonFatalHandledException;
import lsfusion.gwt.client.base.view.*;
import lsfusion.gwt.client.controller.remote.action.form.FormRequestAction;
import lsfusion.gwt.client.controller.remote.action.navigator.NavigatorRequestAction;
import lsfusion.gwt.client.view.MainFrame;
import net.customware.gwt.dispatch.shared.Action;

import java.util.*;

import static lsfusion.gwt.client.base.view.FormButton.ButtonStyle.SECONDARY;

public class GConnectionLostManager {
    private static final ClientMessages messages = ClientMessages.Instance.get();
    private static final AtomicLong failedRequests = new AtomicLong();
    private static final AtomicBoolean connectionLost = new AtomicBoolean(false);
    private static final AtomicBoolean authException = new AtomicBoolean(false);
    private static final HashMap<Action, List<NonFatalHandledException>> failedNotFatalHandledRequests = new LinkedHashMap<>();

    private static Timer timerWhenUnblocked;
    private static Timer timerWhenBlocked;

    private static GBlockDialog blockDialog;

    private static Widget popupOwnerWidget = ModalWindow.GLOBAL;

    public static void start() {
        connectionLost.set(false);

        timerWhenUnblocked = new Timer() {
            @Override
            public void run() {
                GExceptionManager.flushUnreportedThrowables(popupOwnerWidget);
                blockIfHasFailed();
            }
        };
        timerWhenUnblocked.scheduleRepeating(1000);

        timerWhenBlocked = new Timer() {
            @Override
            public void run() {
                if (blockDialog != null) {
                    blockDialog.setFatal(isConnectionLost(), isAuthException());
                    if (!shouldBeBlocked()) {
                        timerWhenBlocked.cancel();
                        blockDialog.hideDialog();
                        blockDialog = null;
                    }
                }
            }
        };
    }

    public static void connectionLost(boolean auth) {
        connectionLost.set(true);
        authException.set(auth);
    }

    public static boolean isConnectionLost() {
        return connectionLost.get();
    }

    public static boolean isAuthException() {
        return authException.get();
    }

    public static void blockIfHasFailed() {
        if (shouldBeBlocked() && blockDialog == null) {
            blockDialog = new GBlockDialog(false, true) {
                @Override
                public void onShow() {
                    super.onShow();

                    if (timerWhenBlocked != null) {
                        timerWhenBlocked.scheduleRepeating(1000);
                    }
                }
            };

            blockDialog.showDialog();
        }
    }

    public static void registerFailedRmiRequest() {
        failedRequests.incrementAndGet();
    }

    public static void unregisterFailedRmiRequest() {
        failedRequests.decrementAndGet();
        // we don't want to drop connection lost flag, since some requests were dropped (because of their failure), so if we continue working results will be unpredictable
        // plus succeeded request can be an accident (for example count < 20 check in LogClientActionHandler)
//        connectionLost(false, false);
    }

    private static boolean hasFailedRequest() {
        return failedRequests.get() > 0;
    }

    public static boolean shouldBeBlocked() {
        return hasFailedRequest() || isConnectionLost();
    }

    public static void invalidate() {
        connectionLost(false);

        failedRequests.set(0);

        if (timerWhenBlocked != null) {
            timerWhenBlocked.cancel();
            timerWhenBlocked = null;
        }

        if (timerWhenUnblocked != null) {
            timerWhenUnblocked.cancel();
            timerWhenUnblocked = null;
        }

        if (blockDialog != null) {
            blockDialog.hideDialog();
            blockDialog = null;
        }
    }

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
        NonFatalHandledException e = new NonFatalHandledException(GExceptionManager.copyMessage(t), thisStack, reqId);
        GExceptionManager.copyStackTraces(t, thisStack); // it seems that it is useless because only SerializableThrowable stacks are copied (see StackException)
        exceptions.add(e);
    }

    public static void flushFailedNotFatalRequests(Action action) {
        final List<NonFatalHandledException> flushExceptions = failedNotFatalHandledRequests.remove(action);
        if(flushExceptions != null) {
            Scheduler.get().scheduleDeferred(() -> {
                Map<Map, Collection<NonFatalHandledException>> group;
                group = GwtSharedUtils.group(new GwtSharedUtils.Group<Map, NonFatalHandledException>() {
                    public Map group(NonFatalHandledException key) {
                        return Collections.singletonMap(key.getMessage() + GExceptionManager.getStackTrace(key), key.reqId);
                    }
                }, flushExceptions);

                for (Map.Entry<Map, Collection<NonFatalHandledException>> entry : group.entrySet()) {
                    Collection<NonFatalHandledException> all = entry.getValue();
                    NonFatalHandledException nonFatal = all.iterator().next();
                    nonFatal.count = all.size();
                    GExceptionManager.logClientError(nonFatal, popupOwnerWidget);
                }
            });
        }
    }


    public static class GBlockDialog extends DialogModalWindow {

        public Timer showButtonsTimer;
        private final FormButton btnExit;
        private final FormButton btnReconnect;
        private final HTML message;
        private final DivWidget loading;
        private final DivWidget warning;
        private final DivWidget error;

        private boolean fatal;

        public GBlockDialog(boolean fatal, boolean showReconnect) {
            super(messages.rmiConnectionLost(), false, ModalWindowSize.FIT_CONTENT);

            this.fatal = fatal;

            ResizableComplexPanel content = new ResizableComplexPanel();
            content.setStyleName("dialog-block-content");

            loading = new DivWidget();
            loading.setStyleName("dialog-block-loading");
            content.add(loading);

            ResizableComplexPanel info = new ResizableComplexPanel();
            info.setStyleName("dialog-block-info");

            warning = new DivWidget();
            warning.setStyleName("dialog-block-warning");
            error = new DivWidget();
            error.setStyleName("dialog-block-error");

            if (fatal)
                warning.setVisible(false);
            if (!fatal)
                error.setVisible(false);

            message = new HTML(fatal ? messages.rmiConnectionLostFatal() : messages.rmiConnectionLostNonfatal());
            message.setStyleName("dialog-block-message");

            info.add(warning);
            info.add(error);
            info.add(message);
            content.add(info);

            setBodyWidget(content);

            btnExit = new FormButton(messages.rmiConnectionLostExit(), SECONDARY, clickEvent -> exitAction());
            btnExit.setEnabled(false);
            addFooterWidget(btnExit);

            btnReconnect = new FormButton(messages.rmiConnectionLostReconnect(), SECONDARY, clickEvent -> reconnectAction());
            btnReconnect.setEnabled(false);
            if (showReconnect) {
                addFooterWidget(btnReconnect);
            }
        }

        public void showDialog() {
            showButtonsTimer = new Timer() {
                @Override
                public void run() {
                    btnExit.setEnabled(true);
                    btnReconnect.setEnabled(true);
                }
            };
            showButtonsTimer.schedule(5000);

            show(popupOwnerWidget);
        }

        public void hideDialog() {
            if (showButtonsTimer != null)
                showButtonsTimer.cancel();
            hide();
        }

        private void exitAction() {
            GwtClientUtils.logout(true);
        }

        private void reconnectAction() {
            GwtClientUtils.reconnect();
        }

        public void setFatal(boolean fatal, boolean authException) {
            if (this.fatal != fatal) {
                if((MainFrame.devMode || MainFrame.autoReconnectOnConnectionLost) && fatal) {
                    GwtClientUtils.reconnect();
                } else {
                    message.setHTML(authException ? messages.rmiConnectionLostAuth() : (fatal ? messages.rmiConnectionLostFatal() : messages.rmiConnectionLostNonfatal()));
                    loading.setVisible(!fatal && !authException);
                    warning.setVisible(!fatal && !authException);
                    error.setVisible(fatal && !authException);
                    if (authException) {
                        btnExit.setEnabled(true);
                        btnReconnect.setEnabled(true);
                    }
                    this.fatal = fatal;
                }
            }
        }
    }
}