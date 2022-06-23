package lsfusion.gwt.client.base.view;

import lsfusion.gwt.client.base.size.GSize;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.ImageDescription;
import lsfusion.gwt.client.base.ImageHolder;

public class NavigatorImageButton extends ImageButton {
    private final ImageHolder imageHolder;

    public NavigatorImageButton(ImageHolder imageHolder, String caption) {
        super(caption, null);
        this.imageHolder = imageHolder;
        setCurrentThemeImage();
    }

    public void setImage(ImageDescription imageDescription) {
        if (imageDescription != null) {
            setAbsoluteImagePath(GwtClientUtils.getAppStaticImageURL(imageDescription.getUrl()));
        }
    }
    
    private void setCurrentThemeImage() {
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
