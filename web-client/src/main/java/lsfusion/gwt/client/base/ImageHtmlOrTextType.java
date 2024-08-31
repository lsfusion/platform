package lsfusion.gwt.client.base;

public class ImageHtmlOrTextType extends CaptionHtmlOrTextType {

    public static final ImageHtmlOrTextType GRID_CAPTION = new ImageHtmlOrTextType(); // property / action grid caption
    public static final ImageHtmlOrTextType PANEL_CAPTION_VERT = new ImageHtmlOrTextType(); // property caption
    public static final ImageHtmlOrTextType PANEL_CAPTION_HORZ = new ImageHtmlOrTextType();
    public static final ImageHtmlOrTextType ACTION_VERT = new ImageHtmlOrTextType();
    public static final ImageHtmlOrTextType ACTION_HORZ = new ImageHtmlOrTextType(); // action caption

    public static final ImageHtmlOrTextType MAP = new ImageHtmlOrTextType(); // map
    public static final ImageHtmlOrTextType CALENDAR = new ImageHtmlOrTextType(); // calendar

    public static final ImageHtmlOrTextType FORM = new ImageHtmlOrTextType();
    public static final ImageHtmlOrTextType CONTAINER = new ImageHtmlOrTextType();

    public static final ImageHtmlOrTextType OTHER = new ImageHtmlOrTextType(); // navigator panel, user settings
    // buttons: grid toolbar, navigator toolbar, close, mobile, page size
    public static final ImageHtmlOrTextType BUTTON_VERT = new ImageHtmlOrTextType();
    public static final ImageHtmlOrTextType BUTTON_HORZ = new ImageHtmlOrTextType();

    public boolean isImageVertical() {
        return this == MAP || this == ACTION_VERT || this == BUTTON_VERT; // || this == GRID_CAPTION;
    }

    @Override
    public boolean isWrap() {
        return super.isWrap() || this == MAP || this == CALENDAR || this == GRID_CAPTION || this == PANEL_CAPTION_VERT;
    }

    @Override
    protected boolean isWrapWordBreak() {
        return super.isWrapWordBreak() || this == ImageHtmlOrTextType.GRID_CAPTION;
    }
}
