package lsfusion.gwt.client.base;

import com.google.gwt.core.client.JsArrayString;

public abstract class HtmlOrTextType {

    public JsArrayString getCssClasses() {
        String mainClass;
        String extraClass = null;

        boolean collapse = isCollapse();
        if(isWrap()) {
            mainClass = collapse ? "html-or-text-wrap-collapse" : "html-or-text-wrap";

            if(isWrapWordBreak())
                extraClass = "html-or-text-wrap-wordbreak";
        } else {
            mainClass = collapse ? "html-or-text-collapse" : "html-or-text";

            if(isEllipsis())
                extraClass = "html-or-text-ellipsis";
        }

        if(extraClass != null)
            return GwtClientUtils.toArray(mainClass, extraClass);

        return GwtClientUtils.toArray(mainClass);
    }

    protected boolean isEllipsis() {
        return false;
    }

    protected boolean isCollapse() {
        return false;
    }

    protected abstract boolean isWrap();

    protected boolean isWrapWordBreak() {
        return false;
    }
}