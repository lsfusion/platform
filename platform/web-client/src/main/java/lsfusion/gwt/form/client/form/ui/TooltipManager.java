package lsfusion.gwt.form.client.form.ui;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.DecoratedPopupPanel;
import com.google.gwt.user.client.ui.HTML;
import lsfusion.gwt.base.shared.GwtSharedUtils;

public class TooltipManager {
    private static final TooltipManager instance = new TooltipManager();

    private static final int DELAY = 300;

    private int mouseX;
    private int mouseY;

    private Tooltip tooltip;

    private boolean mouseIn;

    private String currentText = "";

    public static TooltipManager get() {
        return instance;
    }

    public void showTooltip(final int offsetX, final int offsetY, final String tooltipText) {
        mouseX = offsetX;
        mouseY = offsetY;
        currentText = tooltipText;
        mouseIn = true;

        Scheduler.get().scheduleFixedDelay(new Scheduler.RepeatingCommand() {
            @Override
            public boolean execute() {
                if (mouseIn && !GwtSharedUtils.isRedundantString(tooltipText) && tooltipText.equals(currentText)) {
                    if (tooltip != null) {
                        tooltip.hide();
                    }
                    tooltip = new Tooltip(tooltipText);
                    tooltip.setPopupPosition(mouseX + 1, mouseY + 1);   // минимальный сдвиг специально для Firefox, где
                                                                        // PopupPanel забирает себе MOUSEOVER event
                    tooltip.show();

                    int tooltipWidth = tooltip.getOffsetWidth();
                    int tooltipHeight = tooltip.getOffsetHeight();
                    int tooltipXCorrection = tooltipWidth - (Window.getClientWidth() - mouseX);
                    int tooltipYCorrection = tooltipHeight - (Window.getClientHeight() - mouseY);

                    if (tooltipXCorrection > 0 || tooltipYCorrection > 0) {
                        if (tooltipXCorrection > 0 && tooltipYCorrection > 0) {
                            // по этой же причине при недостатке места с обеих сторон вместо сдвига на нужное расстояние
                            // показываем тултип по другую сторону от курсора. иначе в правом нижнем углу в Firefox тултип не увидим вообще
                            tooltip.setPopupPosition(mouseX - tooltipWidth, mouseY - tooltipHeight);
                        } else {
                            tooltip.setPopupPosition(
                                    tooltipXCorrection > 0 ? Math.max(mouseX - tooltipXCorrection, 0) : mouseX + 1,
                                    tooltipYCorrection > 0 ? Math.max(mouseY - tooltipYCorrection, 0) : mouseY + 1
                            );
                        }
                    }
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

    public void updateMousePosition(int x, int y) {
        if (mouseIn) {
            mouseX = x;
            mouseY = y;
        }
    }

    class Tooltip extends DecoratedPopupPanel {
        public Tooltip(String contents) {
            super(true);
            setWidget(new HTML(contents, false));
        }
    }
}
