package lsfusion.gwt.client.base;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;

public abstract class HtmlOrTextType {

    private static native void setFixedLines(Element element, int lines) /*-{
        element.style.setProperty("--fixed-lines", lines);
    }-*/;

    private static native void clearFixedLines(Element element) /*-{
        element.style.removeProperty("--fixed-lines");
    }-*/;

    private void render(Element element, boolean set) {
        boolean collapse = isCollapse();
        int wrap = getWrap();
        if(wrap != 1) {
            if (collapse) {
                if(set)
                    element.addClassName("html-or-text-wrap-collapse");
                else
                    element.removeClassName("html-or-text-wrap-collapse");
            } else {
                if(set)
                    element.addClassName("html-or-text-wrap");
                else
                    element.removeClassName("html-or-text-wrap");
            }

            if(wrap >= 0) {
                if(set) {
                    setFixedLines(element, wrap);
                    element.addClassName("html-or-text-wrap-fixed");
                } else {
                    clearFixedLines(element);
                    element.removeClassName("html-or-text-wrap-fixed");
                }
            }

            if(isWrapWordBreak()) {
                if(set)
                    element.addClassName("html-or-text-wrap-wordbreak");
                else
                    element.removeClassName("html-or-text-wrap-wordbreak");
            }
        } else {
            if (collapse) {
                if(set)
                    element.addClassName("html-or-text-collapse");
                else
                    element.removeClassName("html-or-text-collapse");
            } else {
                if(set)
                    element.addClassName("html-or-text");
                else
                    element.removeClassName("html-or-text");
            }
        }

        if(isEllipsis()) {
            if(set)
                element.addClassName("html-or-text-ellipsis");
            else
                element.removeClassName("html-or-text-ellipsis");
        }
    }
    public native JavaScriptObject getRenderer() /*-{
        var thisObj = this;
        return function(element, set) {
                thisObj.@HtmlOrTextType::render(*)(element, set);
            };
    }-*/;

    protected boolean isEllipsis() {
        return false;
    }

    protected boolean isCollapse() {
        return false;
    }

    public int getWrap() {
        return 1;
    }

    protected boolean isWrapWordBreak() {
        return false;
    }
}