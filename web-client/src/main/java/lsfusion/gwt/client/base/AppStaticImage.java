package lsfusion.gwt.client.base;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.ImageElement;
import lsfusion.gwt.client.view.GColorTheme;
import lsfusion.gwt.client.view.MainFrame;

import java.io.Serializable;
import java.util.HashMap;

public class AppStaticImage extends BaseStaticImage implements Serializable {
    private HashMap<GColorTheme, ImageDescription> images = new HashMap<>();

    public AppStaticImage() {
    }

    public AppStaticImage(String fontClasses) {
        super(fontClasses);
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
    public void setImageSrc(Element element, BaseStaticImage overrideImage) {
        setImageSrc(element, true, overrideImage);
    }

    public void setImageSrc(Element element, boolean enabled, BaseStaticImage overrideImage) {
        GwtClientUtils.setAppStaticImageSrc(element, this, enabled, overrideImage);
    }

    @Override
    public void setImageElementSrc(ImageElement imageElement, boolean enabled) {
        imageElement.setSrc(GwtClientUtils.getAppStaticImageURL(getImage().getUrl(enabled)));
    }
}
