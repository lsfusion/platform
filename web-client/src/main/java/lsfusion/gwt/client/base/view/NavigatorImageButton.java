package lsfusion.gwt.client.base.view;

import lsfusion.gwt.client.base.size.GSize;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.ImageDescription;
import lsfusion.gwt.client.base.ImageHolder;

public class NavigatorImageButton extends ImageButton {
    private ImageHolder imageHolder;
    
    public NavigatorImageButton(ImageHolder imageHolder, String caption) {
        this(imageHolder, caption, false, true);
    }

    public NavigatorImageButton(ImageHolder imageHolder, String caption, boolean vertical, boolean alignCenter) {
        super(caption, null, vertical, alignCenter);
        this.imageHolder = imageHolder;
        setCurrentThemeImage();
    }

    public void setImage(ImageDescription imageDescription) {
        if (imageDescription != null) {
            setAbsoluteImagePath(GwtClientUtils.getAppStaticImageURL(imageDescription.getUrl()));
            GSize width = imageDescription.getWidth();
            if (width != null)
                image.setWidth(width.getString());
            GSize height = imageDescription.getHeight();
            if (height != null)
                image.setHeight(height.getString());
        }
    }
    
    private void setCurrentThemeImage() {
        setImage(getImage());
    }
    
    public void setDefaultImage() {
        setImage(getImage());
    }

    private ImageDescription getImage() {
        return imageHolder != null ? imageHolder.getImage() : null;
    }

    @Override
    public void colorThemeChanged() {
        setCurrentThemeImage();
    }
}
