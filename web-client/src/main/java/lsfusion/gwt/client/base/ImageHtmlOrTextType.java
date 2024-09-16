package lsfusion.gwt.client.base;

public class ImageHtmlOrTextType extends CaptionHtmlOrTextType {

    public static final ImageHtmlOrTextType MAP = new ImageHtmlOrTextType() {
        public boolean isImageVertical() {
            return true;
        }
        public boolean isWrap() {
            return true;
        }
    };
    public static final ImageHtmlOrTextType CALENDAR = new ImageHtmlOrTextType() {
        public boolean isWrap() {
            return true;
        }
    };

    public static final ImageHtmlOrTextType FORM = new ImageHtmlOrTextType();
    public static final ImageHtmlOrTextType CONTAINER = new ImageHtmlOrTextType();

    public static final ImageHtmlOrTextType OTHER = new ImageHtmlOrTextType(); // navigator panel, user settings, tree caption
    // buttons: grid toolbar, navigator toolbar, close, mobile, page size
    public static ImageHtmlOrTextType BUTTON(boolean vert) {
        return new ImageHtmlOrTextType() {
            public boolean isImageVertical() {
                return vert;
            }
        };
    };

    public boolean isImageVertical() {
        return false;
    }
}
