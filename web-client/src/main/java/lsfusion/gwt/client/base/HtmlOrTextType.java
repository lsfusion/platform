package lsfusion.gwt.client.base;

public abstract class HtmlOrTextType {

    public String getCssClass() {
        boolean collapse = isCollapse();
        boolean wrap = isWrap();
        boolean wrapWordBreak = isWrapWordBreak();
        if(wrap) {
            if(collapse) {
                assert !wrapWordBreak;
                return "html-or-text-wrap-collapse";
            }

            if(wrapWordBreak)
                return "html-or-text-wrap-wordbreak";

            return "html-or-text-wrap";
        } else {
            assert !wrapWordBreak;
            if(collapse)
                return "html-or-text-collapse";

            return "html-or-text";
        }
    }

    protected boolean isCollapse() {
        return false;
    }

    protected abstract boolean isWrap();

    protected boolean isWrapWordBreak() {
        return false;
    }
}