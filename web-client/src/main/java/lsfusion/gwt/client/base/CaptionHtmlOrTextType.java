package lsfusion.gwt.client.base;

public class CaptionHtmlOrTextType extends HtmlOrTextType {
    public static final CaptionHtmlOrTextType MESSAGE = new CaptionHtmlOrTextType() {
        public int getWrap() {
            return -1;
        }
        protected boolean isWrapWordBreak() {
            return true;
        }
    };
    // properties
    public static final CaptionHtmlOrTextType COMMENT_VERT = new CaptionHtmlOrTextType() {
        public int getWrap() {
            return -1;
        }
    };
    public static final CaptionHtmlOrTextType COMMENT_HORZ = new CaptionHtmlOrTextType();
    public static final CaptionHtmlOrTextType ASYNCVALUES = new CaptionHtmlOrTextType();
}
