package lsfusion.gwt.client.base;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.*;
import lsfusion.gwt.client.base.view.PopupOwner;
import lsfusion.gwt.client.view.MainFrame;

import java.util.function.Supplier;

public class TooltipManager {
    public static JavaScriptObject initTooltip(Widget widget, final TooltipHelper tooltipHelper) {
        return initTooltip(new PopupOwner(widget), tooltipHelper, null);
    }

    public static JavaScriptObject initTooltip(PopupOwner popupOwner, final TooltipHelper tooltipHelper) {
        return initTooltip(popupOwner, tooltipHelper, null);
    }

    public static JavaScriptObject initTooltip(PopupOwner popupOwner, final TooltipHelper tooltipHelper, Supplier<Element> referenceElementSupplier) {
        if (!MainFrame.mobile && tooltipHelper.getTooltip(null) != null && MainFrame.showDetailedInfoDelay > 0) {
            // assert that element is "new" and have no tippy (two mouseenter tippies will look odd, however manual tippy can be added)
            assert !GwtClientUtils.hasProperty(popupOwner.element, "_tippy");
            JavaScriptObject tippy = GwtClientUtils.initTippy(popupOwner, MainFrame.showDetailedInfoDelay, "mouseenter",
                    null, null, referenceElementSupplier);
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
            setLinks(tooltipHelper, tooltipElement);
        }

        return tooltipElement;
    }

    private static void setLinks(TooltipHelper tooltipHelper, Element tooltipElement) {
        for (int i = 0; i < tooltipElement.getChildCount(); i++) {
            Node child = tooltipElement.getChild(i);
            if (child.getNodeName().equals("A")) {
                Element childElement = Element.as(child);
                String elementClass = childElement.getAttribute("class");
                if (elementClass.equals("lsf-tooltip-path"))
                    setLink(childElement, tooltipHelper.getCreationPath(), tooltipHelper.getPath());
                else if (elementClass.equals("lsf-form-property-declaration"))
                    setLink(childElement, tooltipHelper.getFormDeclaration(), tooltipHelper.getFormRelativePath());
                else if ((elementClass.equals("lsf-tooltip-help") && tooltipHelper.getCreationPath() != null ) ||
                        (elementClass.equals("lsf-tooltip-form-decl-help") && tooltipHelper.getFormPath() != null))
                    fillLinkElement(childElement, "https://github.com/lsfusion/platform/issues/649", "_blank", " ? ");
            }
        }
    }

    // declaration is "Module(line:column)" (1-based, column may carry a trailing meta marker), relativePath is the
    // module file relative to the source root
    private static void setLink(Element element, String declaration, String relativePath) {
        element.getPreviousSibling().setNodeValue(" ");

        if (declaration != null) {
            String position = declaration.substring(declaration.lastIndexOf("(") + 1, declaration.lastIndexOf(")"));
            int line = Integer.parseInt(position.substring(0, position.indexOf(":")));
            int column = Integer.parseInt(position.substring(position.indexOf(":") + 1).replaceAll("[^0-9]", ""));

            fillLinkElement(element, "#", null, declaration);
            DOM.sinkEvents(element, Event.ONCLICK);
            DOM.setEventListener(element, event -> {
                if (DOM.eventGetType(event) == Event.ONCLICK) {
                    event.preventDefault();
                    openInIDE(relativePath, line, column);
                }
            });
        }
    }

    // The lsFusion IDEA plugin serves /api/lsfusion-open on the IDE's built-in web server, which takes the first free
    // port from 63342 up; the ports are tried in turn until an IDE answers 200 (a 404 is another IDE without the plugin,
    // anything else some unrelated service). In turn, not at once: two IDEs with the plugin would both open the file.
    // A request may legitimately hang for a while: the IDE asks the user whether to trust this host before answering
    // the first one, hence the long timeout; a stalled unrelated service on one of these JetBrains ports would cost
    // a minute.
    private static native void openInIDE(String path, int line, int column) /*-{
        var query = 'path=' + encodeURIComponent(path) + '&line=' + line + '&column=' + column;
        var tryPort = function (port) {
            if (port > 63352)
                return;
            var controller = $wnd.AbortController ? new $wnd.AbortController() : null;
            var timeout = controller ? $wnd.setTimeout(function () { controller.abort(); }, 60000) : null;
            var next = function () { tryPort(port + 1); };
            $wnd.fetch('http://localhost:' + port + '/api/lsfusion-open?' + query, {cache: 'no-store', signal: controller ? controller.signal : undefined})
                .then(function (response) { if (response.status !== 200) next(); }, next)
                .then(function () { if (timeout !== null) $wnd.clearTimeout(timeout); });
        };
        tryPort(63342);
    }-*/;

    private static void fillLinkElement(Element element, String href, String target, String innerText) {
        element.setAttribute("href", href);
        if (target != null)
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
