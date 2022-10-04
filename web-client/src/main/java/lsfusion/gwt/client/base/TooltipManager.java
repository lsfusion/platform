package lsfusion.gwt.client.base;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.user.client.Cookies;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.*;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.view.PopupDialogPanel;
import lsfusion.gwt.client.form.object.table.grid.user.toolbar.view.GToolbarButton;
import lsfusion.gwt.client.view.MainFrame;

import java.util.Date;

import static com.google.gwt.dom.client.BrowserEvents.*;

public class TooltipManager {
    private static final TooltipManager instance = new TooltipManager();
    private static final ClientMessages messages = ClientMessages.Instance.get();

    private static final int DELAY_SHOW = 1500;

    private int mouseX;
    private int mouseY;

    private PopupDialogPanel tooltip;
    private HTML tooltipHtml;

    private boolean mouseIn;

    private String currentText = "";

    private boolean closeOnClick;
    public TooltipManager() {
        RootPanel.get().addDomHandler(ev -> {
            if (closeOnClick && tooltip != null && !tooltip.tooltipFocused) {
                hide();
            }
        }, MouseDownEvent.getType());
    }

    public static TooltipManager get() {
        return instance;
    }

    public static void registerWidget(Widget widget, final TooltipHelper tooltipHelper) {
        widget.addDomHandler(event -> get().showTooltip(event.getClientX(), event.getClientY(), tooltipHelper, false), MouseOverEvent.getType());
        widget.addDomHandler(event -> get().hideTooltip(tooltipHelper), MouseDownEvent.getType());
        widget.addDomHandler(event -> get().hideTooltip(null), MouseOutEvent.getType());
        widget.addDomHandler(event -> get().updateMousePosition(event.getClientX(), event.getClientY()), MouseMoveEvent.getType());
    }

    // the same as registerWidget, but should be used when we don't have Widget
    public static void checkTooltipEvent(NativeEvent event, TooltipHelper tooltipHelper) {
        String eventType = event.getType();
        if (MOUSEOVER.equals(eventType)) get().showTooltip(event.getClientX(), event.getClientY(), tooltipHelper, false);
        else if (MOUSEOUT.equals(eventType)) get().hideTooltip(tooltipHelper);
        else if (MOUSEDOWN.equals(eventType)) get().hideTooltip(null);
        else if (MOUSEMOVE.equals(eventType)) get().updateMousePosition(event.getClientX(), event.getClientY());
    }

    public void showTooltip(final int offsetX, final int offsetY, final TooltipHelper tooltipHelper, boolean closeOnClick) {
        final String tooltipText = tooltipHelper.getTooltip();
        mouseX = offsetX;
        mouseY = offsetY;
        currentText = tooltipText;
        this.closeOnClick = closeOnClick;

        if (tooltipText != null) {
            mouseIn = true;

            Scheduler.get().scheduleFixedDelay(() -> {
                if (mouseIn && tooltipText.equals(currentText)) {
                    if (tooltipHelper.stillShowTooltip()) {
                        if (tooltip != null) { // need this to avoid blinking when hiding / showing tooltip
                            GwtClientUtils.setPopupPosition(tooltip, mouseX, mouseY);
                        } else {
                            tooltip = new PopupDialogPanel() {
                                //if the previous focused element goes outside the visible area, the page will scroll to it when the tooltip is closed
                                @Override
                                public void setFocusedElement(Element focusedElement) {
                                    super.setFocusedElement(null);
                                }

                                @Override
                                protected void onAttach() {
                                    addDomHandler(ev -> tooltipFocused = true, MouseOverEvent.getType());

                                    addDomHandler(ev -> {
                                        tooltipFocused = false;
                                        hideTooltip(null);
                                    }, MouseOutEvent.getType());

                                    super.onAttach();
                                }
                            };

                            VerticalPanel panel = new VerticalPanel();
                            tooltipHtml = fillTooltipText(tooltipHelper, tooltipText, closeOnClick);
                            panel.add(new FocusPanel(tooltipHtml));

                            //to prevent the cursor hovering over the top left of the tooltip
                            //set the style of the popup to the internal element(panel) and make the popup transparent
                            // so that when you hover over it, the focus does not go to other components
                            tooltip.getElement().getStyle().setBackgroundColor("rgba(0, 0, 0, 0)");
                            tooltip.removeStyleName("gwt-PopupPanel");
                            panel.setStyleName("gwt-PopupPanel");
                            panel.getElement().getStyle().setMargin(10, Style.Unit.PX);

                            GwtClientUtils.showPopupInWindow(tooltip, panel, mouseX, mouseY);
                        }
                    } else {
                        if (tooltip != null)
                            hide();
                    }
                }
                return false;
            }, closeOnClick ? 0 : DELAY_SHOW);
        }
    }

    private HTML fillTooltipText(TooltipHelper tooltipHelper, String tooltipText, boolean closeOnClick) {
        HTML tooltipHtml = new HTML(tooltipText, false);

        if (!closeOnClick || MainFrame.showDetailedInfo) {
            String projectLSFDir = MainFrame.projectLSFDir;

            if (projectLSFDir != null) {
                setLinks(tooltipHelper, projectLSFDir, tooltipHtml);
            } else {
                VerticalPanel verticalPanel = new VerticalPanel();
                verticalPanel.setVisible(false);

                TextBox textBox = new TextBox();
                textBox.getElement().getStyle().setMarginLeft(5, Style.Unit.PX);
                textBox.getElement().getStyle().setProperty("padding", "0px 3px");
                textBox.getElement().setPropertyString("placeholder", messages.absolutePathToLsfusionDir());
                textBox.setText(Cookies.getCookie("debugPath"));

                HorizontalPanel userPathPanel = new HorizontalPanel();
                userPathPanel.add(new Label(messages.enterPath()));
                userPathPanel.add(textBox);
                verticalPanel.add(userPathPanel);
                verticalPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);

                Button button = new Button(messages.applyLabel());
                button.getElement().getStyle().setProperty("padding", "0px 3px");
                button.getElement().getStyle().setMarginTop(5, Style.Unit.PX);
                verticalPanel.add(button);
                DOM.sinkEvents(button.getElement(), Event.ONCLICK);
                DOM.setEventListener(button.getElement(), event -> {
                    if (DOM.eventGetType(event) == Event.ONCLICK) {
                        String textBoxText = textBox.getText();
                        if (!textBoxText.trim().isEmpty())
                            Cookies.setCookie("debugPath", textBoxText, new Date(System.currentTimeMillis() + 2592000000L)); //cookies expire after 30 days
                        else
                            Cookies.removeCookie("debugPath");

                        hide();
                    }
                });

                GToolbarButton preferencesButton = new GToolbarButton("userPreferences.png") {
                    @Override
                    public ClickHandler getClickHandler() {return event -> {};}
                };
                DOM.sinkEvents(preferencesButton.getElement(), Event.ONCLICK);
                DOM.setEventListener(preferencesButton.getElement(), event -> {
                    if (DOM.eventGetType(event) == Event.ONCLICK)
                        verticalPanel.setVisible(!verticalPanel.isVisible());
                });

                String debugPath = Cookies.getCookie("debugPath");
                setLinks(tooltipHelper, debugPath == null ? "use_default_path" : debugPath, tooltipHtml);

                tooltipHtml.getElement().appendChild(preferencesButton.getElement());
                tooltipHtml.getElement().appendChild(verticalPanel.getElement());
            }
        }

        return tooltipHtml;
    }

    private void setLinks(TooltipHelper tooltipHelper, String projectLSFDir, HTML tooltipHtml) {
        Element element = tooltipHtml.getElement();
        for (int i = 0; i < element.getChildCount(); i++) {
            Node child = element.getChild(i);
            if (child.getNodeName().equals("A")) {
                Element childElement = Element.as(child);
                String elementClass = childElement.getAttribute("class");
                if (elementClass.equals("lsf-tooltip-path"))
                    setLink(childElement, tooltipHelper, projectLSFDir, tooltipHelper.getCreationPath());
                else if (elementClass.equals("lsf-form-property-declaration"))
                    setLink(childElement, tooltipHelper, projectLSFDir, tooltipHelper.getFormPath());
                else if (elementClass.equals("lsf-tooltip-help") || (elementClass.equals("lsf-tooltip-form-decl-help") && tooltipHelper.getFormPath() != null))
                    childElement.setInnerHTML("(<a href=\"https://github.com/lsfusion/platform/issues/649\" target=\"_blank\"> ? </a>)&ensp;");
            }
        }
    }

    private void setLink(Element element, TooltipHelper tooltipHelper, String projectLSFDir, String path) {
        element.getPreviousSibling().setNodeValue(" ");
        element.setInnerHTML(getPathHTML(tooltipHelper, projectLSFDir, path));
    }

    private void hide() {
        tooltip.hide();
        tooltip = null;
        tooltipHtml = null;
    }

    public void hideTooltip(final TooltipHelper tooltipHelper) {
        if (!closeOnClick) {
            mouseIn = false;
            currentText = "";

            Scheduler.get().scheduleDeferred(() -> {
                if ((!(mouseIn && tooltipHelper != null && GwtClientUtils.nullEquals(tooltipHelper.getTooltip(), currentText)) && tooltip != null && !tooltip.tooltipFocused) ||
                        tooltip != null && !tooltip.tooltipFocused) {
                    hide();
                }
            });
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

        public String getPath() {
            return null;
        }

        public String getCreationPath() {
            return null;
        }

        public String getFormPath() {
            return null;
        }
    }

    private static String getPathHTML(TooltipHelper tooltipHelper, String projectLSFDir, String path) {
        String result = "";

        if (path != null) {
            //use "**" instead "="
            String command = "--line**" + Integer.parseInt(path.substring(path.lastIndexOf("(") + 1, path.lastIndexOf(":"))) +
                    "&path**" + projectLSFDir + tooltipHelper.getPath();
            //replace spaces and slashes because this command going through url
            result = "<a href=\"lsfusion-protocol://" + command.replaceAll(" ", "++").replaceAll("\\\\", "/") +
                    "\" target=\"_blank\">" + path + "</a>";
        }
        return result;
    }

}
