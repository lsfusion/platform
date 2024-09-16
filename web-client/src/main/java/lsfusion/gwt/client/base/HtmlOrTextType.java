package lsfusion.gwt.client.base;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.form.design.GFontMetrics;
import lsfusion.gwt.client.view.MainFrame;

public abstract class HtmlOrTextType {

    private static native void setFixedLines(Element element, int lines) /*-{
        element.style.setProperty("--fixed-lines", lines);
    }-*/;
    private static native void setFixedLinesHeight(Element element, String height) /*-{
        element.style.setProperty("--fixed-lines", height);
    }-*/;

    private static native void clearFixedLines(Element element) /*-{
        element.style.removeProperty("--fixed-lines");
    }-*/;

    private void render(Element element, boolean set) {
        boolean collapse = isCollapse();
        boolean wrap = isWrap();
        if(wrap) {
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

            int wrapLines = getWrapLines();
            if(wrapLines >= 0) {
                if(set) {
                    if(MainFrame.firefox)
                        setFixedLinesHeight(element, GFontMetrics.getStringHeight(getWrapLinesFont(), wrapLines).getString());
                    else
                        setFixedLines(element, wrapLines);

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

    public boolean isWrap() {
        return false;
    }

    public int getWrapLines() {
        return -1;
    }

    public GFont getWrapLinesFont() { // need only for firefox hack, when wrapLines > 0
        throw new UnsupportedOperationException();
    }

    protected boolean isWrapWordBreak() {
        return false;
    }
}