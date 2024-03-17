package lsfusion.gwt.client.base;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.user.client.Cookies;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.*;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.view.FormButton;
import lsfusion.gwt.client.form.object.table.grid.user.toolbar.view.GToolbarButton;
import lsfusion.gwt.client.view.MainFrame;

import java.util.Date;

public class TooltipManager {
    private static final ClientMessages messages = ClientMessages.Instance.get();

    public static JavaScriptObject initTooltip(Widget widget, final TooltipHelper tooltipHelper) {
        return initTooltip(widget, widget.getElement(), tooltipHelper);
    }
    public static JavaScriptObject initTooltip(Widget ownerWidget, Element element, final TooltipHelper tooltipHelper) {
        if (!MainFrame.mobile && tooltipHelper.getTooltip(null) != null && MainFrame.showDetailedInfoDelay > 0) {
            // assert that element is "new" and have no tippy (two mouseenter tippies will look odd, however manual tippy can be added)
            assert !GwtClientUtils.hasProperty(element, "_tippy");
            JavaScriptObject tippy = GwtClientUtils.initTippy(ownerWidget, element, MainFrame.showDetailedInfoDelay, "mouseenter", null);
            updateContent(tippy, tooltipHelper, null);
            return tippy;
        }
        return null;
    }

    public static void updateContent(JavaScriptObject tippy, final TooltipHelper tooltipHelper, String dynamicTooltip) {
        if(tippy != null) {
            GwtClientUtils.updateTippyContent(tippy, getTooltipContent(tooltipHelper, dynamicTooltip, tippy));
        }
    }

    private static Element getTooltipContent(TooltipHelper tooltipHelper, String dynamicTooltip, JavaScriptObject tippy) {
        String tooltip = tooltipHelper.getTooltip(dynamicTooltip);
        if(GwtSharedUtils.isRedundantString(tooltip)) {
            return null;
        }
        Element tooltipElement = EscapeUtils.toHTML(tooltip).getElement();

        if (MainFrame.showDetailedInfo) {
            String projectLSFDir = MainFrame.projectLSFDir;

            if (!projectLSFDir.isEmpty()) {
                setLinks(tooltipHelper, projectLSFDir, tooltipElement);
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

                FormButton button = new FormButton(messages.applyLabel());
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

                        GwtClientUtils.hideTippyPopup(tippy);
                    }
                });

                GToolbarButton preferencesButton = new GToolbarButton(StaticImage.USERPREFERENCES) {
                    @Override
                    public ClickHandler getClickHandler() {return event -> {};}
                };
                DOM.sinkEvents(preferencesButton.getElement(), Event.ONCLICK);
                DOM.setEventListener(preferencesButton.getElement(), event -> {
                    if (DOM.eventGetType(event) == Event.ONCLICK)
                        verticalPanel.setVisible(!verticalPanel.isVisible());
                });
                preferencesButton.addStyleName("tooltip-path-preferences-button");

                String debugPath = Cookies.getCookie("debugPath");
                setLinks(tooltipHelper, debugPath == null ? "use_default_path" : debugPath, tooltipElement);

                tooltipElement.appendChild(preferencesButton.getElement());
                tooltipElement.appendChild(verticalPanel.getElement());
            }
        }

        return tooltipElement;
    }

    private static void setLinks(TooltipHelper tooltipHelper, String projectLSFDir, Element tooltipElement) {
        for (int i = 0; i < tooltipElement.getChildCount(); i++) {
            Node child = tooltipElement.getChild(i);
            if (child.getNodeName().equals("A")) {
                Element childElement = Element.as(child);
                String elementClass = childElement.getAttribute("class");
                if (elementClass.equals("lsf-tooltip-path"))
                    setLink(childElement, projectLSFDir, tooltipHelper.getCreationPath(), tooltipHelper.getPath());
                else if (elementClass.equals("lsf-form-property-declaration"))
                    setLink(childElement, projectLSFDir, tooltipHelper.getFormDeclaration(), tooltipHelper.getFormRelativePath());
                else if ((elementClass.equals("lsf-tooltip-help") && tooltipHelper.getCreationPath() != null ) ||
                        (elementClass.equals("lsf-tooltip-form-decl-help") && tooltipHelper.getFormPath() != null))
                    fillLinkElement(childElement, "https://github.com/lsfusion/platform/issues/649", "_blank", " ? ");
            }
        }
    }

    private static void setLink(Element element, String projectLSFDir, String declaration, String relativePath) {
        element.getPreviousSibling().setNodeValue(" ");

        if (declaration != null) {
            //use "**" instead "="
            String command = "--line**" + Integer.parseInt(declaration.substring(declaration.lastIndexOf("(") + 1, declaration.lastIndexOf(":"))) +
                    "&path**" + projectLSFDir + relativePath;

            //replace spaces and slashes because this command going through url
            fillLinkElement(element, "lsfusion-protocol://" + command.replaceAll(" ", "++").replaceAll("\\\\", "/"),
                    "_blank", declaration);
        }
    }

    private static void fillLinkElement(Element element, String href, String target, String innerText) {
        element.setAttribute("href", href);
        element.setAttribute("target", target);
        element.setInnerText(innerText);
    }

    public static abstract class TooltipHelper {
        public abstract String getTooltip(String dynamicTooltip);

        public String getPath() {
            return null;
        }

        public String getCreationPath() {
            return null;
        }

        public String getFormPath() {
            return null;
        }

        public String getFormDeclaration() {
            String formPath = getFormPath();
            return formPath != null ? formPath.substring(formPath.lastIndexOf("/") + 1).replace(".lsf", "") : null;
        }

        public String getFormRelativePath() {
            String formPath = getFormPath();
            return formPath != null ? formPath.substring(0, formPath.indexOf(".lsf") + 4) : null;
        }
    }
}
