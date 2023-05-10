package lsfusion.gwt.client.base.view;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Widget;

public class RecentlyEventClassHandler {

    private final Widget widget;
    private final String cssClass;

    private final boolean propagate;
    private final int timeout;

    public RecentlyEventClassHandler(Widget widget, boolean propagate, String cssClass, int timeout) {
        this.widget = widget;
        this.propagate = propagate;
        this.cssClass = cssClass;
        this.timeout = timeout;
    }


    private final Timer timer = new Timer() {
        @Override
        public void run() {
            updateCss(false);
        }
    };

    public void onEvent() {
        updateCss(true);
        if (timer.isRunning())
            timer.cancel();

        timer.schedule(timeout);
    }

    private void updateCss(boolean add) {
        Widget widget = this.widget;
        do {
            widget.setStyleName(cssClass, add);
        } while (propagate && (widget = widget.getParent()) != null);
    }
}
