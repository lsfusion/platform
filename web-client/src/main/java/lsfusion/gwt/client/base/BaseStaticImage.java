package lsfusion.gwt.client.base;

import com.google.gwt.dom.client.Element;

import java.io.Serializable;

public abstract class BaseStaticImage implements Serializable {

    public abstract Element createImage();

    public abstract void setImageSrc(Element element);
}
