package lsfusion.gwt.client.base;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.user.client.Cookies;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.view.PopupDialogPanel;
import lsfusion.gwt.client.form.object.table.grid.user.toolbar.view.GToolbarButton;
import lsfusion.gwt.client.view.MainFrame;

import static com.google.gwt.dom.client.BrowserEvents.*;

public class TooltipManager {
    private static final TooltipManager instance = new TooltipManager();

    private static final int DELAY_SHOW = 1500;

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

                            VerticalPanel panel = new VerticalPanel();
                            panel.add(new FocusPanel(tooltipHtml));
                            addDebugLink(tooltipHelper, panel);

                            GwtClientUtils.showPopupInWindow(tooltip, panel, mouseX, mouseY);
                        }
                    } else {
                        if (tooltip != null)
                            hide();
                    }
                }
                return false;
            }, DELAY_SHOW);
        }
    }

    private void addDebugLink(TooltipHelper tooltipHelper, VerticalPanel panel) {
        if (!MainFrame.showDetailedInfo)
            return;

        HorizontalPanel horizontalPanel = new HorizontalPanel();
        horizontalPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
        if (Cookies.getCookie("debugPath") == null) {
            TextBox textBox = new TextBox();
            textBox.getElement().getStyle().setProperty("padding", "0px 3px");
            textBox.getElement().setPropertyString("placeholder", "enter path to lsfusion dir");

            Button button = new Button("OK");
            button.getElement().getStyle().setProperty("padding", "0px 3px");
            button.addClickHandler(event -> {
                if (!textBox.getText().trim().isEmpty())
                    Cookies.setCookie("debugPath", textBox.getText());

                hide();
            });

            horizontalPanel.add(textBox);
            horizontalPanel.add(button);
        } else {
            horizontalPanel.add(new HTML(getCommand(tooltipHelper)));
            horizontalPanel.add(new GToolbarButton("view_hide.png") {
                @Override
                public ClickHandler getClickHandler() {
                    return event -> {
                        Cookies.removeCookie("debugPath");
                        hide();
                    };
                }
            });
        }
        panel.add(horizontalPanel);
    }

    private void hide() {
        tooltip.hide();
        tooltip = null;
        tooltipHtml = null;
    }

    public void hideTooltip(final TooltipHelper tooltipHelper) {
        mouseIn = false;
        currentText = "";

        Scheduler.get().scheduleDeferred(() -> {
            if ((!(mouseIn && tooltipHelper != null && GwtClientUtils.nullEquals(tooltipHelper.getTooltip(), currentText)) && tooltip != null && !tooltip.tooltipFocused) ||
                    tooltip != null && !tooltip.tooltipFocused) {
                hide();
            }
        });
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

        public String getPath() {
            return null;
        }

        public String getCreationPath() {
            return null;
        }
    }

    public static String getCommand(TooltipHelper tooltipHelper) {
        String projectLSFDir = Cookies.getCookie("debugPath");
        String ideaExecPath = MainFrame.ideaExecPath;
        String creationPath = tooltipHelper.getCreationPath();
        String result = "";
        
        if (projectLSFDir != null && ideaExecPath != null && creationPath != null) {
            int line = Integer.parseInt(creationPath.substring(creationPath.lastIndexOf("(") + 1, creationPath.lastIndexOf(":")));
            //use "**" instead "="
            String command = "idea**" + ideaExecPath + "&--line**" + line + "&path**" + projectLSFDir + tooltipHelper.getPath();
            //replace spaces and slashes because this command going through url
            result = "<a href=\"lsfusion-protocol://" + command.replaceAll(" ", "++").replaceAll("\\\\", "/") +
                    "\" target=\"_blank\">" + ClientMessages.Instance.get().showInEditor() + "</a>";
        }
        return result;
    }

}
