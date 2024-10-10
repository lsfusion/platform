package lsfusion.gwt.client.base;

import lsfusion.gwt.client.base.size.GSize;
import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.form.design.GFontMetrics;
import lsfusion.gwt.client.view.GColorTheme;
import lsfusion.gwt.client.view.MainFrame;

import java.util.HashMap;

public class AppStaticImage extends BaseStaticImage implements AppBaseImage {
    private HashMap<GColorTheme, ImageDescription> images;

    public static final String INPUT_DIALOG = "dialog";
    public static final String INPUT_RESET = "reset";

    public AppStaticImage() {
    }

    public AppStaticImage(String fontClasses, HashMap<GColorTheme, ImageDescription> images) {
        super(fontClasses);
        this.images = images;
    }

    @Override
    public boolean useIcon() {
        if(images == null)
            return true;

        return super.useIcon();
    }

    private String getUrl(boolean enabled) {
        return getImage().getUrl(enabled); // images is not null, because useIcon returns true when images is null
    }

    public GSize getWidth(GFont font) {
        if(images == null)
            return GFontMetrics.getStringHeight(font, GFontMetrics.heightChar); // we're assuming that the icon is a square

        return getImage().getWidth();
    }

    public GSize getHeight(GFont font) {
        if(images == null)
            return null;

        return getImage().getHeight();
    }

    public ImageDescription getImage() {
        return images.get(MainFrame.colorTheme);
    }

    @Override
    public String getImageElementSrc(boolean enabled) {
        return GwtClientUtils.getAppStaticImageURL(getUrl(enabled));
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof AppStaticImage && GwtClientUtils.nullEquals(images, ((AppStaticImage) o).images);
    }

    @Override
    public int hashCode() {
        return GwtClientUtils.nullHash(images);
    }
}
