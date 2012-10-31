package platform.gwt.form.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.RootPanel;

public class LoadingBlocker {
    public static final int GLASS_SCREEN_SHOW_DELAY = 500;

    private static class InstanceHolder {
        private static final LoadingBlocker instance = new LoadingBlocker();
    }

    public static LoadingBlocker getInstance() {
        return InstanceHolder.instance;
    }

    private final BlockerPopup popup;
    private final Timer timer;
    private boolean visible;

    private LoadingBlocker() {
        popup = new BlockerPopup();
        timer = new Timer() {
            @Override
            public void run() {
                popup.makeMaskVisible(true);
            }
        };
    }

    public void start() {
        if (!visible) {
            popup.makeMaskVisible(false);
            popup.center();
            timer.schedule(GLASS_SCREEN_SHOW_DELAY);

            showWaitCursor();
            visible = true;
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
        public BlockerPopup() {
            setGlassEnabled(true);
            setWidget(new Image(GWT.getModuleBaseURL() + "images/loading_bar.gif"));
        }

        public void makeMaskVisible(boolean visible) {
            getElement().getStyle().setOpacity(visible ? 1 : 0);
            getGlassElement().getStyle().setOpacity(visible ? 0.3 : 0);
        }
    }
}
