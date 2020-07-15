package lsfusion.gwt.client.base;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.view.PopupDialogPanel;

import static lsfusion.gwt.client.base.GwtSharedUtils.isRedundantString;

public class TooltipManager {
    private static final TooltipManager instance = new TooltipManager();

    private static final int DELAY = 1500;

    private int mouseX;
    private int mouseY;

    private Tooltip tooltip;

    private boolean mouseIn;

    private String currentText = "";

    public static TooltipManager get() {
        return instance;
    }
    
    public static void registerWidget(Widget widget, final TooltipHelper tooltipHelper) {
        widget.addDomHandler(new MouseOverHandler() {
            @Override
            public void onMouseOver(MouseOverEvent mouseOverEvent) {
                get().showTooltip(mouseOverEvent.getClientX(), mouseOverEvent.getClientY(), tooltipHelper);
            }
        }, MouseOverEvent.getType());

        widget.addDomHandler(new MouseDownHandler() {
            @Override
            public void onMouseDown(MouseDownEvent event) {
                get().hideTooltip();
            }
        }, MouseDownEvent.getType());

        widget.addDomHandler(new MouseOutHandler() {
            @Override
            public void onMouseOut(MouseOutEvent mouseOutEvent) {
                get().hideTooltip();
            }
        }, MouseOutEvent.getType());

        widget.addDomHandler(new MouseMoveHandler() {
            @Override
            public void onMouseMove(MouseMoveEvent event) {
                get().updateMousePosition(event.getClientX(), event.getClientY());
            }
        }, MouseMoveEvent.getType());
    } 

    public void showTooltip(final int offsetX, final int offsetY, final TooltipHelper tooltipHelper) {
        final String tooltipText = tooltipHelper.getTooltip();
        
        mouseX = offsetX;
        mouseY = offsetY;
        currentText = tooltipText;
        mouseIn = true;

        Scheduler.get().scheduleFixedDelay(new Scheduler.RepeatingCommand() {
            @Override
            public boolean execute() {
                                                                                                        // если currentText поменялся, значит либо он сбросился,
                                                                                                        // либо заново вызвался showTooltip()
                if (mouseIn && (tooltipHelper.stillShowTooltip()) && !isRedundantString(tooltipText) && tooltipText.equals(currentText)) {
                    if (tooltip != null) {
                        tooltip.hide();
                    }
                    tooltip = new Tooltip(tooltipText);
                    GwtClientUtils.showPopupInWindow(tooltip, mouseX, mouseY);
                }
                return false;
            }
        }, DELAY);
    }

    public void hideTooltip() {
        if (tooltip != null) {
            tooltip.hide();
            tooltip = null;
        }
        mouseIn = false;
        currentText = "";
    }

    // за время ожидания курсор может переместиться далеко от места, где вызвался showTooltip()
    public void updateMousePosition(int x, int y) {
        if (mouseIn) {
            mouseX = x;
            mouseY = y;
        }
    }

    class Tooltip extends PopupDialogPanel {
        public Tooltip(String contents) {
            super();
            setWidget(new HTML(contents, false));
        }
    }
    
    public static abstract class TooltipHelper {
        public abstract String getTooltip();

        // to check if nothing changed after tooltip delay
        public abstract boolean stillShowTooltip();
    }
}
