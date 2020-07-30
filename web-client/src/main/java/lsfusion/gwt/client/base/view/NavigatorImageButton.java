package lsfusion.gwt.client.base.view;

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
            setAppImagePath(imageDescription.url);
            if (imageDescription.width != -1) {
                image.setWidth(imageDescription.width + "px");
            }
            if (imageDescription.height != -1) {
                image.setHeight(imageDescription.height + "px");
            }
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

    public void setAppImagePath(String imagePath) {
        setAbsoluteImagePath(imagePath == null ? null : GwtClientUtils.getAppImagePath(imagePath));
    }

    @Override
    public void colorThemeChanged() {
        setCurrentThemeImage();
    }
}
