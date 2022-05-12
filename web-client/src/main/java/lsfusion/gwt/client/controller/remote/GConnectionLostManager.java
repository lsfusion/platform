package lsfusion.gwt.client.controller.remote;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.OpenEvent;
import com.google.gwt.event.logical.shared.OpenHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.*;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.AtomicBoolean;
import lsfusion.gwt.client.base.AtomicLong;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.exception.GExceptionManager;
import lsfusion.gwt.client.base.view.WindowBox;
import lsfusion.gwt.client.view.MainFrame;

public class GConnectionLostManager {
    private static final ClientMessages messages = ClientMessages.Instance.get();
    private static AtomicLong failedRequests = new AtomicLong();
    private static AtomicBoolean connectionLost = new AtomicBoolean(false);
    private static AtomicBoolean authException = new AtomicBoolean(false);

    //static/images/loading_bar.gif
    private static final String loadingBarImage = "data:image/gif;base64,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";

    //static/images/warning.png
    private static final String warningImage = "data:image/png;base64,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";

    //static/images/error.png
    private static final String errorImage = "data:image/png;base64,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";

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
            blockDialog = new GBlockDialog(false, true);
            blockDialog.addOpenHandler(new OpenHandler<WindowBox>() {
                @Override
                public void onOpen(OpenEvent<WindowBox> event) {
                    if (timerWhenBlocked != null) {
                        timerWhenBlocked.scheduleRepeating(1000);
                    }
                }
            });
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


    public static class GBlockDialog extends WindowBox {

        private int attempt;
        public Timer showButtonsTimer;
        private Button btnExit;
        private Button btnReconnect;
        private HTML lbMessage;
        private VerticalPanel loadingPanel;
        private VerticalPanel warningPanel;
        private VerticalPanel errorPanel;

        private boolean fatal;

        public GBlockDialog(boolean fatal, boolean showReconnect) {
            super(false, false, false);
            this.attempt = 0;
            setGlassEnabled(true);
            this.fatal = fatal;
            lbMessage = new HTML(fatal ? messages.rmiConnectionLostFatal() : messages.rmiConnectionLostNonfatal());

            HorizontalPanel buttonPanel = new HorizontalPanel();
            buttonPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
            buttonPanel.setSpacing(5);

            btnExit = new Button(messages.rmiConnectionLostExit());
            btnExit.setEnabled(false);
            btnExit.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent clickEvent) {
                    exitAction();
                }
            });
            buttonPanel.add(btnExit);

            btnReconnect = new Button(messages.rmiConnectionLostReconnect());
            btnReconnect.setEnabled(false);
            btnReconnect.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent clickEvent) {
                    reconnectAction();
                }
            });
            if (showReconnect) {
                buttonPanel.add(btnReconnect);
            }

            setModal(true);

            setText(messages.rmiConnectionLost());

            loadingPanel = new VerticalPanel();
            loadingPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
            loadingPanel.add(new Image(loadingBarImage));

            warningPanel = new VerticalPanel();
            warningPanel.setSpacing(10);
            warningPanel.add(new Image(warningImage));
            if (fatal)
                warningPanel.setVisible(false);
            errorPanel = new VerticalPanel();
            errorPanel.setSpacing(10);
            errorPanel.add(new Image(errorImage));
            if (!fatal)
                errorPanel.setVisible(false);

            HorizontalPanel centralPanel = new HorizontalPanel();
            centralPanel.add(warningPanel);
            centralPanel.add(errorPanel);
            centralPanel.add(lbMessage);

            VerticalPanel mainPanel = new VerticalPanel();
            mainPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
            if (!fatal)
                mainPanel.add(loadingPanel);
            mainPanel.add(centralPanel);
            mainPanel.add(buttonPanel);

            setWidget(mainPanel);
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

            if (!isShowing())
                center();
            makeMaskVisible(true);
        }

        public void hideDialog() {
            if (showButtonsTimer != null)
                showButtonsTimer.cancel();
            if(isShowing())
                hide();
            blockDialog.makeMaskVisible(false);
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
                    lbMessage.setHTML(authException ? messages.rmiConnectionLostAuth() : (fatal ? messages.rmiConnectionLostFatal() : messages.rmiConnectionLostNonfatal()));
                    loadingPanel.setVisible(!fatal && !authException);
                    warningPanel.setVisible(!fatal && !authException);
                    errorPanel.setVisible(fatal && !authException);
                    if (authException) {
                        btnExit.setEnabled(true);
                        btnReconnect.setEnabled(true);
                    }
                    this.fatal = fatal;
                    center();
                }
            }
        }

        public void makeMaskVisible(boolean visible) {
            getElement().getStyle().setOpacity(visible ? 1 : 0);
            getGlassElement().getStyle().setOpacity(visible ? 0.3 : 0);
        }
    }
}