package lsfusion.gwt.client.base;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.view.PopupDialogPanel;

import static com.google.gwt.dom.client.BrowserEvents.*;
import static lsfusion.gwt.client.base.GwtSharedUtils.isRedundantString;

public class TooltipManager {
    private static final TooltipManager instance = new TooltipManager();

    private static final int DELAY_SHOW = 1500;
    private static final int DELAY_HIDE = 100;

    private int mouseX;
    private int mouseY;

    private PopupDialogPanel tooltip;
    private HTML tooltipHtml;

    private boolean mouseIn;

    private String currentText = "";

    public static TooltipManager get() {
        return instance;
    }
    
    public static void registerWidget(Widget widget, final TooltipHelper tooltipHelper) {
        widget.addDomHandler(event -> get().showTooltip(event.getClientX(), event.getClientY(), tooltipHelper), MouseOverEvent.getType());
        widget.addDomHandler(event -> get().hideTooltip(tooltipHelper), MouseDownEvent.getType());
        widget.addDomHandler(event -> get().hideTooltip(null), MouseOutEvent.getType());
        widget.addDomHandler(event -> get().updateMousePosition(event.getClientX(), event.getClientY()), MouseMoveEvent.getType());
    }

    // the same as registerWidget, but should be used when we don't have Widget
    public static void checkTooltipEvent(NativeEvent event, TooltipHelper tooltipHelper) {
        String eventType = event.getType();
        if (MOUSEOVER.equals(eventType)) get().showTooltip(event.getClientX(), event.getClientY(), tooltipHelper);
        else if (MOUSEOUT.equals(eventType)) get().hideTooltip(tooltipHelper);
        else if (MOUSEDOWN.equals(eventType)) get().hideTooltip(null);
        else if (MOUSEMOVE.equals(eventType)) get().updateMousePosition(event.getClientX(), event.getClientY());
    }

    public void showTooltip(final int offsetX, final int offsetY, final TooltipHelper tooltipHelper) {
        final String tooltipText = tooltipHelper.getTooltip();
        mouseX = offsetX;
        mouseY = offsetY;
        currentText = tooltipText;

        if(tooltipText != null) {
            mouseIn = true;

            Scheduler.get().scheduleFixedDelay(() -> {
                if (mouseIn && tooltipText.equals(currentText)) {
                    if(tooltipHelper.stillShowTooltip()) {
                        if(tooltip != null) { // need this to avoid blinking when hiding / showing tooltip
                            tooltipHtml.setHTML(tooltipText);
                            GwtClientUtils.setPopupPosition(tooltip, mouseX, mouseY);
                        } else {
                            tooltip = new PopupDialogPanel();
                            tooltipHtml = new HTML(tooltipText, false);
                            GwtClientUtils.showPopupInWindow(tooltip, new FocusPanel(tooltipHtml), mouseX, mouseY);
                        }
                    } else {
                        if (tooltip != null) {
                            tooltip.hide();
                            tooltip = null;
                            tooltipHtml = null;
                        }
                    }
                }
                return false;
            }, DELAY_SHOW);
        }
    }

    public void hideTooltip(final TooltipHelper tooltipHelper) {
        mouseIn = false;
        currentText = "";

        if(tooltipHelper != null) {
            String tooltipText = tooltipHelper.getTooltip();

            // we want to delay tooltip hiding to check if the next tooltip showing is the same (this way we'll avoid blinking)
            Scheduler.get().scheduleFixedDelay(() -> {
                if (!(mouseIn && GwtClientUtils.nullEquals(tooltipText, currentText))) {
                    if (tooltip != null) {
                        tooltip.hide();
                        tooltip = null;
                        tooltipHtml = null;
                    }
                }
                return false;
            }, DELAY_HIDE);
        } else {
            if (tooltip != null) {
                tooltip.hide();
                tooltip = null;
                tooltipHtml = null;
            }
        }
    }

    // за время ожидания курсор может переместиться далеко от места, где вызвался showTooltip()
    public void updateMousePosition(int x, int y) {
        if (mouseIn) {
            mouseX = x;
            mouseY = y;
        }
    }

    public static abstract class TooltipHelper {
        public abstract String getTooltip();

        // to check if nothing changed after tooltip delay
        public abstract boolean stillShowTooltip();
    }
}
