package lsfusion.gwt.client.base;

public class CaptionHtmlOrTextType extends HtmlOrTextType {
    public static final CaptionHtmlOrTextType MESSAGE = new CaptionHtmlOrTextType() {
        public boolean isWrap() {
            return true;
        }
        protected boolean isWrapWordBreak() {
            return true;
        }
    };
    // properties
    public static final CaptionHtmlOrTextType COMMENT_VERT = new CaptionHtmlOrTextType() {
        public boolean isWrap() {
            return true;
        }
    };
    public static final CaptionHtmlOrTextType COMMENT_HORZ = new CaptionHtmlOrTextType();
    public static final CaptionHtmlOrTextType ASYNCVALUES = new CaptionHtmlOrTextType();
}
