package lsfusion.gwt.client.base;

import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.view.GColorTheme;
import lsfusion.gwt.client.view.MainFrame;

import java.io.Serializable;
import java.util.HashMap;

public class AppStaticImage extends BaseStaticImage implements Serializable {
    private HashMap<GColorTheme, ImageDescription> images = new HashMap<>();
    private String fontImage;

    public AppStaticImage() {
    }

    public AppStaticImage(String fontImage) {
        this.fontImage = fontImage;
    }

    public void addImage(GColorTheme colorTheme, String url, String disabledUrl, int width, int height) {
        images.put(colorTheme, new ImageDescription(url, disabledUrl, width, height));
    }

    public ImageDescription getImage() {
        return images.get(MainFrame.colorTheme);
    }

    @Override
    public Element createImage() {
        return GwtClientUtils.createAppStaticImage(this);
    }

    @Override
    public void setImageSrc(Element element) {
        setImageSrc(element, true, false);
    }

    public void setImageSrc(Element element, boolean enabled, boolean loadingReplaceImage) {
        if(loadingReplaceImage) // temp, should be always synced with the image element type
            StaticImage.LOADING_IMAGE_PATH.setImageSrc(element);
        else
            GwtClientUtils.setAppStaticImageSrc(element, this, enabled);
    }
}
