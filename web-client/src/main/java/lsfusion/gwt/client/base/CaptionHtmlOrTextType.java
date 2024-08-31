package lsfusion.gwt.client.base;

public class CaptionHtmlOrTextType extends HtmlOrTextType {
    public static final CaptionHtmlOrTextType MESSAGE = new CaptionHtmlOrTextType();
    // properties
    public static final CaptionHtmlOrTextType COMMENT_VERT = new CaptionHtmlOrTextType();
    public static final CaptionHtmlOrTextType COMMENT_HORZ = new CaptionHtmlOrTextType();
    public static final CaptionHtmlOrTextType ASYNCVALUES = new CaptionHtmlOrTextType();

    @Override
    public boolean isWrap() {
        return this == COMMENT_VERT || this == MESSAGE;
    }

    @Override
    protected boolean isWrapWordBreak() {
        return this == MESSAGE;
    }
}
