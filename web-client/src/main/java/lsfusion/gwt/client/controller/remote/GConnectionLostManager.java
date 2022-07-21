package lsfusion.gwt.client.controller.remote;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.*;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.AtomicBoolean;
import lsfusion.gwt.client.base.AtomicLong;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.exception.GExceptionManager;
import lsfusion.gwt.client.base.view.DialogModalWindow;
import lsfusion.gwt.client.base.view.DivWidget;
import lsfusion.gwt.client.base.view.ResizableComplexPanel;
import lsfusion.gwt.client.view.MainFrame;

public class GConnectionLostManager {
    private static final ClientMessages messages = ClientMessages.Instance.get();
    private static final AtomicLong failedRequests = new AtomicLong();
    private static final AtomicBoolean connectionLost = new AtomicBoolean(false);
    private static final AtomicBoolean authException = new AtomicBoolean(false);

    private static Timer timerWhenUnblocked;
    private static Timer timerWhenBlocked;

    private static GBlockDialog blockDialog;

    public static void start() {
        connectionLost.set(false);

        timerWhenUnblocked = new Timer() {
            @Override
            public void run() {
                GExceptionManager.flushUnreportedThrowables();
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


    public static class GBlockDialog extends DialogModalWindow {

        public Timer showButtonsTimer;
        private final Button btnExit;
        private final Button btnReconnect;
        private final HTML message;
        private final DivWidget loading;
        private final DivWidget warning;
        private final DivWidget error;

        private boolean fatal;

        public GBlockDialog(boolean fatal, boolean showReconnect) {
            super(false, true);

            this.fatal = fatal;

            setCaption(messages.rmiConnectionLost());

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

            btnExit = new Button(messages.rmiConnectionLostExit());
            btnExit.setStyleName("btn");
            btnExit.addStyleName("btn-secondary");
            btnExit.setEnabled(false);
            btnExit.addClickHandler(clickEvent -> exitAction());
            addFooterWidget(btnExit);

            btnReconnect = new Button(messages.rmiConnectionLostReconnect());
            btnReconnect.setStyleName("btn");
            btnReconnect.addStyleName("btn-secondary");
            btnReconnect.setEnabled(false);
            btnReconnect.addClickHandler(clickEvent -> reconnectAction());
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

            show();
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
                if(MainFrame.devMode && fatal) {
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