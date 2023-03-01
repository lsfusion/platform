package lsfusion.gwt.client.base.view;

import lsfusion.gwt.client.base.BaseImage;
import lsfusion.gwt.client.base.BaseStaticImage;
import lsfusion.gwt.client.base.StaticImage;
import lsfusion.gwt.client.view.ColorThemeChangeListener;

public class StaticImageButton extends ImageButton {

    protected BaseImage image;
    protected String caption;

    public StaticImageButton(String caption, BaseStaticImage baseImage) {
        super(caption, baseImage, false, null);

        this.caption = caption;
        this.image = baseImage;
    }

    @Override
    protected BaseImage getImage() {
        return image;
    }

    @Override
    protected String getCaption() {
        return caption;
    }

    public void changeImage(StaticImage image) {
        this.image = image;
        updateImage();
    }
}
