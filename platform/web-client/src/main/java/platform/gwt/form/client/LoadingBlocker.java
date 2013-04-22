package platform.gwt.form.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import net.customware.gwt.dispatch.shared.general.StringResult;
import platform.gwt.base.client.ErrorHandlingCallback;
import platform.gwt.base.client.EscapeUtils;
import platform.gwt.base.shared.GwtSharedUtils;
import platform.gwt.form.client.form.ServerMessageProvider;

public class LoadingBlocker {
    public static final int GLASS_SCREEN_SHOW_DELAY = 500;
    public static final int MESSAGE_UPDATE_PERIOD = 1000;

    private final BlockerPopup popup;
    private final Timer timer;
    private boolean visible;

    private ServerMessageProvider messageProvider;

    public LoadingBlocker(ServerMessageProvider messageProvider) {
        this.messageProvider = messageProvider;

        popup = new BlockerPopup();
        timer = new Timer() {
            @Override
            public void run() {
                popup.makeMaskVisible(true);
            }
        };
    }

    public boolean isVisible() {
        return visible;
    }

    public void start() {
        if (!visible) {
            popup.makeMaskVisible(false);
            popup.center();
            timer.schedule(GLASS_SCREEN_SHOW_DELAY);

            Scheduler.get().scheduleFixedPeriod(new Scheduler.RepeatingCommand() {
                @Override
                public boolean execute() {
                    if (visible) {
                        messageProvider.getServerActionMessage(new ErrorHandlingCallback<StringResult>() {
                            @Override
                            public void success(StringResult result) {
                                updateMessage(result.get());
                            }
                        });
                        return true;
                    } else {
                        return false;
                    }
                }
            }, MESSAGE_UPDATE_PERIOD);

            showWaitCursor();
            visible = true;
        }
    }

    private void updateMessage(String message) {
        if (visible) {
            popup.updateMessage(message);
        }
    }

    public void stop() {
        if (visible) {
            popup.hide();
            timer.cancel();

            showDefaultCursor();
            visible = false;
        }
    }

    public static void showWaitCursor() {
        DOM.setStyleAttribute(RootPanel.getBodyElement(), "cursor", "wait");
    }

    public static void showDefaultCursor() {
        DOM.setStyleAttribute(RootPanel.getBodyElement(), "cursor", "default");
    }

    public static class BlockerPopup extends PopupPanel {
        private SimplePanel messagePanel;
        private Label messageLabel;

        private int latestWindowHeight;
        private int latestWindowWidth;

        public BlockerPopup() {
            setModal(true);
            setGlassEnabled(true);

            VerticalPanel vp = new VerticalPanel();
            vp.add(new Image(GWT.getModuleBaseURL() + "images/loading_bar.gif"));

            messageLabel = new Label();
            messageLabel.addStyleName("messageLabel");

            messagePanel = new SimplePanel(messageLabel);
            messagePanel.setVisible(false);
            messagePanel.addStyleName("messageCanvas");

            vp.add(messagePanel);

            setWidget(vp);
        }

        public void makeMaskVisible(boolean visible) {
            getElement().getStyle().setOpacity(visible ? 1 : 0);
            getGlassElement().getStyle().setOpacity(visible ? 0.3 : 0);
        }

        public void updateMessage(String message) {
            String newMessage = EscapeUtils.unicodeEscape(message);
            if (!GwtSharedUtils.nullEquals(newMessage, messageLabel.getText())) {
                messageLabel.setText(EscapeUtils.unicodeEscape(message));
            }

            boolean showMessage = message != null && !"".equals(message.trim());
            if (windowResized()) {
                latestWindowWidth = Window.getClientWidth();
                latestWindowHeight = Window.getClientHeight();
                center();
                int panelWidth = (int) (latestWindowWidth * 0.85);
                int popupWidth = getOffsetWidth();
                messagePanel.setWidth(panelWidth + "px");
                messagePanel.getElement().getStyle().setLeft(- panelWidth / 2 + popupWidth / 2, Style.Unit.PX);
                messagePanel.getElement().getStyle().setProperty("maxHeight", latestWindowHeight / 2 - 100, Style.Unit.PX);
            }
            messagePanel.setVisible(showMessage);
        }

        private boolean windowResized() {
            return latestWindowHeight != Window.getClientHeight() || latestWindowWidth != Window.getClientWidth();
        }

    }
}
