package lsfusion.gwt.client.base;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.ImageElement;

import java.io.Serializable;

public abstract class BaseStaticImage implements Serializable {

    protected String fontClasses;

    public BaseStaticImage() {
    }

    public BaseStaticImage(String fontClasses) {
        this.fontClasses = fontClasses;
    }

    public abstract Element createImage();

    public void setImageSrc(Element element) {
        setImageSrc(element, null);
    }
    public abstract void setImageSrc(Element element, BaseStaticImage overrideImage);

    public String getFontClasses() {
        return fontClasses;
    }
    public abstract void setImageElementSrc(ImageElement imageElement, boolean enabled);
}
